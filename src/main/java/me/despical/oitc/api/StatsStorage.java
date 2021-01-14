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

package me.despical.oitc.api;

import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.sorter.SortUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.user.data.MysqlManager;
import me.despical.oitc.utils.Debugger;
import me.despical.oitc.utils.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Class for accessing users statistics.
 */
public class StatsStorage {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	/**
	 * Get all UUID's sorted ascending by Statistic Type
	 *
	 * @param stat Statistic type to get (kills, deaths etc.)
	 * @return Map of UUID keys and Integer values sorted in ascending order of
	 *         requested statistic type
	 */
	@NotNull
	@Contract("null -> fail")
	public static Map<UUID, Integer> getStats(StatisticType stat) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
			try (Connection connection = plugin.getMysqlDatabase().getConnection()) {
				Statement statement = connection.createStatement();
				ResultSet set = statement.executeQuery("SELECT UUID, " + stat.getName() + " FROM " + ((MysqlManager) plugin.getUserManager().getDatabase()).getTableName() + " ORDER BY " + stat.getName());
				Map<UUID, Integer> column = new LinkedHashMap<>();
				
				while (set.next()) {
					column.put(UUID.fromString(set.getString("UUID")), set.getInt(stat.getName()));
				}

				return column;
			} catch (SQLException e) {
				plugin.getLogger().log(Level.WARNING, "SQL Exception occurred! " + e.getSQLState() + " (" + e.getErrorCode() + ")");
				MessageUtils.errorOccurred();
				Debugger.sendConsoleMessage("Cannot get contents from MySQL database!");
				Debugger.sendConsoleMessage("Check configuration of mysql.yml file or disable mysql option in config.yml");
				return Collections.emptyMap();
			}
		}

		FileConfiguration config = ConfigUtils.getConfig(plugin, "stats");
		Map<UUID, Integer> stats = new TreeMap<>();

		for (String string : config.getKeys(false)) {
			if (string.equals("data-version")) {
				continue;
			}

			stats.put(UUID.fromString(string), config.getInt(string + "." + stat.getName()));
		}

		return SortUtils.sortByValue(stats);
	}

	/**
	 * Get user statistic based on StatisticType
	 *
	 * @param player Online player to get data from
	 * @param statisticType Statistic type to get (kills, deaths etc.)
	 * @return int of statistic
	 * @see StatisticType
	 */
	public static int getUserStats(Player player, StatisticType statisticType) {
		return plugin.getUserManager().getUser(player).getStat(statisticType);
	}

	/**
	 * Available statistics to get.
	 */
	public enum StatisticType {
		KILLS("kills", true), DEATHS("deaths", true), GAMES_PLAYED("gamesplayed", true), HIGHEST_SCORE("highestscore", true),
		LOSES("loses", true), WINS("wins", true), LOCAL_KILLS("local_kills", false), LOCAL_DEATHS("local_deaths", false),
		LOCAL_KILL_STREAK("local_kill_streak", false);

		private final String name;
		private final boolean persistent;

		StatisticType(String name, boolean persistent) {
			this.name = name;
			this.persistent = persistent;
		}

		public String getName() {
			return name;
		}

		public boolean isPersistent() {
			return persistent;
		}
	}
}