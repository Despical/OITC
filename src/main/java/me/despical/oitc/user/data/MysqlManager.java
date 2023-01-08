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

package me.despical.oitc.user.data;

import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.database.MysqlDatabase;
import me.despical.commons.util.LogUtils;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.user.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class MysqlManager implements UserDatabase {

	private final MysqlDatabase database;
	private final String tableName;

	public MysqlManager() {
		this.database = plugin.getMysqlDatabase();
		this.tableName = ConfigUtils.getConfig(plugin, "mysql").getString("table", "playerstats");

		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try (Connection connection = database.getConnection()) {
				Statement statement = connection.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName + "` (\n"
					+ "  `UUID` char(36) NOT NULL PRIMARY KEY,\n"
					+ "  `name` varchar(32) NOT NULL,\n"
					+ "  `kills` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `deaths` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `highestscore` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `gamesplayed` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `wins` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `loses` int(11) NOT NULL DEFAULT '0'\n" + ");");
			} catch (SQLException exception) {
				exception.printStackTrace();
				LogUtils.sendConsoleMessage("&cCouldn't save user statistics to MySQL database!");
				LogUtils.sendConsoleMessage("&cCheck your configuration or disable MySQL option in config.yml");
			}
		});
	}

	@Override
	public void saveStatistic(User user, StatsStorage.StatisticType stat) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			final String query = "UPDATE " + tableName + " SET " + stat.getName() + "=" + user.getStat(stat) + " WHERE UUID='" + user.getUniqueId().toString() + "';";

			database.executeUpdate(query);
			LogUtils.log("Executed MySQL: " + query);
		});
	}

	@Override
	public void saveAllStatistic(User user) {
		final StringBuilder builder = new StringBuilder(" SET ");

		for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
			if (!stat.isPersistent()) continue;

			final String name = stat.getName();
			final int val = user.getStat(stat);

			if (builder.toString().equalsIgnoreCase(" SET ")) {
				builder.append(name).append("=").append(val);
			}

			builder.append(", ").append(name).append("=").append(val);
		}

		final String update = builder.toString();
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> database.executeUpdate("UPDATE " + tableName + update + " WHERE UUID='" + user.getUniqueId().toString() + "';"));
	}

	@Override
	public void loadStatistics(User user) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			final String uuid = user.getUniqueId().toString(), playerName = user.getPlayer().getName();

			try (Connection connection = database.getConnection()) {
				final Statement statement = connection.createStatement();
				final ResultSet result = statement.executeQuery("SELECT * from " + tableName + " WHERE UUID='" + uuid + "';");

				if (result.next()) {
					LogUtils.log("MySQL Stats | Player {0} already exist. Getting Stats...", playerName);

					for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
						if (!stat.isPersistent()) continue;

						user.setStat(stat, result.getInt(stat.getName()));
					}
				} else {
					LogUtils.log("MySQL Stats | Player {0} does not exist. Creating new one...", playerName);
					statement.executeUpdate("INSERT INTO " + tableName + " (UUID,name) VALUES ('" + uuid + "','" + playerName + "');");

					for (StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
						if (!stat.isPersistent()) continue;

						user.setStat(stat, 0);
					}
				}
			} catch (SQLException exception) {
				exception.printStackTrace();
			}
		});
	}

	public String getTableName() {
		return tableName;
	}

	public MysqlDatabase getDatabase() {
		return database;
	}
}