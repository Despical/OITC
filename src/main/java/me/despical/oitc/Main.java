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

package me.despical.oitc;

import me.despical.commandframework.CommandFramework;
import me.despical.commons.scoreboard.ScoreboardLib;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.util.Collections;
import me.despical.commons.util.UpdateChecker;
import me.despical.oitc.addons.AddonManager;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaUtils;
import me.despical.oitc.command.AdminCommands;
import me.despical.oitc.command.PlayerCommands;
import me.despical.oitc.events.EventListener;
import me.despical.oitc.handlers.BungeeManager;
import me.despical.oitc.handlers.ChatManager;
import me.despical.oitc.handlers.PermissionsManager;
import me.despical.oitc.handlers.PlaceholderManager;
import me.despical.oitc.handlers.items.GameItemManager;
import me.despical.oitc.handlers.language.LanguageManager;
import me.despical.oitc.handlers.rewards.RewardsFactory;
import me.despical.oitc.handlers.sign.SignManager;
import me.despical.oitc.user.User;
import me.despical.oitc.user.UserManager;
import me.despical.oitc.user.data.MySQLStatistics;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Main extends JavaPlugin {

	private ArenaRegistry arenaRegistry;
	private BungeeManager bungeeManager;
	private RewardsFactory rewardsFactory;
	private SignManager signManager;
	private ConfigPreferences configPreferences;
	private ChatManager chatManager;
	private AddonManager addonManager;
	private UserManager userManager;
	private PermissionsManager permissionsManager;
	private GameItemManager gameItemManager;
	private LanguageManager languageManager;
	private CommandFramework commandFramework;

	@Override
	public void onEnable() {
		initializeClasses();
		checkUpdate();

		getLogger().info("Initialization finished.");
		getLogger().info("Join our Discord server: https://discord.gg/uXVU8jmtpU");
	}

	@Override
	public void onDisable() {
		saveAllUserStatistics();
		
		for (Arena arena : arenaRegistry.getArenas()) {
			arena.getScoreboardManager().stopAllScoreboards();

			for (Player player : arena.getPlayers()) {
				User user = userManager.getUser(player);
				user.resetAttackCooldown();

				arena.teleportToEndLocation(player);
				arena.getGameBar().doBarAction(user, 0);
				arena.getScoreboardManager().removeScoreboard(player);

				player.setFlySpeed(.1F);
				player.setWalkSpeed(.2F);
				player.getInventory().clear();
				player.getInventory().setArmorContents(null);
				player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));

				if (configPreferences.getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
					InventorySerializer.loadInventory(this, player);
				}
			}

			arena.teleportAllToEndLocation();
		}
	}
	
	private void initializeClasses() {
		setupConfigurationFiles();

		configPreferences = new ConfigPreferences(this);
		chatManager = new ChatManager(this);
		addonManager = new AddonManager(this);
		languageManager = new LanguageManager(this);
		userManager = new UserManager(this);
		signManager = new SignManager(this);
		arenaRegistry = new ArenaRegistry(this);
		signManager.loadSigns();
		rewardsFactory = new RewardsFactory(this);
		permissionsManager = new PermissionsManager(this);
		gameItemManager = new GameItemManager(this);
		commandFramework = new CommandFramework(this);

		if (configPreferences.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) bungeeManager = new BungeeManager(this);
		if (chatManager.isPapiEnabled()) new PlaceholderManager(this);

		ScoreboardLib.setPluginInstance(this);
		EventListener.registerEvents(this);
		User.cooldownHandlerTask();

		startPluginMetrics();

		new AdminCommands();
		new PlayerCommands();

		if (getOption(ConfigPreferences.Option.NAME_TAGS_HIDDEN)) {
			getServer().getScheduler().runTaskTimer(this, () -> getServer().getOnlinePlayers().forEach(ArenaUtils::updateNameTagsVisibility), 60, 140);
		}
	}

	private void startPluginMetrics() {
		Metrics metrics = new Metrics(this, 8118);

		metrics.addCustomChart(new SimplePie("database_enabled", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED))));
		metrics.addCustomChart(new SimplePie("bungeecord_hooked", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.BUNGEE_ENABLED))));
		metrics.addCustomChart(new SimplePie("update_notifier", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.UPDATE_NOTIFIER_ENABLED))));
	}
	
	private void checkUpdate() {
		if (!getOption(ConfigPreferences.Option.UPDATE_NOTIFIER_ENABLED)) {
			return;
		}

		UpdateChecker.init(this, 81185).onNewUpdate(result -> getLogger().info("Found a new version available: v" + result.getNewestVersion()));
	}

	private void setupConfigurationFiles() {
		saveDefaultConfig();

		Collections.streamOf("arenas", "bungee", "rewards", "stats", "items", "mysql", "messages").filter(name -> !new File(getDataFolder(),name + ".yml").exists()).forEach(name -> saveResource(name + ".yml", false));
	}

	@NotNull
	public RewardsFactory getRewardsFactory() {
		return rewardsFactory;
	}

	@NotNull
	public BungeeManager getBungeeManager() {
		return bungeeManager;
	}

	public SignManager getSignManager() {
		return signManager;
	}

	@NotNull
	public ChatManager getChatManager() {
		return chatManager;
	}

	@NotNull
	public AddonManager getAddonManager() {
		return addonManager;
	}

	@NotNull
	public UserManager getUserManager() {
		return userManager;
	}

	@NotNull
	public PermissionsManager getPermissionsManager() {
		return permissionsManager;
	}

	@NotNull
	public GameItemManager getGameItemManager() {
		return gameItemManager;
	}

	@NotNull
	public CommandFramework getCommandFramework() {
		return commandFramework;
	}

	@NotNull
	public ArenaRegistry getArenaRegistry() {
		return arenaRegistry;
	}

	@SuppressWarnings("unused")
	@NotNull
	public LanguageManager getLanguageManager() {
		return languageManager;
	}

	public boolean getOption(ConfigPreferences.Option option) {
		return configPreferences.getOption(option);
	}

	public void reload() {
		this.reloadConfig();
		this.configPreferences.reload();
		this.chatManager.reload();
		this.permissionsManager.loadPermissions();
		this.gameItemManager.reloadItems();
		this.addonManager.reload();
		this.rewardsFactory.reload();
	}

	private void saveAllUserStatistics() {
		var userDatabase = userManager.getDatabase();

		for (User user : userManager.getUsers()) {
			if (userDatabase instanceof MySQLStatistics mysqlManager) {
				StringBuilder builder = new StringBuilder(" SET ");

				for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
					if (!stat.isPersistent()) continue;

					int value = user.getStat(stat);
					String name = stat.getName();

					if (builder.toString().equalsIgnoreCase(" SET ")) {
						builder.append(name).append("=").append(value);
					}

					builder.append(", ").append(name).append("=").append(value);
				}

				String update = builder.toString();

				mysqlManager.getDatabase().executeUpdate(String.format("UPDATE %s%s WHERE UUID='%s';", mysqlManager.getTableName(), update, user.getUniqueId().toString()));
				continue;
			}

			userDatabase.saveAllStatistic(user);
		}

		userDatabase.shutdown();
	}
}