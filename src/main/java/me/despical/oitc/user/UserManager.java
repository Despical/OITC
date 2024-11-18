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

package me.despical.oitc.user;

import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.user.data.FlatFileStatistics;
import me.despical.oitc.user.data.MySQLStatistics;
import me.despical.oitc.user.data.AbstractDatabase;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class UserManager {

	private final Map<UUID, User> users;
	private final AbstractDatabase database;

	public UserManager(Main plugin) {
		this.users = new HashMap<>();
		this.database = plugin.getOption(ConfigPreferences.Option.DATABASE_ENABLED) ? new MySQLStatistics() : new FlatFileStatistics();

		plugin.getServer().getOnlinePlayers().forEach(this::addUser);
	}

	public User getUser(Player player) {
		User user = users.get(player.getUniqueId());

		if (user != null) {
			return user;
		}

		return this.addUser(player);
	}

	public Set<User> getUsers(Arena arena) {
		return arena.getPlayers().stream().map(this::getUser).collect(Collectors.toSet());
	}

	public Set<User> getUsers() {
		return Set.copyOf(users.values());
	}

	public void saveAllStatistic(User user) {
		database.saveAllStatistic(user);
	}

	public void loadStatistics(Player player) {
		database.loadStatistics(getUser(player));
	}

	public User addUser(Player player) {
		User user = new User(player);
		database.loadStatistics(user);

		users.put(player.getUniqueId(), user);
		return user;
	}

	public void removeUser(Player player) {
		users.remove(player.getUniqueId());
	}

	public AbstractDatabase getDatabase() {
		return database;
	}
}