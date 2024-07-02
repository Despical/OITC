/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2024 Despical
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.oitc.arena;

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.miscellaneous.PlayerUtils;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.api.events.game.OITCGameJoinAttemptEvent;
import me.despical.oitc.api.events.game.OITCGameLeaveAttemptEvent;
import me.despical.oitc.api.events.game.OITCGameStopEvent;
import me.despical.oitc.arena.options.ArenaOption;
import me.despical.oitc.handlers.ChatManager;
import me.despical.oitc.handlers.ChatManager.ActionType;
import me.despical.oitc.handlers.rewards.Reward;
import me.despical.oitc.user.User;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2018
 */
public class ArenaManager {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);
	private static final ChatManager chatManager = plugin.getChatManager();

	private ArenaManager() {
	}

	public static void joinAttempt(Player player, Arena arena) {
		OITCGameJoinAttemptEvent gameJoinEvent = new OITCGameJoinAttemptEvent(player, arena);
		plugin.getServer().getPluginManager().callEvent(gameJoinEvent);

		if (!arena.isReady()) {
			player.sendMessage(chatManager.prefixedMessage("In-Game.Arena-Not-Configured"));
			return;
		}

		if (gameJoinEvent.isCancelled()) {
			player.sendMessage(chatManager.prefixedMessage("In-Game.Join-Cancelled-Via-API"));
			return;
		}

		if (plugin.getArenaRegistry().isInArena(player)) {
			player.sendMessage(chatManager.prefixedMessage("In-Game.Already-Playing"));
			return;
		}

		User user = plugin.getUserManager().getUser(player);

		if (!plugin.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			if (!plugin.getPermissionsManager().hasJoinPerm(player, arena.getId())) {
				player.sendMessage(chatManager.prefixedMessage("In-Game.Join-No-Permission").replace("%permission%", plugin.getPermissionsManager().getJoinPerm().replace("<arena>", arena.getId())));
				return;
			}
		} else if (plugin.getOption(ConfigPreferences.Option.DISABLE_SPECTATING_ON_BUNGEE) && arena.getArenaState() == ArenaState.IN_GAME) {
			if (user.isSpectator()) {
				player.sendMessage(chatManager.prefixedMessage("In-Game.Spectating-Disabled-On-Bungee"));
				return;
			}
		}

		if (arena.getArenaState() == ArenaState.RESTARTING) {
			return;
		}

		if (arena.getPlayers().size() >= arena.getMaximumPlayers() && arena.getArenaState() == ArenaState.STARTING) {
			String perm = plugin.getPermissionsManager().getJoinFullPerm();

			if (!player.hasPermission(perm)) {
				player.sendMessage(chatManager.prefixedMessage("In-Game.Full-Game-No-Permission"));
				return;
			}

			boolean foundSlot = false;

			for (Player loopPlayer : arena.getPlayers()) {
				if (loopPlayer.hasPermission(perm)) {
					continue;
				}

				leaveAttempt(loopPlayer, arena);
				loopPlayer.sendMessage(chatManager.prefixedMessage("In-Game.Messages.Lobby-Messages.You-Were-Kicked-For-Premium-Slot"));
				arena.broadcastMessage(chatManager.prefixedFormattedMessage(arena, chatManager.message("In-Game.Messages.Lobby-Messages.Kicked-For-Premium-Slot"), loopPlayer));
				foundSlot = true;
				break;
			}

			if (!foundSlot) {
				player.sendMessage(chatManager.prefixedMessage("In-Game.No-Slots-For-Premium"));
				return;
			}
		}

		if (plugin.getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.saveInventoryToFile(plugin, player);
		}

		user.cacheScoreboard();

		arena.addPlayer(player);
		arena.getScoreboardManager().createScoreboard(player);

		player.setLevel(0);
		player.setExp(0);
		player.setFoodLevel(20);
		player.getInventory().clear();
		player.getInventory().setHeldItemSlot(0);
		player.getInventory().setArmorContents(null);
		player.setGameMode(GameMode.ADVENTURE);

		ArenaUtils.hidePlayersOutsideTheGame(player, arena);

		user.addGameItem("leave-item");
		user.resetStats();
		user.heal();
		user.updateAttackCooldown();

		if (arena.isArenaState(ArenaState.WAITING_FOR_PLAYERS, ArenaState.STARTING) && player.isOp()) {
			user.addGameItem("force-start-item");
		}

		if (arena.isArenaState(ArenaState.IN_GAME, ArenaState.ENDING)) {
			user.setSpectator(true);
			user.addGameItems("teleporter-item", "settings-item");

			arena.teleportToStartLocation(player);

			player.sendMessage(chatManager.prefixedMessage("In-Game.You-Are-Spectator"));
			player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
			player.setAllowFlight(true);
			player.setFlying(true);

			ArenaUtils.hidePlayer(player, arena);

			hide_player: {
				if (!ArenaUtils.shouldHide()) break hide_player;

				for (Player spectator : arena.getPlayers()) {
					if (plugin.getUserManager().getUser(spectator).isSpectator()) {
						PlayerUtils.hidePlayer(player, spectator, plugin);
					} else {
						PlayerUtils.showPlayer(player, spectator, plugin);
					}
				}
			}

			return;
		}

		arena.teleportToLobby(player);
		arena.getGameBar().doBarAction(user, 1);
		arena.showPlayers();

		ArenaUtils.showPlayer(player, arena);
		ArenaUtils.updateNameTagsVisibility(player);

		chatManager.broadcastAction(arena, user, ActionType.JOIN);
		plugin.getSignManager().updateSigns();
	}

	public static void leaveAttempt(Player player, Arena arena) {
		leaveAttempt(player, arena, false);
	}

	public static void leaveAttempt(Player player, Arena arena, boolean quit) {
		OITCGameLeaveAttemptEvent event = new OITCGameLeaveAttemptEvent(player, arena);
		plugin.getServer().getPluginManager().callEvent(event);

		User user = plugin.getUserManager().getUser(player);

		int localKills = user.getStat(StatsStorage.StatisticType.LOCAL_KILLS);

		if (localKills > user.getStat(StatsStorage.StatisticType.HIGHEST_SCORE)) {
			user.setStat(StatsStorage.StatisticType.HIGHEST_SCORE, localKills);
		}

		plugin.getUserManager().saveAllStatistic(user);

		if (arena.getArenaState() == ArenaState.IN_GAME && !user.isSpectator()) {
			if (arena.getPlayersLeft().size() - 1 == 1) {
				stopGame(false, arena);
				return;
			}
		}

		arena.removePlayer(player);
		arena.teleportToEndLocation(player);

		chatManager.broadcastAction(arena, user, ActionType.LEAVE);

		user.heal();
		user.resetAttackCooldown();
		user.setSpectator(false);
		arena.getScoreboardManager().removeScoreboard(player);
		arena.getGameBar().doBarAction(user, 0);

		user.removeScoreboard();

		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		player.setFoodLevel(20);
		player.setLevel(0);
		player.setExp(0);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		player.setFlySpeed(0.1f);
		player.setWalkSpeed(0.2f);
		player.setFireTicks(0);
		player.setGameMode(GameMode.SURVIVAL);
		player.getInventory().setHeldItemSlot(4); // change slot to not trigger something else

		if (!arena.isArenaState(ArenaState.WAITING_FOR_PLAYERS, ArenaState.STARTING) && arena.getPlayers().isEmpty()) {
			arena.setArenaState(ArenaState.ENDING);
			arena.setTimer(0);
		}

		arena.teleportToEndLocation(player);

		ArenaUtils.showPlayersOutsideTheGame(player, arena);

		if (!quit && plugin.getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, player);
		}

		plugin.getUserManager().saveAllStatistic(user);
		plugin.getSignManager().updateSigns();
	}

	public static void stopGame(boolean quickStop, Arena arena) {
		OITCGameStopEvent gameStopEvent = new OITCGameStopEvent(arena, quickStop ? OITCGameStopEvent.StopReason.COMMAND : OITCGameStopEvent.StopReason.DEFAULT);
		plugin.getServer().getPluginManager().callEvent(gameStopEvent);

		arena.setArenaState(ArenaState.ENDING);

		if (quickStop) {
			arena.setTimer(2);
			arena.broadcastMessage(chatManager.prefixedMessage("in_game.messages.admin_messages.stopped_game"));
			return;
		} else {
			arena.setTimer(ArenaOption.LOBBY_ENDING_TIME.value());
		}

		String topPlayerName = arena.getScoreboardManager().getTopPlayerName(0);

		for (Player player : arena.getPlayers()) {
			arena.getScoreboardManager().stopAllScoreboards();

			User user = plugin.getUserManager().getUser(player);

			if (topPlayerName.equals(player.getName())) {
				user.addStat(StatsStorage.StatisticType.WINS, 1);
				user.performReward(Reward.RewardType.WIN);
				user.sendTitle(chatManager.message("in_game.messages.game_end_messages.titles.win"), chatManager.message("in_game.messages.game_end_messages.subtitles.win").replace("%winner%", topPlayerName));
			} else {
				user.sendTitle(chatManager.message("in_game.messages.game_end_messages.titles.lose"), chatManager.message("in_game.messages.game_end_messages.subtitles.lose").replace("%winner%", topPlayerName));

				if (!user.isSpectator()) {
					user.addStat(StatsStorage.StatisticType.LOSES, 1);
					user.performReward(Reward.RewardType.LOSE);
				}
			}

			plugin.getUserManager().saveAllStatistic(user);

			player.getInventory().clear();

			for (String msg : chatManager.getStringList("in_game.messages.game_end_messages.summary_message")) {
				MiscUtils.sendCenteredMessage(player, formatSummaryPlaceholders(msg, arena, player));
			}

			if (plugin.getConfig().getBoolean("Firework-When-Game-Ends", true)) {
				new BukkitRunnable() {

					private int i = 0;

					public void run() {
						if (i == 4 || !arena.getPlayers().contains(player)) {
							cancel();
						}

						MiscUtils.spawnRandomFirework(arena.getRandomSpawnPoint());
						i++;
					}
				}.runTaskTimer(plugin, 30, 30);
			}
		}
	}

	private static String formatSummaryPlaceholders(String msg, Arena arena, Player player) {
		String formatted = msg, topPlayerName = arena.getScoreboardManager().getTopPlayerName(0);

		User user = plugin.getUserManager().getUser(player);
		formatted = formatted.replace("%score%", StatsStorage.StatisticType.LOCAL_KILLS.from(user));
		formatted = formatted.replace("%deaths%", StatsStorage.StatisticType.LOCAL_DEATHS.from(user));
		formatted = formatted.replace("%rank%", Integer.toString(arena.getScoreboardManager().getRank(player)));
		formatted = formatted.replace("%winner%", topPlayerName);
		formatted = formatted.replace("%winner_score%", Integer.toString(StatsStorage.getUserStats(plugin.getServer().getPlayer(topPlayerName), StatsStorage.StatisticType.LOCAL_KILLS)));

		if (chatManager.isPapiEnabled()) {
			formatted = PlaceholderAPI.setPlaceholders(player, formatted);
		}

		return formatted;
	}
}