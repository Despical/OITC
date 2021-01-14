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

import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.commonsbox.serializer.InventorySerializer;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.api.events.game.OITCGameStartEvent;
import me.despical.oitc.api.events.game.OITCGameStateChangeEvent;
import me.despical.oitc.arena.managers.ScoreboardManager;
import me.despical.oitc.arena.options.ArenaOption;
import me.despical.oitc.handlers.rewards.Reward;
import me.despical.oitc.user.User;
import me.despical.oitc.utils.Debugger;
import me.despical.oitc.utils.ItemPosition;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Arena extends BukkitRunnable {

	private final Random random = new Random();
	private final Main plugin = JavaPlugin.getPlugin(Main.class);
	private final String id;

	private final Set<Player> players = new HashSet<>();
	private List<Location> playerSpawnPoints = new ArrayList<>();

	private final Map<ArenaOption, Integer> arenaOptions = new EnumMap<>(ArenaOption.class);
	private final Map<GameLocation, Location> gameLocations = new EnumMap<>(GameLocation.class);

	private ArenaState arenaState = ArenaState.INACTIVE;
	private BossBar gameBar;
	private final ScoreboardManager scoreboardManager;
	private String mapName = "";
	private boolean ready;
	private boolean forceStart = false;

	public Arena(String id) {
		this.id = id;

		for (ArenaOption option : ArenaOption.values()) {
			arenaOptions.put(option, option.getDefaultValue());
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
			gameBar = Bukkit.createBossBar(plugin.getChatManager().colorMessage("Bossbar.Main-Title"), BarColor.BLUE, BarStyle.SOLID);
		}

		scoreboardManager = new ScoreboardManager(this);
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	@Override
	public void run() {
		if (getPlayers().isEmpty() && getArenaState() == ArenaState.WAITING_FOR_PLAYERS) {
			return;
		}

		Debugger.performance("ArenaTask", "[PerformanceMonitor] [{0}] Running game task", getId());
		long start = System.currentTimeMillis();

		switch (getArenaState()) {
		case WAITING_FOR_PLAYERS:
			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
				plugin.getServer().setWhitelist(false);
			}

			if (getPlayers().size() < getMinimumPlayers()) {
				if (getTimer() <= 0) {
					setTimer(45);
					broadcastMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().formatMessage(this, plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Waiting-For-Players"), getMinimumPlayers()));
					break;
				}
			} else {
				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Waiting-For-Players"));
				}

				broadcastMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Enough-Players-To-Start"));
				setArenaState(ArenaState.STARTING);
				setTimer(plugin.getConfig().getInt("Starting-Waiting-Time", 60));
				showPlayers();
			}

			setTimer(getTimer() - 1);
			break;
		case STARTING:
			if (getPlayers().size() == getMaximumPlayers() && getTimer() >= plugin.getConfig().getInt("Start-Time-On-Full-Lobby", 15) && !forceStart) {
				setTimer(plugin.getConfig().getInt("Start-Time-On-Full-Lobby", 15));
				broadcastMessage(plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Start-In").replace("%time%", String.valueOf(getTimer())));
			}

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
				gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Starting-In").replace("%time%", String.valueOf(getTimer())));
				gameBar.setProgress(getTimer() / plugin.getConfig().getDouble("Starting-Waiting-Time", 60));
			}

			for (Player player : getPlayers()) {
				player.setExp((float) (getTimer() / plugin.getConfig().getDouble("Starting-Waiting-Time", 60)));
				player.setLevel(getTimer());
			}

			if (getPlayers().size() < getMinimumPlayers() && !forceStart) {
				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Waiting-For-Players"));
					gameBar.setProgress(1.0);
				}

				broadcastMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().formatMessage(this, plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Waiting-For-Players"), getMinimumPlayers()));
				setArenaState(ArenaState.WAITING_FOR_PLAYERS);
				Bukkit.getPluginManager().callEvent(new OITCGameStartEvent(this));
				setTimer(15);

				for (Player player : getPlayers()) {
					player.setExp(1);
					player.setLevel(0);
				}

				if (forceStart) {
					forceStart = false;
				}

				break;
			}

			if (getTimer() == 0 || forceStart) {
				OITCGameStartEvent gameStartEvent = new OITCGameStartEvent(this);
				Bukkit.getPluginManager().callEvent(gameStartEvent);
				setArenaState(ArenaState.IN_GAME);

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
					gameBar.setProgress(1.0);
				}

				setTimer(5);

				if (players.isEmpty()) {
					break;
				}

				teleportAllToStartLocation();

				for (Player player : getPlayers()) {
					ArenaUtils.updateNameTagsVisibility(player);
					player.getInventory().clear();
					player.setGameMode(GameMode.ADVENTURE);
					ArenaUtils.hidePlayersOutsideTheGame(player, this);
					plugin.getUserManager().getUser(player).addStat(StatsStorage.StatisticType.GAMES_PLAYED, 1);
					setTimer(plugin.getConfig().getInt("Classic-Gameplay-Time", 600));
					player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Messages.Lobby-Messages.Game-Started"));
					ItemPosition.setItem(player, ItemPosition.SWORD, new ItemBuilder(XMaterial.WOODEN_SWORD.parseItem()).unbreakable(true).amount(1).build());
					ItemPosition.setItem(player, ItemPosition.BOW, new ItemBuilder(XMaterial.BOW.parseItem()).enchantment(Enchantment.LUCK).flag(ItemFlag.HIDE_ENCHANTS).unbreakable(true).amount(1).build());
					ItemPosition.setItem(player, ItemPosition.ARROW, new ItemStack(Material.ARROW, 1));
					player.updateInventory();
				}
			}

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
				gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.In-Game-Info"));
			}

			if (forceStart) {
				forceStart = false;
			}

			setTimer(getTimer() - 1);
			break;
		case IN_GAME:
			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
				plugin.getServer().setWhitelist(getMaximumPlayers() <= getPlayers().size());
			}

			if (getTimer() <= 0) {
				ArenaManager.stopGame(false, this);
			}

			if (getTimer() == 30 || getTimer() == 60) {
				String title = plugin.getChatManager().colorMessage("In-Game.Messages.Seconds-Left-Title").replace("%time%", String.valueOf(getTimer()));
				String subtitle = plugin.getChatManager().colorMessage("In-Game.Messages.Seconds-Left-Subtitle").replace("%time%", String.valueOf(getTimer()));

				for (Player p : getPlayers()) {
					p.sendTitle(title, subtitle, 5, 40, 5);
				}
			}

			if (getPlayersLeft().isEmpty()) {
				ArenaManager.stopGame(false, this);
			}

			setTimer(getTimer() - 1);
			break;
		case ENDING:
			scoreboardManager.stopAllScoreboards();

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
				plugin.getServer().setWhitelist(false);
			}

			if (getTimer() <= 0) {
				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
					gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Game-Ended"));
				}

				List<Player> playersToQuit = new ArrayList<>(getPlayers());

				for (Player player : playersToQuit) {
					plugin.getUserManager().getUser(player).removeScoreboard();
					player.setGameMode(GameMode.SURVIVAL);

					for (Player players : Bukkit.getOnlinePlayers()) {
						player.showPlayer(plugin, players);

						if (ArenaRegistry.getArena(players) == null) {
							players.showPlayer(plugin, player);
						}
					}

					player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
					player.setWalkSpeed(0.2f);
					player.setFlying(false);
					player.setAllowFlight(false);
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					doBarAction(BarAction.REMOVE, player);
					player.setFireTicks(0);
					player.setFoodLevel(20);
				}

				teleportAllToEndLocation();

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
					for (Player player : getPlayers()) {
						InventorySerializer.loadInventory(plugin, player);
					}
				}

				broadcastMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("Commands.Teleported-To-The-Lobby"));

				for (User user : plugin.getUserManager().getUsers(this)) {
					user.setSpectator(false);
					user.getPlayer().setCollidable(true);
					plugin.getUserManager().saveAllStatistic(user);
				}

				plugin.getRewardsFactory().performReward(this, Reward.RewardType.END_GAME);
				players.clear();

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
					if (ConfigUtils.getConfig(plugin, "bungee").getBoolean("Shutdown-When-Game-Ends")) {
						plugin.getServer().shutdown();
					}
				}

				setArenaState(ArenaState.RESTARTING);
			}

			setTimer(getTimer() - 1);
			break;
		case RESTARTING:
			getPlayers().clear();
			setArenaState(ArenaState.WAITING_FOR_PLAYERS);

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
				ArenaRegistry.shuffleBungeeArena();

				for (Player player : Bukkit.getOnlinePlayers()) {
					ArenaManager.joinAttempt(player, ArenaRegistry.getArenas().get(ArenaRegistry.getBungeeArena()));
				}
			}

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
				gameBar.setTitle(plugin.getChatManager().colorMessage("Bossbar.Waiting-For-Players"));
			}

			break;
		default:
			break;
		}

		Debugger.performance("ArenaTask", "[PerformanceMonitor] [{0}] Game task finished took {1} ms", getId(), System.currentTimeMillis() - start);
	}

	public void setForceStart(boolean forceStart) {
		this.forceStart = forceStart;
	}

	public ScoreboardManager getScoreboardManager() {
		return scoreboardManager;
	}

	/**
	 * Get arena identifier used to get arenas by string.
	 *
	 * @return arena name
	 * @see ArenaRegistry#getArena(String)
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get minimum players needed.
	 *
	 * @return minimum players needed to start arena
	 */
	public int getMinimumPlayers() {
		return getOption(ArenaOption.MINIMUM_PLAYERS);
	}

	/**
	 * Set minimum players needed.
	 *
	 * @param minimumPlayers players needed to start arena
	 */
	public void setMinimumPlayers(int minimumPlayers) {
		if (minimumPlayers < 2) {
			Debugger.debug(Level.WARNING, "Minimum players amount for arena cannot be less than 2! Got {0}", minimumPlayers);
			setOptionValue(ArenaOption.MINIMUM_PLAYERS, 2);
			return;
		}

		setOptionValue(ArenaOption.MINIMUM_PLAYERS, minimumPlayers);
	}

	/**
	 * Get arena map name.
	 *
	 * @return arena map name, it's not arena id
	 * @see #getId()
	 */
	public String getMapName() {
		return mapName;
	}

	/**
	 * Set arena map name.
	 *
	 * @param mapname new map name, it's not arena id
	 */
	public void setMapName(String mapname) {
		this.mapName = mapname;
	}

	/**
	 * Get timer of arena.
	 *
	 * @return timer of lobby time / time to next wave
	 */
	public int getTimer() {
		return getOption(ArenaOption.TIMER);
	}

	/**
	 * Modify game timer.
	 *
	 * @param timer timer of lobby / time to next wave
	 */
	public void setTimer(int timer) {
		setOptionValue(ArenaOption.TIMER, timer);
	}

	/**
	 * Return maximum players arena can handle.
	 *
	 * @return maximum players arena can handle
	 */
	public int getMaximumPlayers() {
		return getOption(ArenaOption.MAXIMUM_PLAYERS);
	}

	/**
	 * Set maximum players arena can handle.
	 *
	 * @param maximumPlayers how many players arena can handle
	 */
	public void setMaximumPlayers(int maximumPlayers) {
		setOptionValue(ArenaOption.MAXIMUM_PLAYERS, maximumPlayers);
	}

	/**
	 * Return game state of arena.
	 *
	 * @return game state of arena
	 * @see ArenaState
	 */
	public ArenaState getArenaState() {
		return arenaState;
	}

	/**
	 * Set game state of arena.
	 *
	 * @param arenaState new game state of arena
	 * @see ArenaState
	 */
	public void setArenaState(ArenaState arenaState) {
		this.arenaState = arenaState;
		OITCGameStateChangeEvent gameStateChangeEvent = new OITCGameStateChangeEvent(this, getArenaState());
		Bukkit.getPluginManager().callEvent(gameStateChangeEvent);
	}

	/**
	 * Get all players in arena.
	 *
	 * @return set of players in arena
	 */
	public Set<Player> getPlayers() {
		return players;
	}

	public void teleportToLobby(Player player) {
		player.setFoodLevel(20);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		player.setWalkSpeed(0.2f);
		Location location = getLobbyLocation();

		if (location == null) {
			System.out.print("Lobby location isn't intialized for arena " + getId());
			return;
		}

		player.teleport(location);
	}

	/**
	 * Executes boss bar action for arena
	 *
	 * @param action add or remove a player from boss bar
	 * @param p player
	 */
	public void doBarAction(BarAction action, Player p) {
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BOSSBAR_ENABLED)) {
			return;
		}

		switch (action) {
			case ADD:
				gameBar.addPlayer(p);
				break;
			case REMOVE:
				gameBar.removePlayer(p);
				break;
			default:
				break;
		}
	}

	/**
	 * Get lobby location of arena.
	 *
	 * @return lobby location of arena
	 */
	public Location getLobbyLocation() {
		return gameLocations.get(GameLocation.LOBBY);
	}

	/**
	 * Set lobby location of arena.
	 *
	 * @param loc new lobby location of arena
	 */
	public void setLobbyLocation(Location loc) {
		gameLocations.put(GameLocation.LOBBY, loc);
	}

	public void teleportToStartLocation(Player player) {
		player.teleport(playerSpawnPoints.get(random.nextInt(playerSpawnPoints.size())));
	}
	
	public Location getRandomSpawnPoint() {
		return playerSpawnPoints.get(random.nextInt(playerSpawnPoints.size()));
	}
	
	private void teleportAllToStartLocation() {
		for (int i = 0; i <= getPlayersLeft().size() - 1; i++) {
			getPlayersLeft().get(i).teleport(playerSpawnPoints.get(i));
		}
	}

	public void teleportAllToEndLocation() {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED) && ConfigUtils.getConfig(plugin, "bungee").getBoolean("End-Location-Hub", true)) {
			getPlayers().forEach(plugin.getBungeeManager()::connectToHub);
			return;
		}

		Location location = getEndLocation();

		if (location == null) {
			location = getLobbyLocation();
			System.out.print("End location for arena " + getId() + " isn't intialized!");
		}

		if (location != null) {
			for (Player player : getPlayers()) {
				player.teleport(location);
			}
		}
	}

	public void broadcastMessage(String message) {
		for (Player player : players) {
			player.sendMessage(message);
		}
	}

	public void teleportToEndLocation(Player player) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED) && ConfigUtils.getConfig(plugin, "bungee").getBoolean("End-Location-Hub", true)) {
			plugin.getBungeeManager().connectToHub(player);
			return;
		}

		Location location = getEndLocation();

		if (location == null) {
			location = getLobbyLocation();
			System.out.print("End location for arena " + getId() + " isn't intialized!");
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

	/**
	 * Get end location of arena.
	 *
	 * @return end location of arena
	 */
	public Location getEndLocation() {
		return gameLocations.get(GameLocation.END);
	}

	/**
	 * Set end location of arena.
	 *
	 * @param endLoc new end location of arena
	 */
	public void setEndLocation(Location endLoc) {
		gameLocations.put(GameLocation.END, endLoc);
	}

	public void start() {
		Debugger.debug("[{0}] Game instance started", getId());
		this.runTaskTimer(plugin, 20L, 20L);
		this.setArenaState(ArenaState.RESTARTING);
	}

	void addPlayer(Player player) {
		players.add(player);
	}

	void removePlayer(Player player) {
		if (player != null) {
			players.remove(player);
		}
	}

	public List<Player> getPlayersLeft() {
		List<Player> players = new ArrayList<>();
		for (User user : plugin.getUserManager().getUsers(this)) {
			if (!user.isSpectator()) {
				players.add(user.getPlayer());
			}
		}
		return players;
	}

	void showPlayers() {
		for (Player player : getPlayers()) {
			for (Player p : getPlayers()) {
				player.showPlayer(plugin, p);
				p.showPlayer(plugin, player);
			}
		}
	}

	public int getOption(ArenaOption option) {
		return arenaOptions.get(option);
	}

	public void setOptionValue(ArenaOption option, int value) {
		arenaOptions.put(option, value);
	}

	public enum BarAction {
		ADD, REMOVE
	}

	public enum GameLocation {
		LOBBY, END
	}
}