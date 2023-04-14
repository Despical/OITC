/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2023 Despical
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

import me.despical.commons.compat.Titles;
import me.despical.commons.compat.VersionResolver;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.miscellaneous.PlayerUtils;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.util.LogUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.api.events.game.OITCGameStartEvent;
import me.despical.oitc.api.events.game.OITCGameStateChangeEvent;
import me.despical.oitc.arena.managers.ScoreboardManager;
import me.despical.oitc.arena.options.ArenaOption;
import me.despical.oitc.handlers.ChatManager;
import me.despical.oitc.handlers.rewards.Reward;
import me.despical.oitc.user.User;
import me.despical.oitc.util.ItemPosition;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Arena extends BukkitRunnable {

	private final static Main plugin = JavaPlugin.getPlugin(Main.class);
	private final static ChatManager chatManager = plugin.getChatManager();

	private final String id;
	private final ScoreboardManager scoreboardManager;

	private final Set<Player> players;
	private final Map<ArenaOption, Integer> arenaOptions;
	private final Map<GameLocation, Location> gameLocations;

	private boolean forceStart, ready;
	private BossBar gameBar;
	private String mapName = "";
	private ArenaState arenaState = ArenaState.INACTIVE;
	private List<Location> playerSpawnPoints;

	public Arena(String id) {
		this.id = id;
		this.players = new HashSet<>();
		this.playerSpawnPoints = new ArrayList<>();
		this.arenaOptions = new EnumMap<>(ArenaOption.class);
		this.gameLocations = new EnumMap<>(GameLocation.class);

		for (ArenaOption option : ArenaOption.values()) {
			arenaOptions.put(option, option.getDefaultValue());
		}

		if (VersionResolver.isCurrentHigher(VersionResolver.ServerVersion.v1_8_R3) && plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
			gameBar = plugin.getServer().createBossBar(chatManager.message("boss_bar.main_title"), BarColor.BLUE, BarStyle.SOLID);
		}

		scoreboardManager = new ScoreboardManager(plugin, this);
	}

	public String getId() {
		return id;
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public void setForceStart(boolean forceStart) {
		this.forceStart = forceStart;
	}

	public boolean isForceStart() {
		return forceStart;
	}

	public ScoreboardManager getScoreboardManager() {
		return scoreboardManager;
	}

	public int getMinimumPlayers() {
		return getOption(ArenaOption.MINIMUM_PLAYERS);
	}

	public void setMinimumPlayers(int minimumPlayers) {
		setOptionValue(ArenaOption.MINIMUM_PLAYERS, Math.max(2, minimumPlayers));
	}

	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public int getTimer() {
		return getOption(ArenaOption.TIMER);
	}

	public void setTimer(int timer) {
		setOptionValue(ArenaOption.TIMER, timer);
	}

	public int getMaximumPlayers() {
		return getOption(ArenaOption.MAXIMUM_PLAYERS);
	}

	public void setMaximumPlayers(int maximumPlayers) {
		setOptionValue(ArenaOption.MAXIMUM_PLAYERS, maximumPlayers);
	}

	public Location getLobbyLocation() {
		return gameLocations.get(GameLocation.LOBBY);
	}

	public void setLobbyLocation(Location loc) {
		gameLocations.put(GameLocation.LOBBY, loc);
	}

	public Location getEndLocation() {
		return gameLocations.get(GameLocation.END);
	}

	public void setEndLocation(Location endLoc) {
		gameLocations.put(GameLocation.END, endLoc);
	}

	public int getStartTime() {
		return getOption(ArenaOption.START_TIME);
	}

	public int getWaitingTime() {
		return getOption(ArenaOption.WAITING_TIME);
	}

	public int getGameplayTime() {
		return getOption(ArenaOption.CLASSIC_GAMEPLAY_TIME);
	}

	public ArenaState getArenaState() {
		return arenaState;
	}

	public void setArenaState(ArenaState arenaState) {
		this.arenaState = arenaState;

		plugin.getServer().getPluginManager().callEvent(new OITCGameStateChangeEvent(this, arenaState));
		plugin.getSignManager().updateSigns();
	}

	public boolean isArenaState(final ArenaState first, final ArenaState second) {
		return arenaState == first || arenaState == second;
	}

	private int getOption(ArenaOption option) {
		return arenaOptions.get(option);
	}

	private void setOptionValue(ArenaOption option, int value) {
		arenaOptions.put(option, value);
	}

	public Set<Player> getPlayers() {
		return players;
	}

	public void teleportToLobby(Player player) {
		player.setFoodLevel(20);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		player.setFlySpeed(.1F);
		player.setWalkSpeed(.2F);

		Location location = getLobbyLocation();

		if (location == null) {
			LogUtils.sendConsoleMessage("&cLobby location isn't initialized for arena " + id);
			return;
		}

		player.teleport(location);
	}

	public void doBarAction(BarAction action, Player p) {
		if (gameBar == null) return;

		if (action == BarAction.ADD) {
			gameBar.addPlayer(p);
		} else {
			gameBar.removePlayer(p);
		}
	}

	public Location getRandomSpawnPoint() {
		return playerSpawnPoints.get(ThreadLocalRandom.current().nextInt(playerSpawnPoints.size()));
	}

	public void teleportToStartLocation(Player player) {
		player.teleport(getRandomSpawnPoint());
	}

	private void teleportAllToStartLocation() {
		int i = 0;

		for (Player player : getPlayersLeft()) {
			player.teleport(playerSpawnPoints.get(i++));
		}
	}

	public void teleportAllToEndLocation() {
		for (Player player : players) {
			teleportToEndLocation(player);
		}
	}

	public void broadcastMessage(String message) {
		for (Player player : players) {
			player.sendMessage(message);
		}
	}

	public void teleportToEndLocation(Player player) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			plugin.getBungeeManager().connectToHub(player);
			return;
		}

		Location location = getEndLocation();

		if (location == null) {
			location = getLobbyLocation();
			LogUtils.sendConsoleMessage(String.format("&cCouldn't teleport %s to end location for arena %s because it isn't initialized!", player.getName(), id));
		}

		if (location != null) {
			player.teleport(location);
		}
	}

	public List<Location> getPlayerSpawnPoints() {
		return playerSpawnPoints;
	}

	public void setPlayerSpawnPoints(List<Location> playerSpawnPoints) {
		this.playerSpawnPoints = playerSpawnPoints;
	}

	public void start() {
		this.runTaskTimer(plugin, 20L, 20L);
		this.setArenaState(ArenaState.RESTARTING);

		LogUtils.log("[{0}] Game instance started.", id);
	}

	public void addPlayer(Player player) {
		players.add(player);
	}

	public void removePlayer(Player player) {
		players.remove(player);
	}

	public Set<Player> getPlayersLeft() {
		return plugin.getUserManager().getUsers(this).stream().filter(user -> !user.isSpectator()).map(User::getPlayer).collect(Collectors.toSet());
	}

	public void showPlayers() {
		if (ArenaUtils.isLegacy()) return;

		for (Player player : players) {
			for (Player p : players) {
				PlayerUtils.showPlayer(player, p, plugin);
				PlayerUtils.showPlayer(p, player, plugin);
			}
		}
	}

	@Override
	public void run() {
		if (players.isEmpty() && arenaState == ArenaState.WAITING_FOR_PLAYERS) {
			return;
		}

		int size = players.size(), waitingTime = getWaitingTime(), minPlayers = getMinimumPlayers();

		switch (arenaState) {
			case WAITING_FOR_PLAYERS:

				if (size < minPlayers) {
					if (gameBar != null) {
						gameBar.setTitle(chatManager.message("boss_bar.waiting_for_players"));
					}

					if (getTimer() <= 0) {
						setTimer(45);
						broadcastMessage(chatManager.formatMessage(this, "in_game.messages.lobby_messages.waiting_for_players"));
					}
				} else {
					showPlayers();
					setTimer(waitingTime);
					setArenaState(ArenaState.STARTING);
					broadcastMessage(chatManager.message("in_game.messages.lobby_messages.enough_players_to_start"));
					break;
				}

				setTimer(getTimer() - 1);
				break;
			case STARTING:
				if (gameBar != null) {
					gameBar.setProgress((double) getTimer() / waitingTime);
					gameBar.setTitle(chatManager.message("boss_bar.starting_in").replace("%time%", Integer.toString(getTimer())));
				}

				for (Player player : players) {
					player.setLevel(getTimer());
					player.setExp((float) (getTimer() / waitingTime));
				}

				if (size < minPlayers) {
					if (gameBar != null) {
						gameBar.setProgress(1D);
						gameBar.setTitle(chatManager.message("boss_bar.waiting_for_players"));
					}

					setTimer(waitingTime);
					setArenaState(ArenaState.WAITING_FOR_PLAYERS);
					broadcastMessage(chatManager.prefixedFormattedMessage(this, "in_game.messages.lobby_messages.waiting_for_players", minPlayers));

					for (Player player : players) {
						player.setExp(1F);
						player.setLevel(0);
					}

					break;
				}

				if (size >= getMaximumPlayers() && getTimer() >= getStartTime() && !forceStart) {
					setTimer(getStartTime());

					if (getTimer() == 15 || getTimer() == 10 || getTimer() <= 5) {
						broadcastMessage(chatManager.prefixedMessage("in_game.messages.lobby_messages.start_in", getTimer()));
					}
				}

				if (getTimer() == 0 || forceStart) {
					setArenaState(ArenaState.IN_GAME);

					plugin.getServer().getPluginManager().callEvent(new OITCGameStartEvent(this));

					if (gameBar != null) {
						gameBar.setProgress(1D);
						gameBar.setTitle(chatManager.message("boss_bar.in_game_info"));
					}

					setTimer(getGameplayTime());
					teleportAllToStartLocation();

					for (Player player : players) {
						ArenaUtils.updateNameTagsVisibility(player);
						ArenaUtils.hidePlayersOutsideTheGame(player, this);

						plugin.getUserManager().getUser(player).addStat(StatsStorage.StatisticType.GAMES_PLAYED, 1);

						player.setGameMode(GameMode.ADVENTURE);
						player.sendMessage(chatManager.prefixedMessage("in_game.messages.lobby_messages.game_started"));

						ItemPosition.giveKit(player);
					}

					if (forceStart) {
						forceStart = false;
					}
				}

				setTimer(getTimer() - 1);
				break;
			case IN_GAME:
				int playerSize = getPlayersLeft().size();

				if (playerSize < 2 || getTimer() <= 0) {
					ArenaManager.stopGame(false, this);
					return;
				}

				if (getTimer() == 30 || getTimer() == 60) {
					String title = chatManager.message("in_game.messages.seconds_left_title").replace("%time%", Integer.toString(getTimer()));
					String subtitle = chatManager.message("in_game.messages.seconds_left_subtitle").replace("%time%", Integer.toString(getTimer()));

					players.forEach(p -> Titles.sendTitle(p, title, subtitle));
				}

				setTimer(getTimer() - 1);
				break;
			case ENDING:
				if (getTimer() != 0) {
					setTimer(getTimer() - 1);
					return;
				}

				scoreboardManager.stopAllScoreboards();

				if (gameBar != null) {
					gameBar.setTitle(chatManager.message("boss-bar.game-ended"));
				}

				for (Player player : players) {
					ArenaUtils.showPlayersOutsideTheGame(player, this);
					AttributeUtils.resetAttackCooldown(player);

					player.setGameMode(GameMode.SURVIVAL);
					player.setFlySpeed(.1f);
					player.setWalkSpeed(.2f);
					player.setFlying(false);
					player.setAllowFlight(false);
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

					teleportToEndLocation(player);
					doBarAction(BarAction.REMOVE, player);
				}

				plugin.getRewardsFactory().performReward(this, Reward.RewardType.END_GAME);

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
					players.forEach(player -> InventorySerializer.loadInventory(plugin, player));
				}

				setArenaState(ArenaState.RESTARTING);
				broadcastMessage(chatManager.prefixedMessage("commands.teleported-to-the-lobby"));
				break;
			case RESTARTING:
				plugin.getUserManager().getUsers(this).forEach(user -> user.setSpectator(false));
				players.clear();

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					ArenaRegistry.shuffleBungeeArena();

					Arena bungeeArena = ArenaRegistry.getBungeeArena();

					for (Player player : plugin.getServer().getOnlinePlayers()) {
						ArenaManager.joinAttempt(player, bungeeArena);
					}
				}

				if (gameBar != null) {
					gameBar.setTitle(chatManager.message("boss_bar.waiting_for_players"));
				}

				setArenaState(ArenaState.WAITING_FOR_PLAYERS);
				break;
		}
	}

	public enum BarAction {
		ADD, REMOVE
	}

	public enum GameLocation {
		LOBBY, END
	}
}