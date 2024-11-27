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

package me.despical.oitc.handlers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.despical.commons.string.StringFormatUtils;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.despical.oitc.api.StatsStorage.StatisticType.*;

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

	@NotNull
	@Override
	public String getIdentifier() {
		return "oitc";
	}

	@NotNull
	@Override
	public String getAuthor() {
		return "Despical";
	}

	@NotNull
	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public String onPlaceholderRequest(Player player, @NotNull String id) {
		if (player == null) return null;

		User user = plugin.getUserManager().getUser(player);

		return switch (id.toLowerCase()) {
			case "online_players" ->
				Long.toString(plugin.getArenaRegistry().getArenas().stream().map(arena -> arena.getPlayers().size()).count());
			case "kills" -> KILLS.from(user);
			case "deaths" -> DEATHS.from(user);
			case "games_played" -> GAMES_PLAYED.from(user);
			case "highest_score" -> HIGHEST_SCORE.from(user);
			case "wins" -> WINS.from(user);
			case "loses" -> LOSES.from(user);
			case "local_kills" -> LOCAL_KILLS.from(user);
			case "local_deaths" -> LOCAL_DEATHS.from(user);
			case "local_kill_streak" -> LOCAL_KILL_STREAK.from(user);
			default -> handleArenaPlaceholderRequest(id);
		};
	}

	private String handleArenaPlaceholderRequest(String id) {
		String[] data = id.split(":");
		Arena arena = plugin.getArenaRegistry().getArena(data[0]);

		if (arena == null) return null;

		return switch (data[1].toLowerCase()) {
			case "id" -> arena.getId();
			case "timer" -> Integer.toString(arena.getTimer());
			case "timer_pretty" -> StringFormatUtils.formatIntoMMSS(arena.getTimer());
			case "players" -> Integer.toString(arena.getPlayers().size());
			case "players_left" -> Integer.toString(arena.getPlayersLeft().size());
			case "max_players" -> Integer.toString(arena.getMaximumPlayers());
			case "min_players" -> Integer.toString(arena.getMinimumPlayers());
			case "state" -> arena.getArenaState().name();
			case "state_pretty" -> arena.getArenaState().getFormattedName();
			case "map_name" -> arena.getMapName();
			default -> null;
		};
	}
}