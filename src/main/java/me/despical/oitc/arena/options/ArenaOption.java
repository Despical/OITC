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

package me.despical.oitc.arena.options;

import me.despical.oitc.Main;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public enum ArenaOption {

	TIMER(45),

	MINIMUM_PLAYERS(2),

	MAXIMUM_PLAYERS(10),

	LOBBY_WAITING_TIME("Time-Settings.Lobby-Waiting-Time", 45),

	LOBBY_STARTING_TIME("Time-Settings.Lobby-Starting-Time", 16),

	LOBBY_ENDING_TIME("Time-Settings.Ending-Time", 6),

	GAMEPLAY_TIME("Time-Settings.Default-Gameplay-Time", 600);

	private final int defaultValue;

	ArenaOption(int defaultValue) {
		this.defaultValue = defaultValue;
	}

	ArenaOption(String path, int defaultValue) {
		final Main plugin = JavaPlugin.getPlugin(Main.class);

		this.defaultValue = Math.max(0, plugin.getConfig().getInt(path, defaultValue));
	}

	public int value() {
		return defaultValue;
	}
}