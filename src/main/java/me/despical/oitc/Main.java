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

package me.despical.oitc;

import me.despical.commonsbox.compat.VersionResolver;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.database.MysqlDatabase;
import me.despical.commonsbox.scoreboard.ScoreboardLib;
import me.despical.commonsbox.serializer.InventorySerializer;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaUtils;
import me.despical.oitc.commands.CommandHandler;
import me.despical.oitc.events.*;
import me.despical.oitc.events.spectator.SpectatorEvents;
import me.despical.oitc.events.spectator.SpectatorItemEvents;
import me.despical.oitc.handlers.*;
import me.despical.oitc.handlers.items.SpecialItem;
import me.despical.oitc.handlers.rewards.RewardsFactory;
import me.despical.oitc.handlers.sign.SignManager;
import me.despical.oitc.user.User;
import me.despical.oitc.user.UserManager;
import me.despical.oitc.user.data.MysqlManager;
import me.despical.oitc.utils.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Main extends JavaPlugin {

	private ExceptionLogHandler exceptionLogHandler;
	private boolean forceDisable = false;
	private BungeeManager bungeeManager;	
	private RewardsFactory rewardsFactory;
	private MysqlDatabase database;
	private SignManager signManager;
	private ConfigPreferences configPreferences;
	private CommandHandler commandHandler;
	private ChatManager chatManager;
	private UserManager userManager;
	
	@Override
	public void onEnable() {
		if (!validateIfPluginShouldStart()) {
			forceDisable = true;
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		exceptionLogHandler = new ExceptionLogHandler(this);
		saveDefaultConfig();

		Debugger.setEnabled(getDescription().getVersion().contains("debug") || getConfig().getBoolean("Debug-Messages"));

		Debugger.debug("Initialization start");
		if (getConfig().getBoolean("Developer-Mode")) {
			Debugger.deepDebug(true);
			Debugger.debug("Deep debug enabled");
			getConfig().getStringList("Listenable-Performances").forEach(Debugger::monitorPerformance);
		}

		long start = System.currentTimeMillis();
		configPreferences = new ConfigPreferences(this);

		setupFiles();
		initializeClasses();
		checkUpdate();

		Debugger.debug("Initialization finished took {0} ms", System.currentTimeMillis() - start);
		if (configPreferences.getOption(ConfigPreferences.Option.NAMETAGS_HIDDEN)) {
			Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, () ->
				Bukkit.getOnlinePlayers().forEach(ArenaUtils::updateNameTagsVisibility), 60, 140);
		}
	}
	
	private boolean validateIfPluginShouldStart() {
		if (VersionResolver.isCurrentLower(VersionResolver.ServerVersion.v1_12_R1)) {
			MessageUtils.thisVersionIsNotSupported();
			Debugger.sendConsoleMessage("&cYour server version is not supported by One in the Chamber!");
			Debugger.sendConsoleMessage("&cSadly, we must shut off. Maybe you consider changing your server version?");
			return false;
		} try {
			Class.forName("org.spigotmc.SpigotConfig");
		} catch (Exception e) {
			MessageUtils.thisVersionIsNotSupported();
			Debugger.sendConsoleMessage("&cYour server software is not supported by One in the Chamber!");
			Debugger.sendConsoleMessage("&cWe support only Spigot and Spigot forks only! Shutting off...");
			return false;
		}

		return true;
	}
	
	@Override
	public void onDisable() {
		if (forceDisable) {
			return;
		}

		Debugger.debug("System disable initialized");
		long start = System.currentTimeMillis();

		Bukkit.getLogger().removeHandler(exceptionLogHandler);
		saveAllUserStatistics();
		
		if (configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
			database.shutdownConnPool();
		}

		for (Arena arena : ArenaRegistry.getArenas()) {
			arena.getScoreboardManager().stopAllScoreboards();

			for (Player player : arena.getPlayers()) {
				arena.doBarAction(Arena.BarAction.REMOVE, player);
				arena.teleportToEndLocation(player);
				player.setFlySpeed(0.1f);

				if (configPreferences.getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
					InventorySerializer.loadInventory(this, player);
				} else {
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
					player.setWalkSpeed(0.2f);
				}
			}

			arena.teleportAllToEndLocation();
		}

		Debugger.debug("System disable finished took {0} ms", System.currentTimeMillis() - start);
	}
	
	private void initializeClasses() {
		ScoreboardLib.setPluginInstance(this);
		chatManager = new ChatManager(this);

		if (configPreferences.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			bungeeManager = new BungeeManager(this);
		}

		if (configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
			FileConfiguration config = ConfigUtils.getConfig(this, "mysql");
			database = new MysqlDatabase(config.getString("user"), config.getString("password"), config.getString("address"));
		}

		userManager = new UserManager(this);
		SpecialItem.loadAll();
		PermissionsManager.init();
		new SpectatorEvents(this);
		new QuitEvent(this);
		new JoinEvent(this);
		new ChatEvents(this);
		signManager = new SignManager(this);
		ArenaRegistry.registerArenas();
		signManager.loadSigns();
		signManager.updateSigns();
		new Events(this);
		new LobbyEvent(this);
		new SpectatorItemEvents(this);
		rewardsFactory = new RewardsFactory(this);
		registerSoftDependenciesAndServices();
		commandHandler = new CommandHandler(this);
		new BowTrailsHandler(this);
	}
	
	private void registerSoftDependenciesAndServices() {
		Debugger.debug("Hooking into soft dependencies");
		long start = System.currentTimeMillis();

		startPluginMetrics();
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			Debugger.debug("Hooking into PlaceholderAPI");
			new PlaceholderManager().register();
		}

		Debugger.debug("Hooked into soft dependencies took {0} ms", System.currentTimeMillis() - start);
	}
	
	private void startPluginMetrics() {
		Metrics metrics = new Metrics(this, 8118);

		if (!metrics.isEnabled()) {
			return;
		}

		metrics.addCustomChart(new Metrics.SimplePie("database_enabled", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED))));
		metrics.addCustomChart(new Metrics.SimplePie("bungeecord_hooked", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.BUNGEE_ENABLED))));
		metrics.addCustomChart(new Metrics.SimplePie("update_notifier", () -> {
			if (getConfig().getBoolean("Update-Notifier.Enabled", true)) {
				return getConfig().getBoolean("Update-Notifier.Notify-Beta-Versions", true) ? "Enabled with beta notifier" : "Enabled";
			}

			return getConfig().getBoolean("Update-Notifier.Notify-Beta-Versions", true) ? "Beta notifier only" : "Disabled";
		}));
	}
	
	private void checkUpdate() {
		if (!getConfig().getBoolean("Update-Notifier.Enabled", true)) {
			return;
		}

		UpdateChecker.init(this, 81185).requestUpdateCheck().whenComplete((result, exception) -> {
			if (!result.requiresUpdate()) {
				return;
			}

			if (result.getNewestVersion().contains("b")) {
				if (getConfig().getBoolean("Update-Notifier.Notify-Beta-Versions", true)) {
					Debugger.sendConsoleMessage("[OITC] Found a new beta version available: v" + result.getNewestVersion());
					Debugger.sendConsoleMessage("[OITC] Download it on SpigotMC:");
					Debugger.sendConsoleMessage("[OITC] https://www.spigotmc.org/resources/one-in-the-chamber-1-12-1-16-4.81185/");
				}

				return;
			}

			MessageUtils.updateIsHere();
			Debugger.sendConsoleMessage("[OITC] Found a new version available: v" + result.getNewestVersion());
			Debugger.sendConsoleMessage("[OITC] Download it SpigotMC:");
			Debugger.sendConsoleMessage("[OITC] https://www.spigotmc.org/resources/one-in-the-chamber-1-12-1-16-4.81185/");
		});
	}

	private void setupFiles() {
		for (String fileName : Arrays.asList("arenas", "bungee", "rewards", "stats", "items", "mysql", "messages")) {
			File file = new File(getDataFolder() + File.separator + fileName + ".yml");

			if (!file.exists()) {
				saveResource(fileName + ".yml", false);
			}
		}
	}
	
	public RewardsFactory getRewardsFactory() {
		return rewardsFactory;
	}
	
	public BungeeManager getBungeeManager() {
		return bungeeManager;
	}
	
	public ConfigPreferences getConfigPreferences() {
		return configPreferences;
	}
	
	public MysqlDatabase getMysqlDatabase() {
		return database;
	}
	
	public SignManager getSignManager() {
		return signManager;
	}

	public ChatManager getChatManager() {
		return chatManager;
	}
	
	public CommandHandler getCommandHandler() {
		return commandHandler;
	}

	public UserManager getUserManager() {
		return userManager;
	}

	private void saveAllUserStatistics() {
		for (Player player : getServer().getOnlinePlayers()) {
			User user = userManager.getUser(player);

			if (userManager.getDatabase() instanceof MysqlManager) {
				StringBuilder update = new StringBuilder(" SET ");

				for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
					if (!stat.isPersistent()) continue;
					if (update.toString().equalsIgnoreCase(" SET ")) {
						update.append(stat.getName()).append("'='").append(user.getStat(stat));
					}

					update.append(", ").append(stat.getName()).append("'='").append(user.getStat(stat));
				}

				String finalUpdate = update.toString();
				((MysqlManager) userManager.getDatabase()).getDatabase().executeUpdate("UPDATE " + ((MysqlManager) getUserManager().getDatabase()).getTableName() + finalUpdate + " WHERE UUID='" + user.getPlayer().getUniqueId().toString() + "';");
				continue;
			}

			for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
				userManager.getDatabase().saveStatistic(user, stat);
			}
		}
	}
}