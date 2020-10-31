/*
 * OITC - Reach 25 points to win!
 * Copyright (C) 2020 Despical
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.oitc.arena;

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.commonsbox.miscellaneous.MiscUtils;
import me.despical.commonsbox.serializer.InventorySerializer;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.api.events.game.OITCGameJoinAttemptEvent;
import me.despical.oitc.api.events.game.OITCGameLeaveAttemptEvent;
import me.despical.oitc.api.events.game.OITCGameStopEvent;
import me.despical.oitc.handlers.ChatManager.ActionType;
import me.despical.oitc.handlers.PermissionsManager;
import me.despical.oitc.handlers.items.SpecialItemManager;
import me.despical.oitc.handlers.rewards.Reward;
import me.despical.oitc.user.User;
import me.despical.oitc.utils.Debugger;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2018
 */
public class ArenaManager {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	private ArenaManager() {}

	/**
	 * Attempts player to join arena.
	 * Calls OITCGameJoinAttemptEvent.
	 * Can be cancelled only via above-mentioned event
	 *
	 * @param player player to join
	 * @param arena target arena
	 * @see OITCGameJoinAttemptEvent
	 */
	public static void joinAttempt(Player player, Arena arena) {
		Debugger.debug("[{0}] Initial join attempt for {1}", arena.getId(), player.getName());
		long start = System.currentTimeMillis();
		OITCGameJoinAttemptEvent gameJoinAttemptEvent = new OITCGameJoinAttemptEvent(player, arena);
		Bukkit.getPluginManager().callEvent(gameJoinAttemptEvent);

		if (!arena.isReady()) {
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Arena-Not-Configured"));
			return;
		}

		if (gameJoinAttemptEvent.isCancelled()) {
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Join-Cancelled-Via-API"));
			return;
		}

		if (ArenaRegistry.isInArena(player)) {
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Already-Playing"));
			return;
		}

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			if (!player.hasPermission(PermissionsManager.getJoinPerm().replace("<arena>", "*")) || !player.hasPermission(PermissionsManager.getJoinPerm().replace("<arena>", arena.getId()))) {
				player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Join-No-Permission").replace("%permission%", PermissionsManager.getJoinPerm().replace("<arena>", arena.getId())));
				return;
			}
		}

		if (arena.getArenaState() == ArenaState.RESTARTING) {
			return;
		}

		if (arena.getPlayers().size() >= arena.getMaximumPlayers() && arena.getArenaState() == ArenaState.STARTING) {
			if (!player.hasPermission(PermissionsManager.getJoinFullGames())) {
				player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Full-Game-No-Permission"));
				return;
			}

			boolean foundSlot = false;

			for (Player loopPlayer : arena.getPlayers()) {
				if (loopPlayer.hasPermission(PermissionsManager.getJoinFullGames())) {
					continue;
				}

				ArenaManager.leaveAttempt(loopPlayer, arena);
				loopPlayer.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.You-Were-Kicked-For-Premium-Slot"));
				plugin.getChatManager().broadcast(arena, plugin.getChatManager().formatMessage(arena, plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Kicked-For-Premium-Slot"), loopPlayer));
				foundSlot = true;
				break;
			}

			if (!foundSlot) {
				player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.No-Slots-For-Premium"));
				return;
			}
		}

		Debugger.debug("[{0}] Checked join attempt for {1} initialized", arena.getId(), player.getName());
		User user = plugin.getUserManager().getUser(player);

		arena.getScoreboardManager().createScoreboard(user);

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.saveInventoryToFile(plugin, player);
		}

		arena.addPlayer(player);

		player.setLevel(0);
		player.setExp(1);
		player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		player.setFoodLevel(20);
		player.getInventory().setArmorContents(null);
		player.getInventory().clear();
		player.setGameMode(GameMode.ADVENTURE);

		Arrays.stream(StatsStorage.StatisticType.values()).filter(stat -> !stat.isPersistent()).forEach(stat -> user.setStat(stat, 0));

		if (arena.getArenaState() == ArenaState.IN_GAME || arena.getArenaState() == ArenaState.ENDING) {
			arena.teleportToStartLocation(player);
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.You-Are-Spectator"));
			player.getInventory().clear();
			player.getInventory().setItem(0, new ItemBuilder(XMaterial.COMPASS.parseItem()).name(plugin.getChatManager().colorMessage("In-Game.Spectator.Spectator-Item-Name")).build());
			player.getInventory().setItem(4, new ItemBuilder(XMaterial.COMPARATOR.parseItem()).name(plugin.getChatManager().colorMessage("In-Game.Spectator.Settings-Menu.Item-Name")).build());
			player.getInventory().setItem(8, SpecialItemManager.getSpecialItem("Leave").getItemStack());
			player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
			player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
			ArenaUtils.hidePlayer(player, arena);
			user.setSpectator(true);
			player.setCollidable(false);
			player.setAllowFlight(true);
			player.setFlying(true);

			for (Player spectator : arena.getPlayers()) {
				if (plugin.getUserManager().getUser(spectator).isSpectator()) {
					player.hidePlayer(plugin, spectator);
				} else {
					player.showPlayer(plugin, spectator);
				}
			}

			ArenaUtils.hidePlayersOutsideTheGame(player, arena);
			Debugger.debug("[{0}] Join attempt as spectator finished for {1} took {2} ms", arena.getId(), player.getName(), System.currentTimeMillis() - start);
			return;
		}

		arena.teleportToLobby(player);
		player.setFlying(false);
		player.setAllowFlight(false);
		arena.doBarAction(Arena.BarAction.ADD, player);

		if (!plugin.getUserManager().getUser(player).isSpectator()) {
			plugin.getChatManager().broadcastAction(arena, player, ActionType.JOIN);
		}

		if (arena.getArenaState() == ArenaState.STARTING || arena.getArenaState() == ArenaState.WAITING_FOR_PLAYERS) {
			player.getInventory().setItem(SpecialItemManager.getSpecialItem("Leave").getSlot(), SpecialItemManager.getSpecialItem("Leave").getItemStack());
		}

		player.updateInventory();

		arena.getPlayers().forEach(arenaPlayer -> ArenaUtils.showPlayer(arenaPlayer, arena));
		arena.showPlayers();

		ArenaUtils.updateNameTagsVisibility(player);
		Debugger.debug("[{0}] Join attempt as player for {1} took {2} ms", arena.getId(), player.getName(), System.currentTimeMillis() - start);
	}

	/**
	 * Attempts player to leave arena.
	 * Calls OITCGameLeaveAttemptEvent event.
	 *
	 * @param player player to join
	 * @param arena target arena
	 * @see OITCGameLeaveAttemptEvent
	 */
	public static void leaveAttempt(Player player, Arena arena) {
		Debugger.debug("[{0}] Initial leave attempt for {1}", arena.getId(), player.getName());
		long start = System.currentTimeMillis();
		OITCGameLeaveAttemptEvent event = new OITCGameLeaveAttemptEvent(player, arena);
		Bukkit.getPluginManager().callEvent(event);
		User user = plugin.getUserManager().getUser(player);

		if (user.getStat(StatsStorage.StatisticType.LOCAL_KILLS) > user.getStat(StatsStorage.StatisticType.HIGHEST_SCORE)) {
			user.setStat(StatsStorage.StatisticType.HIGHEST_SCORE, user.getStat(StatsStorage.StatisticType.LOCAL_KILLS));
		}

		arena.getScoreboardManager().removeScoreboard(user);

		if (arena.getArenaState() == ArenaState.IN_GAME && !user.isSpectator()) {
			if (arena.getPlayersLeft().size() - 1 == 1) {
				ArenaManager.stopGame(false, arena);
				return;
			}
		}

		player.setFlySpeed(0.1f);
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		arena.removePlayer(player);
		arena.teleportToEndLocation(player);

		if (!user.isSpectator()) {
			plugin.getChatManager().broadcastAction(arena, player, ActionType.LEAVE);
		}

		player.setGlowing(false);
		user.setSpectator(false);
		player.setCollidable(true);
		user.removeScoreboard();
		arena.doBarAction(Arena.BarAction.REMOVE, player);
		player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		player.setFoodLevel(20);
		player.setLevel(0);
		player.setExp(0);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		player.setWalkSpeed(0.2f);
		player.setFireTicks(0);

		if (arena.getArenaState() != ArenaState.WAITING_FOR_PLAYERS && arena.getArenaState() != ArenaState.STARTING && arena.getPlayers().size() == 0) {
			arena.setArenaState(ArenaState.ENDING);
			arena.setTimer(0);
		}

		player.setGameMode(GameMode.SURVIVAL);

		for (Player players : plugin.getServer().getOnlinePlayers()) {
			if (ArenaRegistry.getArena(players) == null) {
				players.showPlayer(plugin, player);
			}

			player.showPlayer(plugin, players);
		}

		arena.teleportToEndLocation(player);

		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED) && plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, player);
		}

		plugin.getUserManager().saveAllStatistic(user);
		Debugger.debug("[{0}] Game leave finished for {1} took {2} ms", arena.getId(), player.getName(), System.currentTimeMillis() - start);
	}

	/**
	 * Stops current arena.
	 * Calls OITCGameStopEvent event
	 *
	 * @param quickStop should arena be stopped immediately? (use only in important cases)
	 * @param arena target arena
	 * @see OITCGameStopEvent
	 */
	public static void stopGame(boolean quickStop, Arena arena) {
		Debugger.debug("[{0}] Stop game event initialized with quickStop {1}", arena.getId(), quickStop);
		long start = System.currentTimeMillis();
		OITCGameStopEvent gameStopEvent = new OITCGameStopEvent(arena);

		Bukkit.getPluginManager().callEvent(gameStopEvent);
		arena.setArenaState(ArenaState.ENDING);

		if (quickStop) {
			arena.setTimer(2);
			plugin.getChatManager().broadcast(arena, plugin.getChatManager().colorMessage("In-Game.Messages.Admin-Messages.Stopped-Game"));
		} else {
			arena.setTimer(10);
		}

		List<String> summaryMessages = plugin.getChatManager().getStringList("In-Game.Messages.Game-End-Messages.Summary-Message");
		arena.getScoreboardManager().stopAllScoreboards();

		for (Player player : arena.getPlayers()) {
			User user = plugin.getUserManager().getUser(player);

			if (arena.getScoreboardManager().getTopPlayerName(0).equals(player.getName())) {
				user.addStat(StatsStorage.StatisticType.WINS, 1);
				player.sendTitle(plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Titles.Win"),
					plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Subtitles.Win").replace("%winner%", arena.getScoreboardManager().getTopPlayerName(0)), 5, 40, 5);
				plugin.getRewardsFactory().performReward(player, Reward.RewardType.WIN);
			} else if (!user.isSpectator()) {
				user.addStat(StatsStorage.StatisticType.LOSES, 1);
				player.sendTitle(plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Titles.Lose"),
					plugin.getChatManager().colorMessage("In-Game.Messages.Game-End-Messages.Subtitles.Lose").replace("%winner%", arena.getScoreboardManager().getTopPlayerName(0)), 5, 40, 5);
				plugin.getRewardsFactory().performReward(player, Reward.RewardType.LOSE);
			}

			player.getInventory().clear();
			player.getInventory().setItem(SpecialItemManager.getSpecialItem("Leave").getSlot(), SpecialItemManager.getSpecialItem("Leave").getItemStack());

			if (!quickStop) {
				for (String msg : summaryMessages) {
					MiscUtils.sendCenteredMessage(player, formatSummaryPlaceholders(msg, arena, player));
				}
			}

			user.removeScoreboard();

			if (!quickStop && plugin.getConfig().getBoolean("Firework-When-Game-Ends", true)) {
				new BukkitRunnable() {
					int i = 0;

					public void run() {
						if (i == 4 || !arena.getPlayers().contains(player)) {
							this.cancel();
						}

						MiscUtils.spawnRandomFirework(player.getLocation());
						i++;
					}
				}.runTaskTimer(plugin, 30, 30);
			}
		}

		Debugger.debug("[{0}] Stop game event finished took {1} ms", arena.getId(), System.currentTimeMillis() - start);
	}

	private static String formatSummaryPlaceholders(String msg, Arena arena, Player player) {
		String formatted = msg;
		formatted = StringUtils.replace(formatted, "%score%", String.valueOf(StatsStorage.getUserStats(player, StatsStorage.StatisticType.LOCAL_KILLS)));
		formatted = StringUtils.replace(formatted, "%deaths%", String.valueOf(StatsStorage.getUserStats(player, StatsStorage.StatisticType.LOCAL_DEATHS)));
		formatted = StringUtils.replace(formatted, "%rank%", String.valueOf(arena.getScoreboardManager().getRank(player)));
		formatted = StringUtils.replace(formatted, "%winner%", String.valueOf(arena.getScoreboardManager().getTopPlayerName(0)));
		formatted = StringUtils.replace(formatted, "%winner_score%", String.valueOf(StatsStorage.getUserStats(Bukkit.getPlayerExact(arena.getScoreboardManager().getTopPlayerName(0)), StatsStorage.StatisticType.LOCAL_KILLS)));

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			formatted = PlaceholderAPI.setPlaceholders(player, formatted);
		}

		return formatted;
	}
}