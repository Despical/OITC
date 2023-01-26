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

package me.despical.oitc.handlers.setup;

import me.despical.commons.serializer.LocationSerializer;
import me.despical.oitc.arena.Arena;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SetupUtilities {

	private final FileConfiguration config;
	private final Arena arena;

	SetupUtilities(FileConfiguration config, Arena arena) {
		this.config = config;
		this.arena = arena;
	}

	public String isOptionDone(String path) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		return config.isSet(path) ? "&a&l✔ Completed &7(value: &8" + config.getString(path) + "&7)" : "&c&l✘ Not Completed";
	}

	public String isOptionDoneList(String path, int minimum) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		if (config.isSet(path)) {
			int size = config.getStringList(path).size();

			return size < minimum ? "&c&l✘ Not Completed &c| &c&lPlease add more spawns" : "&a&l✔ Completed &7(value: &8" + size + "&7)";
		}

		return "&c&l✘ Not Completed";
	}

	public String isOptionDoneBool(String path) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		return config.isSet(path) ? LocationSerializer.isDefaultLocation(config.getString(path)) ? "&c&l✘ Not Completed" : "&a&l✔ Completed" : "&c&l✘ Not Completed";
	}

	public int getMinimumValueHigherThanZero(String path) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		return Math.max(1, config.getInt(path));
	}
}