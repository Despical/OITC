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

package me.despical.oitc.arena;

import me.despical.oitc.Main;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public enum ArenaState {

	WAITING_FOR_PLAYERS ("Waiting"), STARTING ("Starting"), IN_GAME("Playing"),
	ENDING ("Ending"), RESTARTING ("Restarting"), INACTIVE ("Inactive");

	String formattedName, defaultName;

	ArenaState(String name) {
		final Main plugin = JavaPlugin.getPlugin(Main.class);

		this.defaultName = name;
		this.formattedName = plugin.getChatManager().message("formatted_arena_states." + name);
	}

	public String getDefaultName() {
		return defaultName;
	}

	public String getFormattedName() {
		return formattedName;
	}
}