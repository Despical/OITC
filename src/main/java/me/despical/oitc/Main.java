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

import me.despical.commandframework.CommandFramework;
import me.despical.commons.compat.VersionResolver;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.database.MysqlDatabase;
import me.despical.commons.exception.ExceptionLogHandler;
import me.despical.commons.scoreboard.ScoreboardLib;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.util.Collections;
import me.despical.commons.util.JavaVersion;
import me.despical.commons.util.LogUtils;
import me.despical.commons.util.UpdateChecker;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaUtils;
import me.despical.oitc.commands.TabCompletion;
import me.despical.oitc.commands.AdminCommands;
import me.despical.oitc.commands.PlayerCommands;
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
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Main extends JavaPlugin {

	private boolean forceDisable = false;

	private ExceptionLogHandler exceptionLogHandler;
	private BungeeManager bungeeManager;
	private RewardsFactory rewardsFactory;
	private MysqlDatabase database;
	private SignManager signManager;
	private ConfigPreferences configPreferences;
	private CommandFramework commandFramework;
	private ChatManager chatManager;
	private UserManager userManager;
	private PermissionsManager permissionsManager;
	
	@Override
	public void onEnable() {
		if (forceDisable = !validateIfPluginShouldStart()) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		exceptionLogHandler = new ExceptionLogHandler(this);
		exceptionLogHandler.setMainPackage("me.despical.oitc");
		exceptionLogHandler.addBlacklistedClass("me.despical.oitc.user.data.MysqlManager", "me.despical.commons.database.MysqlDatabase");
		exceptionLogHandler.setRecordMessage("[OITC] We have found a bug in the code. Contact us at our official GitHub repo (Repo link: https://github.com/Despical/OITC) with the following error given above!");

		saveDefaultConfig();

		if (getConfig().getBoolean("Debug-Messages")) {
			LogUtils.setLoggerName("OITC");
			LogUtils.enableLogging();
			LogUtils.log("Initialization started.");
		}

		long start = System.currentTimeMillis();
		configPreferences = new ConfigPreferences(this);

		setupFiles();
		initializeClasses();
		checkUpdate();

		LogUtils.log("Initialization finished took {0} ms.", System.currentTimeMillis() - start);

		if (configPreferences.getOption(ConfigPreferences.Option.NAMETAGS_HIDDEN)) {
			getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> getServer().getOnlinePlayers().forEach(ArenaUtils::updateNameTagsVisibility), 60, 140);
		}
	}
	
	private boolean validateIfPluginShouldStart() {
		if (!VersionResolver.isCurrentBetween(VersionResolver.ServerVersion.v1_9_R1, VersionResolver.ServerVersion.v1_18_R1)) {
			LogUtils.sendConsoleMessage("&cYour server version is not supported by One in the Chamber!");
			LogUtils.sendConsoleMessage("&cSadly, we must shut off. Maybe you consider changing your server version?");
			return false;
		}

		if (JavaVersion.getCurrentVersion().isAt(JavaVersion.JAVA_8)) {
			LogUtils.sendConsoleMessage("&cThis plugin won't support Java 8 in future updates.");
			LogUtils.sendConsoleMessage("&cSo, maybe consider to update your version, right?");
		}

		try {
			Class.forName("org.spigotmc.SpigotConfig");
		} catch (Exception exception) {
			LogUtils.sendConsoleMessage("&cYour server software is not supported by One in the Chamber!");
			LogUtils.sendConsoleMessage("&cWe support only Spigot and Spigot forks only! Shutting off...");
			return false;
		}

		return true;
	}
	
	@Override
	public void onDisable() {
		if (forceDisable) return;

		LogUtils.log("System disable initialized.");
		long start = System.currentTimeMillis();

		getServer().getLogger().removeHandler(exceptionLogHandler);
		saveAllUserStatistics();
		
		if (configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
			database.shutdownConnPool();
		}

		for (Arena arena : ArenaRegistry.getArenas()) {
			arena.getScoreboardManager().stopAllScoreboards();

			for (Player player : arena.getPlayers()) {
				arena.teleportToEndLocation(player);
				arena.doBarAction(Arena.BarAction.REMOVE, player);
				arena.getScoreboardManager().removeScoreboard(player);

				player.setFlySpeed(.1F);
				player.setWalkSpeed(.2F);

				if (configPreferences.getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
					InventorySerializer.loadInventory(this, player);
				} else {
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
				}
			}

			arena.teleportAllToEndLocation();
		}

		LogUtils.log("System disable finished took {0} ms.", System.currentTimeMillis() - start);
	}
	
	private void initializeClasses() {
		ScoreboardLib.setPluginInstance(this);
		chatManager = new ChatManager(this);

		if (configPreferences.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			bungeeManager = new BungeeManager(this);
		}

		if (configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
			database = new MysqlDatabase(ConfigUtils.getConfig(this, "mysql"));
		}

		SpecialItem.init(this);

		userManager = new UserManager(this);
		signManager = new SignManager(this);
		ArenaRegistry.registerArenas();
		signManager.loadSigns();
		rewardsFactory = new RewardsFactory(this);
		commandFramework = new CommandFramework(this);
		permissionsManager = new PermissionsManager(this);

		new AdminCommands(this);
		new PlayerCommands(this);
		new TabCompletion(this);

		new SpectatorEvents(this);
		new QuitEvent(this);
		new JoinEvent(this);
		new ChatEvents(this);
		new Events(this);
		new LobbyEvent(this);
		new SpectatorItemEvents(this);
		new LegacyEvents(this);

		registerSoftDependenciesAndServices();
	}
	
	private void registerSoftDependenciesAndServices() {
		LogUtils.log("Hooking into soft dependencies");
		long start = System.currentTimeMillis();

		startPluginMetrics();

		if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			LogUtils.log("Hooking into PlaceholderAPI");
			new PlaceholderManager(this);
		}

		LogUtils.log("Hooked into soft dependencies took {0} ms", System.currentTimeMillis() - start);
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
			if (result.requiresUpdate()) {
				LogUtils.sendConsoleMessage("[OITC] Found a new version available: v" + result.getNewestVersion());
				LogUtils.sendConsoleMessage("[OITC] Download it SpigotMC:");
				LogUtils.sendConsoleMessage("[OITC] https://www.spigotmc.org/resources/one-in-the-chamber.81185/");
			}
		});
	}

	private void setupFiles() {
		Collections.streamOf("arenas", "bungee", "rewards", "stats", "items", "mysql", "messages").filter(name -> !new File(getDataFolder(),name + ".yml").exists()).forEach(name -> saveResource(name + ".yml", false));
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
	
	public CommandFramework getCommandFramework() {
		return commandFramework;
	}

	public UserManager getUserManager() {
		return userManager;
	}

	public PermissionsManager getPermissionsManager() {
		return permissionsManager;
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

				MysqlManager database = (MysqlManager) userManager.getDatabase();
				String finalUpdate = update.toString();
				database.getDatabase().executeUpdate("UPDATE " + database.getTableName() + finalUpdate + " WHERE UUID='" + user.getPlayer().getUniqueId().toString() + "';");
				continue;
			}

			userManager.getDatabase().saveAllStatistic(user);
		}
	}
}