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

package me.despical.oitc.api;

import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.sorter.SortUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.user.User;
import me.despical.oitc.user.data.MysqlManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author Despical
 * @since 1.0.0
 */
public class StatsStorage {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	public static Map<UUID, Integer> getStats(StatisticType stat) {
		if (plugin.getUserManager().getDatabase() instanceof MysqlManager) {
			MysqlManager mysqlManager = (MysqlManager) plugin.getUserManager().getDatabase();

			try (Connection connection = plugin.getMysqlDatabase().getConnection()) {
				Statement statement = connection.createStatement();
				ResultSet set = statement.executeQuery("SELECT UUID, " + stat.name + " FROM " + mysqlManager.getTable() + " ORDER BY " + stat.name);
				Map<UUID, Integer> column = new LinkedHashMap<>();
				
				while (set.next()) {
					column.put(UUID.fromString(set.getString("UUID")), set.getInt(stat.name));
				}

				return column;
			} catch (SQLException exception) {
				plugin.getLogger().log(Level.WARNING, "SQL Exception occurred! " + exception.getSQLState() + " (" + exception.getErrorCode() + ")");
				return null;
			}
		}

		FileConfiguration config = ConfigUtils.getConfig(plugin, "stats");
		Map<UUID, Integer> stats = config.getKeys(false).stream().collect(Collectors.toMap(UUID::fromString, string -> config.getInt(string + "." + stat.name), (a, b) -> b));

		return SortUtils.sortByValue(stats);
	}

	public static int getUserStats(Player player, StatisticType statisticType) {
		return plugin.getUserManager().getUser(player).getStat(statisticType);
	}

	public enum StatisticType {
		KILLS("kills"), DEATHS("deaths"), GAMES_PLAYED("gamesplayed"), HIGHEST_SCORE("highestscore"),
		LOSES("loses"), WINS("wins"), LOCAL_KILLS("local_kills", false), LOCAL_DEATHS("local_deaths", false),
		LOCAL_KILL_STREAK("local_kill_streak", false);

		final String name;
		final boolean persistent;

		StatisticType(String name) {
			this (name, true);
		}

		StatisticType(String name, boolean persistent) {
			this.name = name;
			this.persistent = persistent;
		}

		public String from(User user) {
			return Integer.toString(user.getStat(this));
		}

		public String getName() {
			return name;
		}

		public boolean isPersistent() {
			return persistent;
		}
	}
}