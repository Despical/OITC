package me.despical.oitc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.database.MysqlDatabase;
import me.despical.commonsbox.scoreboard.ScoreboardLib;
import me.despical.commonsbox.serializer.InventorySerializer;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaUtils;
import me.despical.oitc.commands.CommandHandler;
import me.despical.oitc.events.ChatEvents;
import me.despical.oitc.events.Events;
import me.despical.oitc.events.JoinEvent;
import me.despical.oitc.events.LobbyEvent;
import me.despical.oitc.events.QuitEvent;
import me.despical.oitc.events.spectator.SpectatorEvents;
import me.despical.oitc.events.spectator.SpectatorItemEvents;
import me.despical.oitc.handlers.BowTrailsHandler;
import me.despical.oitc.handlers.BungeeManager;
import me.despical.oitc.handlers.ChatManager;
import me.despical.oitc.handlers.PermissionsManager;
import me.despical.oitc.handlers.PlaceholderManager;
import me.despical.oitc.handlers.items.SpecialItem;
import me.despical.oitc.handlers.rewards.RewardsFactory;
import me.despical.oitc.handlers.sign.ArenaSign;
import me.despical.oitc.handlers.sign.SignManager;
import me.despical.oitc.user.User;
import me.despical.oitc.user.UserManager;
import me.despical.oitc.user.data.MysqlManager;
import me.despical.oitc.utils.Debugger;
import me.despical.oitc.utils.ExceptionLogHandler;
import me.despical.oitc.utils.MessageUtils;
import me.despical.oitc.utils.UpdateChecker;
import me.despical.oitc.utils.Utils;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Main extends JavaPlugin {

	private ExceptionLogHandler exceptionLogHandler;
	private String version;
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
			return;
		}
		exceptionLogHandler = new ExceptionLogHandler(this);
		saveDefaultConfig();
		if (getDescription().getVersion().contains("d")) {
			Debugger.setEnabled(true);
		} else {
			Debugger.setEnabled(getConfig().getBoolean("Debug-Messages", false));
		}
		Debugger.debug(Level.INFO, "Initialization start");
		if (getConfig().getBoolean("Developer-Mode", false)) {
			Debugger.deepDebug(true);
			Debugger.debug(Level.INFO, "Deep debug enabled");
			for (String listenable : new ArrayList<>(getConfig().getStringList("Listanable-Performances"))) {
				Debugger.monitorPerformance(listenable);
			}
		}
		long start = System.currentTimeMillis();
		
		configPreferences = new ConfigPreferences(this);
		setupFiles();
		initializeClasses();
//		checkUpdate(); not implemented yet

		Debugger.debug(Level.INFO, "Initialization finished took {0} ms", System.currentTimeMillis() - start);
		if (configPreferences.getOption(ConfigPreferences.Option.NAMETAGS_HIDDEN)) {
			Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
				for (Player player : Bukkit.getOnlinePlayers()) {
					ArenaUtils.updateNameTagsVisibility(player);
				}
			}, 60, 140);
		}
	}
	
	private boolean validateIfPluginShouldStart() {
		version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
		if (!(version.equalsIgnoreCase("v1_12_R1") || version.equalsIgnoreCase("v1_13_R1") 
			|| version.equalsIgnoreCase("v1_13_R2") || version.equalsIgnoreCase("v1_14_R1") 
			|| version.equalsIgnoreCase("v1_15_R1") || version.equalsIgnoreCase("v1_16_R1"))) {
			MessageUtils.thisVersionIsNotSupported();
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Your server version is not supported by One in the Chamber!");
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Sadly, we must shut off. Maybe you consider changing your server version?");
			forceDisable = true;
			getServer().getPluginManager().disablePlugin(this);
			return false;
		} try {
			Class.forName("org.spigotmc.SpigotConfig");
		} catch (Exception e) {
			MessageUtils.thisVersionIsNotSupported();
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Your server software is not supported by One in the Chamber!");
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "We support only Spigot and Spigot forks only! Shutting off...");
			forceDisable = true;
			getServer().getPluginManager().disablePlugin(this);
			return false;
		}
		return true;
	}
	
	@Override
	public void onDisable() {
		if (forceDisable) {
			return;
		}
		Debugger.debug(Level.INFO, "System disable initialized");
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
					for (PotionEffect pe : player.getActivePotionEffects()) {
						player.removePotionEffect(pe.getType());
					}
					player.setWalkSpeed(0.2f);
				}
			}
			arena.teleportAllToEndLocation();
		}
		Debugger.debug(Level.INFO, "System disable finished took {0} ms", System.currentTimeMillis() - start);
	}
	
	private void initializeClasses() {
		ScoreboardLib.setPluginInstance(this);
		if (getConfig().getBoolean("BungeeActivated", false)) {
			bungeeManager = new BungeeManager(this);
		}
		if (configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
			FileConfiguration config = ConfigUtils.getConfig(this, "mysql");
			database = new MysqlDatabase(config.getString("user"), config.getString("password"), config.getString("address"));
		}
		chatManager = new ChatManager(this);
		userManager = new UserManager(this);
		Utils.init(this);
		ArenaSign.init(this);
		SpecialItem.loadAll();
		PermissionsManager.init();
		new SpectatorEvents(this);
		new QuitEvent(this);
		new JoinEvent(this);
		new ChatEvents(this);
		ArenaRegistry.registerArenas();
		new Events(this);
		new LobbyEvent(this);
		new SpectatorItemEvents(this);
		rewardsFactory = new RewardsFactory(this);
		signManager = new SignManager(this);
		registerSoftDependenciesAndServices();
		commandHandler = new CommandHandler(this);
		new BowTrailsHandler(this);
	}
	
	private void registerSoftDependenciesAndServices() {
		Debugger.debug(Level.INFO, "Hooking into soft dependencies");
		long start = System.currentTimeMillis();

		startPluginMetrics();
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			Debugger.debug(Level.INFO, "Hooking into PlaceholderAPI");
			new PlaceholderManager().register();
		}
		Debugger.debug(Level.INFO, "Hooked into soft dependencies took {0} ms", System.currentTimeMillis() - start);
	}
	
	private void startPluginMetrics() {
		Metrics metrics = new Metrics(this, 8092);
		metrics.addCustomChart(new Metrics.SimplePie("database_enabled", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED))));
		metrics.addCustomChart(new Metrics.SimplePie("bungeecord_hooked", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.BUNGEE_ENABLED))));
		metrics.addCustomChart(new Metrics.SimplePie("update_notifier", () -> {
			if (getConfig().getBoolean("Update-Notifier.Enabled", true)) {
				if (getConfig().getBoolean("Update-Notifier.Notify-Beta-Versions", true)) {
					return "Enabled with beta notifier";
				} else {
					return "Enabled";
				}
			} else {
				if (getConfig().getBoolean("Update-Notifier.Notify-Beta-Versions", true)) {
					return "Beta notifier only";
				} else {
					return "Disabled";
				}
			}
		}));
	}
	
	private void checkUpdate() {
		if (!getConfig().getBoolean("Update-Notifier.Enabled", true)) {
			return;
		}
		UpdateChecker.init(this, 1).requestUpdateCheck().whenComplete((result, exception) -> {
			if (!result.requiresUpdate()) {
				return;
			}
			if (result.getNewestVersion().contains("b")) {
				if (getConfig().getBoolean("Update-Notifier.Notify-Beta-Versions", true)) {
					Bukkit.getConsoleSender().sendMessage("[OITC] Found a new beta version available: v" + result.getNewestVersion());
					Bukkit.getConsoleSender().sendMessage("[OITC] Download it on SpigotMC:");
					Bukkit.getConsoleSender().sendMessage("[OITC] spigotmc.org/resources/");
				}
				return;
			}
			MessageUtils.updateIsHere();
			Bukkit.getConsoleSender().sendMessage("[OITC] Found a new version available: v" + result.getNewestVersion());
			Bukkit.getConsoleSender().sendMessage("[OITC] Download it SpigotMC:");
			Bukkit.getConsoleSender().sendMessage("[OITC] spigotmc.org/resources/");
		});
	}
	
	private void setupFiles() {
		for (String fileName : Arrays.asList("arenas", "bungee", "rewards", "stats", "lobbyitems", "mysql", "messages")) {
			File file = new File(getDataFolder() + File.separator + fileName + ".yml");
			if (!file.exists()) {
				saveResource(fileName + ".yml", false);
			}
		}
	}

	public boolean is1_12_R1() {
		return version.equalsIgnoreCase("v1_12_R1");
	}

	public boolean is1_14_R1() {
		return version.equalsIgnoreCase("v1_14_R1");
	}

	public boolean is1_15_R1() {
		return version.equalsIgnoreCase("v1_15_R1");
	}

	public boolean is1_16_R1() {
		return version.equalsIgnoreCase("v1_16_R1");
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
			for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
				if (!stat.isPersistent()) {
					continue;
				}
				if (userManager.getDatabase() instanceof MysqlManager) {
					((MysqlManager) userManager.getDatabase()).getDatabase().executeUpdate("UPDATE " + ((MysqlManager) userManager.getDatabase()).getTableName() + " SET " + stat.getName() + "=" + user.getStat(stat) + " WHERE UUID='" + user.getPlayer().getUniqueId().toString() + "';");
					Debugger.debug(Level.INFO, "Executed MySQL: " + "UPDATE " + ((MysqlManager) userManager.getDatabase()).getTableName() + " " + stat.getName() + "=" + user.getStat(stat) + " WHERE UUID='" + user.getPlayer().getUniqueId().toString() + "';");
					continue;
				}
				userManager.getDatabase().saveStatistic(user, stat);
			}
		}
	}
}