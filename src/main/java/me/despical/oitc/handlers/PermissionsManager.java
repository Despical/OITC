/*
 * OITC - Reach 25 points to win!
 * Copyright (C) 2020 Despical
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.oitc.handlers;

import me.despical.oitc.Main;
import me.despical.oitc.utils.Debugger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class PermissionsManager {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);
	private static String joinFullPerm = "oitc.fullgames";
	private static String joinPerm = "oitc.join.<arena>";

	public static void init() {
		setupPermissions();
	}

	public static String getJoinFullGames() {
		return joinFullPerm;
	}

	private static void setJoinFullGames(String joinFullGames) {
		PermissionsManager.joinFullPerm = joinFullGames;
	}

	public static String getJoinPerm() {
		return joinPerm;
	}

	private static void setJoinPerm(String joinPerm) {
		PermissionsManager.joinPerm = joinPerm;
	}

	private static void setupPermissions() {
		PermissionsManager.setJoinFullGames(plugin.getConfig().getString("Basic-Permissions.Full-Games-Permission", "oitc.fullgames"));
		PermissionsManager.setJoinPerm(plugin.getConfig().getString("Basic-Permissions.Join-Permission", "oitc.join.<arena>"));
		Debugger.debug("Basic permissions registered");
	}
}