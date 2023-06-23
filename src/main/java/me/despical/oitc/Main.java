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

package me.despical.oitc;

import me.despical.oitc.handlers.items.GameItemManager;
import me.despical.oitc.handlers.language.LanguageManager;
import org.bstats.bukkit.Metrics;
import me.despical.commons.database.MysqlDatabase;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.scoreboard.ScoreboardLib;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.util.Collections;
import me.despical.commons.util.UpdateChecker;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaUtils;
import me.despical.oitc.commands.CommandHandler;
import me.despical.oitc.events.*;
import me.despical.oitc.handlers.*;
import me.despical.oitc.handlers.rewards.RewardsFactory;
import me.despical.oitc.handlers.sign.SignManager;
import me.despical.oitc.user.User;
import me.despical.oitc.user.UserManager;
import me.despical.oitc.user.data.MysqlManager;
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

	private BungeeManager bungeeManager;
	private RewardsFactory rewardsFactory;
	private MysqlDatabase database;
	private SignManager signManager;
	private ConfigPreferences configPreferences;
	private CommandHandler commandHandler;
	private ChatManager chatManager;
	private UserManager userManager;
	private PermissionsManager permissionsManager;
	private GameItemManager gameItemManager;
	private LanguageManager languageManager;

	@Override
	public void onEnable() {
		initializeClasses();
		checkUpdate();

		getLogger().info("Initialization finished. Join our Discord server: https://discord.gg/rVkaGmyszE");
	}

	@Override
	public void onDisable() {
		saveAllUserStatistics();
		
		if (database != null) {
			database.shutdownConnPool();
		}

		for (Arena arena : ArenaRegistry.getArenas()) {
			arena.getScoreboardManager().stopAllScoreboards();

			for (Player player : arena.getPlayers()) {
				AttributeUtils.resetAttackCooldown(player);

				arena.teleportToEndLocation(player);
				arena.doBarAction(0, player);
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
	}
	
	private void initializeClasses() {
		setupFiles();

		configPreferences = new ConfigPreferences(this);
		chatManager = new ChatManager(this);
		languageManager = new LanguageManager(this);
		userManager = new UserManager(this);
		signManager = new SignManager(this);
		ArenaRegistry.registerArenas();
		signManager.loadSigns();
		rewardsFactory = new RewardsFactory(this);
		commandHandler = new CommandHandler(this);
		permissionsManager = new PermissionsManager(this);
		gameItemManager = new GameItemManager(this);

		if (configPreferences.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) bungeeManager = new BungeeManager(this);
		if (configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED)) database = new MysqlDatabase(this, "mysql");

		ScoreboardLib.setPluginInstance(this);
		ListenerAdapter.registerEvents(this);

		registerSoftDependenciesAndServices();

		if (configPreferences.getOption(ConfigPreferences.Option.NAME_TAGS_HIDDEN)) {
			getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> getServer().getOnlinePlayers().forEach(ArenaUtils::updateNameTagsVisibility), 60, 140);
		}
	}
	
	private void registerSoftDependenciesAndServices() {
		startPluginMetrics();

		if (chatManager.isPapiEnabled()) {
			new PlaceholderManager(this);
		}
	}
	
	private void startPluginMetrics() {
		Metrics metrics = new Metrics(this, 8118);

		metrics.addCustomChart(new SimplePie("locale_used", () -> languageManager.getPluginLocale().prefix));
		metrics.addCustomChart(new SimplePie("database_enabled", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.DATABASE_ENABLED))));
		metrics.addCustomChart(new SimplePie("bungeecord_hooked", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.BUNGEE_ENABLED))));
		metrics.addCustomChart(new SimplePie("update_notifier", () -> String.valueOf(configPreferences.getOption(ConfigPreferences.Option.UPDATE_NOTIFIER_ENABLED))));
	}
	
	private void checkUpdate() {
		if (!configPreferences.getOption(ConfigPreferences.Option.UPDATE_NOTIFIER_ENABLED)) return;

		UpdateChecker.init(this, 81185).requestUpdateCheck().whenComplete((result, exception) -> {
			if (result.requiresUpdate()) {
				getLogger().info("Found a new version available: v" + result.getNewestVersion());
				getLogger().info("Download it SpigotMC:");
				getLogger().info("https://spigotmc.org/resources/81185");
			}
		});
	}

	private void setupFiles() {
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

	@NotNull
	public ConfigPreferences getConfigPreferences() {
		return configPreferences;
	}

	@NotNull
	public MysqlDatabase getMysqlDatabase() {
		return database;
	}

	@NotNull
	public SignManager getSignManager() {
		return signManager;
	}

	@NotNull
	public ChatManager getChatManager() {
		return chatManager;
	}

	@NotNull
	public CommandHandler getCommandHandler() {
		return commandHandler;
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

	private void saveAllUserStatistics() {
		for (Player player : getServer().getOnlinePlayers()) {
			final User user = userManager.getUser(player);

			if (userManager.getDatabase() instanceof MysqlManager) {
				final StringBuilder builder = new StringBuilder(" SET ");
				final MysqlManager database = (MysqlManager) userManager.getDatabase();

				for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
					if (!stat.isPersistent()) continue;

					final int value = user.getStat(stat);

					if (builder.toString().equalsIgnoreCase(" SET ")) {
						builder.append(stat.getName()).append("'='").append(value);
					}

					builder.append(", ").append(stat.getName()).append("'='").append(value);
				}

				final String update = builder.toString();
				database.getDatabase().executeUpdate("UPDATE " + database.getTableName() + update + " WHERE UUID='" + user.getPlayer().getUniqueId() + "';");
				continue;
			}

			userManager.getDatabase().saveAllStatistic(user);
		}
	}
}