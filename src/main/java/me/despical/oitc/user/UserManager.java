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

import me.despical.commons.util.LogUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.user.data.FileStats;
import me.despical.oitc.user.data.MysqlManager;
import me.despical.oitc.user.data.UserDatabase;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class UserManager {

	private final Set<User> users;
	private final UserDatabase database;

	public UserManager(Main plugin) {
		this.users = new HashSet<>();
		this.database = plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DATABASE_ENABLED) ? new MysqlManager() : new FileStats();

		plugin.getServer().getOnlinePlayers().forEach(this::getUser);
	}

	public User getUser(Player player) {
		final UUID uuid = player.getUniqueId();

		for (User user : users) {
			if (user.getUniqueId().equals(uuid)) {
				return user;
			}
		}

		LogUtils.log("Registering new user {0} ({1})", uuid, player.getName());

		final User user = new User(player);
		users.add(user);

		database.loadStatistics(user);
		return user;
	}
	
	public Set<User> getUsers(Arena arena) {
		return arena.getPlayers().stream().map(this::getUser).collect(Collectors.toSet());
	}

	public void saveAllStatistic(User user) {
		database.saveAllStatistic(user);
	}

	public void loadStatistics(User user) {
		database.loadStatistics(user);
	}

	public void removeUser(Player player) {
		users.remove(getUser(player));
	}

	public UserDatabase getDatabase() {
		return database;
	}
}