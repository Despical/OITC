/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2021 Despical and contributors
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
import me.despical.commons.compat.Titles;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.miscellaneous.PlayerUtils;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.util.LogUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.api.events.game.OITCGameJoinAttemptEvent;
import me.despical.oitc.api.events.game.OITCGameLeaveAttemptEvent;
import me.despical.oitc.api.events.game.OITCGameStopEvent;
import me.despical.oitc.handlers.ChatManager;
import me.despical.oitc.handlers.ChatManager.ActionType;
import me.despical.oitc.handlers.items.SpecialItemManager;
import me.despical.oitc.handlers.rewards.Reward;
import me.despical.oitc.user.User;
import org.apache.commons.lang.StringUtils;
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
		LogUtils.log("[{0}] Initial join attempt for {1}", arena.getId(), player.getName());
		long start = System.currentTimeMillis();

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

		if (ArenaRegistry.isInArena(player)) {
			player.sendMessage(chatManager.prefixedMessage("In-Game.Already-Playing"));
			return;
		}

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			String perm = plugin.getPermissionsManager().getJoinPerm();

			if (!player.hasPermission(perm.replace("<arena>", "*")) || !player.hasPermission(perm.replace("<arena>", arena.getId()))) {
				player.sendMessage(chatManager.prefixedMessage("In-Game.Join-No-Permission").replace("%permission%", perm.replace("<arena>", arena.getId())));
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

		LogUtils.log("[{0}] Checked join attempt for {1} initialized", arena.getId(), player.getName());

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.saveInventoryToFile(plugin, player);
		}

		arena.addPlayer(player);
		arena.getScoreboardManager().createScoreboard(player);

		player.setLevel(0);
		player.setExp(1);
		player.setFoodLevel(20);
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		player.setGameMode(GameMode.ADVENTURE);

		AttributeUtils.healPlayer(player);
		PlayerUtils.setCollidable(player, false);
		PlayerUtils.setGlowing(player, false);

		User user = plugin.getUserManager().getUser(player);
		user.resetStats();

		if (arena.getArenaState() == ArenaState.IN_GAME || arena.getArenaState() == ArenaState.ENDING) {
			arena.teleportToStartLocation(player);
			player.sendMessage(chatManager.prefixedMessage("In-Game.You-Are-Spectator"));

			SpecialItemManager.giveItem(player, "Teleporter", "Spectator-Settings", "Leave", "Play-Again");

			player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
			player.setFlying(true);
			player.setAllowFlight(true);

			user.setSpectator(true);

			ArenaUtils.hidePlayer(player, arena);

			for (Player spectator : arena.getPlayers()) {
				if (plugin.getUserManager().getUser(spectator).isSpectator()) {
					PlayerUtils.hidePlayer(player, spectator, plugin);
				} else {
					PlayerUtils.showPlayer(player, spectator, plugin);
				}
			}

			ArenaUtils.hidePlayersOutsideTheGame(player, arena);
			LogUtils.log("[{0}] Join attempt as spectator finished for {1} took {2} ms.", arena.getId(), player.getName(), System.currentTimeMillis() - start);
			return;
		}

		SpecialItemManager.giveItem(player, "Leave");

		arena.teleportToLobby(player);
		arena.doBarAction(Arena.BarAction.ADD, player);
		arena.getPlayers().forEach(p -> ArenaUtils.showPlayer(p, arena));
		arena.showPlayers();

		chatManager.broadcastAction(arena, user, ActionType.JOIN);
		plugin.getSignManager().updateSigns();

		ArenaUtils.updateNameTagsVisibility(player);
		LogUtils.log("[{0}] Join attempt as player for {1} took {2} ms.", arena.getId(), player.getName(), System.currentTimeMillis() - start);
	}

	public static void leaveAttempt(Player player, Arena arena) {
		LogUtils.log("[{0}] Initial leave attempt for {1}", arena.getId(), player.getName());
		long start = System.currentTimeMillis();

		OITCGameLeaveAttemptEvent event = new OITCGameLeaveAttemptEvent(player, arena);
		plugin.getServer().getPluginManager().callEvent(event);

		User user = plugin.getUserManager().getUser(player);

		if (user.getStat(StatsStorage.StatisticType.LOCAL_KILLS) > user.getStat(StatsStorage.StatisticType.HIGHEST_SCORE)) {
			user.setStat(StatsStorage.StatisticType.HIGHEST_SCORE, user.getStat(StatsStorage.StatisticType.LOCAL_KILLS));
		}

		if (arena.getArenaState() == ArenaState.IN_GAME && !user.isSpectator()) {
			if (arena.getPlayersLeft().size() - 1 == 1) {
				stopGame(false, arena);
				return;
			}
		}

		arena.removePlayer(player);
		arena.teleportToEndLocation(player);

		chatManager.broadcastAction(arena, user, ActionType.LEAVE);

		PlayerUtils.setGlowing(player, false);
		PlayerUtils.setCollidable(player, false);
		AttributeUtils.healPlayer(player);

		user.setSpectator(false);
		arena.getScoreboardManager().removeScoreboard(player);
		arena.doBarAction(Arena.BarAction.REMOVE, player);

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

		if (arena.getArenaState() != ArenaState.WAITING_FOR_PLAYERS && arena.getArenaState() != ArenaState.STARTING && arena.getPlayers().isEmpty()) {
			arena.setArenaState(ArenaState.ENDING);
			arena.setTimer(0);
		}

		for (Player players : plugin.getServer().getOnlinePlayers()) {
			if (!ArenaRegistry.isInArena(players)) {
				players.showPlayer(plugin, player);
			}

			player.showPlayer(plugin, players);
		}

		arena.teleportToEndLocation(player);

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, player);
		}

		plugin.getUserManager().saveAllStatistic(user);
		plugin.getSignManager().updateSigns();

		LogUtils.log("[{0}] Game leave finished for {1} took {2} ms.", arena.getId(), player.getName(), System.currentTimeMillis() - start);
	}

	public static void stopGame(boolean quickStop, Arena arena) {
		LogUtils.log("[{0}] Stop game event initialized | quickStop = {1}", arena.getId(), quickStop);
		long start = System.currentTimeMillis();

		OITCGameStopEvent gameStopEvent = new OITCGameStopEvent(arena, quickStop ? OITCGameStopEvent.StopReason.COMMAND : OITCGameStopEvent.StopReason.DEFAULT);
		plugin.getServer().getPluginManager().callEvent(gameStopEvent);

		arena.setArenaState(ArenaState.ENDING);

		if (quickStop) {
			arena.setTimer(2);
			arena.broadcastMessage(chatManager.prefixedMessage("In-Game.Messages.Admin-Messages.Stopped-Game"));
			return;
		} else {
			arena.setTimer(12);
		}

		arena.getScoreboardManager().stopAllScoreboards();

		String topPlayerName = arena.getScoreboardManager().getTopPlayerName(0);

		for (Player player : arena.getPlayers()) {
			arena.getScoreboardManager().stopAllScoreboards();

			User user = plugin.getUserManager().getUser(player);

			if (topPlayerName.equals(player.getName())) {
				user.addStat(StatsStorage.StatisticType.WINS, 1);

				Titles.sendTitle(player, chatManager.message("In-Game.Messages.Game-End-Messages.Titles.Win"), chatManager.message("In-Game.Messages.Game-End-Messages.Subtitles.Win").replace("%winner%", topPlayerName), 5, 40, 5);

				plugin.getRewardsFactory().performReward(player, Reward.RewardType.WIN);
			} else if (!user.isSpectator()) {
				user.addStat(StatsStorage.StatisticType.LOSES, 1);

				Titles.sendTitle(player, chatManager.message("In-Game.Messages.Game-End-Messages.Titles.Lose"), chatManager.message("In-Game.Messages.Game-End-Messages.Subtitles.Lose").replace("%winner%", topPlayerName), 5, 40, 5);

				plugin.getRewardsFactory().performReward(player, Reward.RewardType.LOSE);
			}

			player.getInventory().clear();

			SpecialItemManager.giveItem(player, "Teleporter", "Spectator-Settings", "Leave", "Play-Again");

			for (String msg : chatManager.getStringList("In-Game.Messages.Game-End-Messages.Summary-Message")) {
				MiscUtils.sendCenteredMessage(player, formatSummaryPlaceholders(msg, arena, player));
			}

			if (plugin.getConfig().getBoolean("Firework-When-Game-Ends", true)) {
				new BukkitRunnable() {
					int i = 0;

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

		LogUtils.log("[{0}] Stop game event finished took {1} ms", arena.getId(), System.currentTimeMillis() - start);
	}

	private static String formatSummaryPlaceholders(String msg, Arena arena, Player player) {
		String formatted = msg, topPlayerName = arena.getScoreboardManager().getTopPlayerName(0);

		formatted = StringUtils.replace(formatted, "%score%", Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.LOCAL_KILLS)));
		formatted = StringUtils.replace(formatted, "%deaths%", Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.LOCAL_DEATHS)));
		formatted = StringUtils.replace(formatted, "%rank%", Integer.toString(arena.getScoreboardManager().getRank(player)));
		formatted = StringUtils.replace(formatted, "%winner%", topPlayerName);
		formatted = StringUtils.replace(formatted, "%winner_score%", Integer.toString(StatsStorage.getUserStats(plugin.getServer().getPlayerExact(topPlayerName), StatsStorage.StatisticType.LOCAL_KILLS)));

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			formatted = PlaceholderAPI.setPlaceholders(player, formatted);
		}

		return formatted;
	}
}