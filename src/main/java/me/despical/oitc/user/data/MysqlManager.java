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
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.user.User;
import org.jetbrains.annotations.NotNull;

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

	private final Main plugin;
	private final String table;
	private final MysqlDatabase database;

	public MysqlManager(Main plugin) {
		this.plugin = plugin;
		this.table = ConfigUtils.getConfig(plugin, "mysql").getString("table", "oitc_stats");
		this.database = plugin.getMysqlDatabase();

		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try (Connection connection = database.getConnection()) {
				Statement statement = connection.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + table + "` (\n"
					+ "  `UUID` char(36) NOT NULL PRIMARY KEY,\n"
					+ "  `name` varchar(32) NOT NULL,\n"
					+ "  `kills` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `deaths` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `highestscore` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `gamesplayed` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `wins` int(11) NOT NULL DEFAULT '0',\n"
					+ "  `loses` int(11) NOT NULL DEFAULT '0');");
			} catch (SQLException exception) {
				exception.printStackTrace();

				plugin.getLogger().severe("Couldn't create statistics table on MySQL database!");
			}
		});
	}

	@Override
	public void saveStatistic(@NotNull User user, StatsStorage.StatisticType statisticType) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> database.executeUpdate(String.format("UPDATE %s SET %s=%d WHERE UUID='%s';", table, statisticType.getName(), user.getStat(statisticType), user.getUniqueId().toString())));
	}

	@Override
	public void saveAllStatistic(@NotNull User user) {
		final StringBuilder builder = new StringBuilder(" SET ");

		for (final StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
			if (!stat.isPersistent()) continue;

			final String name = stat.getName();
			final int value = user.getStat(stat);

			if (builder.toString().equalsIgnoreCase(" SET ")) {
				builder.append(name).append("=").append(value);
			}

			builder.append(", ").append(name).append("=").append(value);
		}

		final String update = builder.toString();

		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> database.executeUpdate(String.format("UPDATE %s%s WHERE UUID='%s';", table, update, user.getUniqueId().toString())));
	}

	@Override
	public void loadStatistics(@NotNull User user) {
		final String uuid = user.getUniqueId().toString();

		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try (final Connection connection = database.getConnection()) {
				final Statement statement = connection.createStatement();
				final ResultSet result = statement.executeQuery(String.format("SELECT * from %s WHERE UUID='%s';", table, uuid));

				if (result.next()) {
					for (final StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
						if (!stat.isPersistent()) continue;

						user.setStat(stat, result.getInt(stat.getName()));
					}
				} else {
					statement.executeUpdate(String.format("INSERT INTO %s (UUID,name) VALUES ('%s','%s');", table, uuid, user.getName()));

					for (final StatsStorage.StatisticType stat : StatsStorage.StatisticType.values()) {
						if (!stat.isPersistent()) continue;

						user.setStat(stat, 0);
					}
				}
			} catch (SQLException exception) {
				exception.printStackTrace();
			}
		});
	}

	@NotNull
	public String getTable() {
		return table;
	}

	@NotNull
	public MysqlDatabase getDatabase() {
		return database;
	}
}