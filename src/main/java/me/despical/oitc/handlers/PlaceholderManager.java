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

package me.despical.oitc.handlers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.user.User;
import org.bukkit.entity.Player;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class PlaceholderManager extends PlaceholderExpansion {

	private final Main plugin;

	public PlaceholderManager(Main plugin) {
		this.plugin = plugin;
		this.register();
	}

	public boolean persist() {
		return true;
	}

	public String getIdentifier() {
		return "oitc";
	}

	public String getAuthor() {
		return "Despical";
	}

	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	public String onPlaceholderRequest(Player player, String id) {
		if (player == null) {
			return null;
		}

		final User user = plugin.getUserManager().getUser(player);

		switch (id.toLowerCase()) {
			case "kills":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.KILLS));
			case "deaths":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.DEATHS));
			case "games_played":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.GAMES_PLAYED));
			case "highest_score":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.HIGHEST_SCORE));
			case "wins":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.WINS));
			case "loses":
				return Integer.toString(user.getStat(StatsStorage.StatisticType.LOSES));
			default:
				return handleArenaPlaceholderRequest(id);
		}
	}

	private String handleArenaPlaceholderRequest(String id) {
		final String[] data = id.split(":");
		final Arena arena = ArenaRegistry.getArena(data[0]);

		if (arena == null) {
			return null;
		}

		switch (data[1].toLowerCase()) {
			case "players":
				return Integer.toString(arena.getPlayers().size());
			case "players_left":
				return Integer.toString(arena.getPlayersLeft().size());
			case "max_players":
				return Integer.toString(arena.getMaximumPlayers());
			case "min_players":
				return Integer.toString(arena.getMinimumPlayers());
			case "state":
				return arena.getArenaState().name();
			case "state_pretty":
				return arena.getArenaState().getFormattedName();
			case "map_name":
				return arena.getMapName();
			default:
				return null;
		}
	}
}