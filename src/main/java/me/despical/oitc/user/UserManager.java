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

package me.despical.oitc.user;

import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.user.data.FileStats;
import me.despical.oitc.user.data.MysqlManager;
import me.despical.oitc.user.data.UserDatabase;
import me.despical.oitc.utils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class UserManager {

	private final UserDatabase database;
	private final List<User> users = new ArrayList<>();

	public UserManager(Main plugin) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
			database = new MysqlManager(plugin);

			Debugger.debug("MySQL Stats enabled");
		} else {
			database = new FileStats(plugin);

			Debugger.debug("File Stats enabled");
		}

		loadStatsForPlayersOnline();
	}

	private void loadStatsForPlayersOnline() {
		Bukkit.getServer().getOnlinePlayers().stream().map(this::getUser).forEach(this::loadStatistics);
	}

	public User getUser(Player player) {
		for (User user : users) {
			if (user.getPlayer().equals(player)) {
				return user;
			}
		}

		Debugger.debug("Registering new user {0} ({1})", player.getUniqueId(), player.getName());
		User user = new User(player);
		users.add(user);
		return user;
	}
	
	public List<User> getUsers(Arena arena) {
		return arena.getPlayers().stream().map(this::getUser).collect(Collectors.toList());
	}

	public void saveStatistic(User user, StatsStorage.StatisticType stat) {
		if (!stat.isPersistent()) {
			return;
		}

		database.saveStatistic(user, stat);
	}

	public void saveAllStatistic(User user) {
		database.saveAllStatistic(user);
	}

	public void loadStatistics(User user) {
		database.loadStatistics(user);
	}

	public void removeUser(User user) {
		users.remove(user);
	}

	public UserDatabase getDatabase() {
		return database;
	}
}