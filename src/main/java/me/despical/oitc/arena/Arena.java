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

import me.despical.commons.compat.Titles;
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

	private final Main plugin = JavaPlugin.getPlugin(Main.class);

	private final String id;

	private final Set<Player> players;
	private List<Location> playerSpawnPoints;

	private final Map<ArenaOption, Integer> arenaOptions;
	private final Map<GameLocation, Location> gameLocations;

	private ArenaState arenaState = ArenaState.INACTIVE;
	private BossBar gameBar;
	private final ScoreboardManager scoreboardManager;
	private String mapName = "";
	private boolean forceStart, ready;

	public Arena(String id) {
		this.id = id;
		this.players = new HashSet<>();
		this.playerSpawnPoints = new ArrayList<>();
		this.arenaOptions = new EnumMap<>(ArenaOption.class);
		this.gameLocations = new EnumMap<>(GameLocation.class);

		for (ArenaOption option : ArenaOption.values()) {
			arenaOptions.put(option, option.getDefaultValue());
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
			gameBar = plugin.getServer().createBossBar(plugin.getChatManager().message("Bossbar.Main-Title"), BarColor.BLUE, BarStyle.SOLID);
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

	public int getComplementProgress() {
		return ready ? 100 : 0;
	}

	public void setForceStart(boolean forceStart) {
		this.forceStart = forceStart;
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

	public ArenaState getArenaState() {
		return arenaState;
	}

	public void setArenaState(ArenaState arenaState) {
		this.arenaState = arenaState;

		plugin.getServer().getPluginManager().callEvent(new OITCGameStateChangeEvent(this, arenaState));
		plugin.getSignManager().updateSigns();
	}

	public Set<Player> getPlayers() {
		return new HashSet<>(players);
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

	public Location getLobbyLocation() {
		return gameLocations.get(GameLocation.LOBBY);
	}

	public void setLobbyLocation(Location loc) {
		gameLocations.put(GameLocation.LOBBY, loc);
	}

	public void teleportToStartLocation(Player player) {
		player.teleport(getRandomSpawnPoint());
	}
	
	public Location getRandomSpawnPoint() {
		return playerSpawnPoints.get(ThreadLocalRandom.current().nextInt(playerSpawnPoints.size()));
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

	public Location getEndLocation() {
		return gameLocations.get(GameLocation.END);
	}

	public void setEndLocation(Location endLoc) {
		gameLocations.put(GameLocation.END, endLoc);
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
		for (Player player : players) {
			for (Player p : players) {
				PlayerUtils.showPlayer(player, p, plugin);
				PlayerUtils.showPlayer(p, player, plugin);
			}
		}
	}

	public int getOption(ArenaOption option) {
		return arenaOptions.get(option);
	}

	public void setOptionValue(ArenaOption option, int value) {
		arenaOptions.put(option, value);
	}

	@Override
	public void run() {
		if (players.isEmpty() && getArenaState() == ArenaState.WAITING_FOR_PLAYERS) {
			return;
		}

		LogUtils.log("[{0}] Running game task.", id);
		long start = System.currentTimeMillis();
		int timer = getTimer();

		switch (getArenaState()) {
			case WAITING_FOR_PLAYERS:
				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					plugin.getServer().setWhitelist(false);
				}

				if (players.size() < getMinimumPlayers()) {
					if (timer <= 0) {
						setTimer(45);
						broadcastMessage(plugin.getChatManager().prefixedFormattedPathMessage(this, "In-Game.Messages.Lobby-Messages.Waiting-For-Players", getMinimumPlayers()));
						break;
					}
				} else {
					if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
						gameBar.setTitle(plugin.getChatManager().message("Bossbar.Waiting-For-Players"));
					}

					broadcastMessage(plugin.getChatManager().prefixedMessage("In-Game.Messages.Lobby-Messages.Enough-Players-To-Start"));
					setArenaState(ArenaState.STARTING);
					setTimer(plugin.getConfigPreferences().getIntOption(ConfigPreferences.IntOption.STARTING_WAITING_TIME));
					showPlayers();
				}

				setTimer(timer - 1);
				break;
			case STARTING:
				int startTime = plugin.getConfigPreferences().getIntOption(ConfigPreferences.IntOption.STARTING_TIME_ON_FULL_LOBBY);

				if (players.size() == getMaximumPlayers() && timer >= startTime && !forceStart) {
					setTimer(startTime);
					broadcastMessage(plugin.getChatManager().message("In-Game.Messages.Lobby-Messages.Start-In").replace("%time%", Integer.toString(timer)));
				}

				double waitingTime = plugin.getConfigPreferences().getIntOption(ConfigPreferences.IntOption.STARTING_WAITING_TIME);
				boolean bossBarEnabled = plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED);

				if (bossBarEnabled) {
					gameBar.setTitle(plugin.getChatManager().message("Bossbar.Starting-In").replace("%time%", Integer.toString(timer)));
					gameBar.setProgress(timer / waitingTime);
				}

				for (Player player : players) {
					player.setExp((float) (timer / waitingTime));
					player.setLevel(timer);
				}

				if (players.size() < getMinimumPlayers() && !forceStart) {
					if (bossBarEnabled) {
						gameBar.setTitle(plugin.getChatManager().message("Bossbar.Waiting-For-Players"));
						gameBar.setProgress(1d);
					}

					broadcastMessage(plugin.getChatManager().prefixedFormattedPathMessage(this, "In-Game.Messages.Lobby-Messages.Waiting-For-Players", getMinimumPlayers()));
					setArenaState(ArenaState.WAITING_FOR_PLAYERS);
					setTimer(15);

					for (Player player : players) {
						player.setExp(1);
						player.setLevel(0);
					}

					if (forceStart) {
						forceStart = false;
					}

					break;
				}

				if (timer == 0 || forceStart) {
					plugin.getServer().getPluginManager().callEvent(new OITCGameStartEvent(this));
					setArenaState(ArenaState.IN_GAME);

					if (bossBarEnabled) {
						gameBar.setProgress(1d);
					}

					setTimer(5);

					if (players.isEmpty()) {
						break;
					}

					teleportAllToStartLocation();

					for (Player player : players) {
						ArenaUtils.updateNameTagsVisibility(player);
						ArenaUtils.hidePlayersOutsideTheGame(player, this);

						plugin.getUserManager().getUser(player).addStat(StatsStorage.StatisticType.GAMES_PLAYED, 1);

						player.setGameMode(GameMode.ADVENTURE);
						player.sendMessage(plugin.getChatManager().prefixedMessage("In-Game.Messages.Lobby-Messages.Game-Started"));
						ItemPosition.giveKit(player);

						setTimer(plugin.getConfigPreferences().getIntOption(ConfigPreferences.IntOption.CLASSIC_GAMEPLAY_TIME));

						player.setInvulnerable(false);
					}
				}

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().message("Bossbar.In-Game-Info"));
				}

				if (forceStart) {
					forceStart = false;
				}

				setTimer(timer - 1);
				break;
			case IN_GAME:
				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					plugin.getServer().setWhitelist(getMaximumPlayers() <= players.size());
				}

				if (timer <= 0) {
					ArenaManager.stopGame(false, this);
				}

				if (timer == 30 || timer == 60) {
					String title = plugin.getChatManager().message("In-Game.Messages.Seconds-Left-Title").replace("%time%", Integer.toString(timer));
					String subtitle = plugin.getChatManager().message("In-Game.Messages.Seconds-Left-Subtitle").replace("%time%", Integer.toString(timer));

					for (Player p : players) {
						Titles.sendTitle(p, title, subtitle, 5, 40, 5);
					}
				}

				if (getPlayersLeft().isEmpty()) {
					ArenaManager.stopGame(false, this);
				}

				setTimer(timer - 1);
				break;
			case ENDING:
				scoreboardManager.stopAllScoreboards();

				if (timer <= 0) {
					if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
						gameBar.setTitle(plugin.getChatManager().message("Bossbar.Game-Ended"));
					}

					for (Player player : getPlayers()) {
						for (Player players : plugin.getServer().getOnlinePlayers()) {
							player.showPlayer(plugin, players);

							if (!ArenaRegistry.isInArena(players)) {
								players.showPlayer(plugin, player);
							}
						}

						PlayerUtils.setCollidable(player, false);

						player.setFlySpeed(0.1f);
						player.setWalkSpeed(0.2f);
						player.setFlying(false);
						player.setAllowFlight(false);
						player.getInventory().clear();
						player.getInventory().setArmorContents(null);
						player.setFireTicks(0);
						player.setFoodLevel(20);
						player.setGameMode(GameMode.SURVIVAL);
						player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

						doBarAction(BarAction.REMOVE, player);

						User user = plugin.getUserManager().getUser(player);
						user.setSpectator(false);

						plugin.getUserManager().saveAllStatistic(user);
					}

					teleportAllToEndLocation();

					if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
						for (Player player : players) {
							InventorySerializer.loadInventory(plugin, player);
						}
					}

					broadcastMessage(plugin.getChatManager().prefixedMessage("Commands.Teleported-To-The-Lobby"));

					plugin.getRewardsFactory().performReward(this, Reward.RewardType.END_GAME);
					plugin.getBungeeManager().shutdownIfEnabled();

					setArenaState(ArenaState.RESTARTING);
				}

				setTimer(timer - 1);
				break;
			case RESTARTING:
				players.clear();
				setArenaState(ArenaState.WAITING_FOR_PLAYERS);

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					ArenaRegistry.shuffleBungeeArena();

					Arena bungeeArena = ArenaRegistry.getBungeeArena();

					for (Player player : plugin.getServer().getOnlinePlayers()) {
						ArenaManager.joinAttempt(player, bungeeArena);
					}
				}

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSS_BAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().message("Bossbar.Waiting-For-Players"));
				}

				break;
			default:
				break;
		}

		LogUtils.log("[{0}] Game task finished took {1} ms", id, System.currentTimeMillis() - start);
	}

	public enum BarAction {
		ADD, REMOVE
	}

	public enum GameLocation {
		LOBBY, END
	}
}