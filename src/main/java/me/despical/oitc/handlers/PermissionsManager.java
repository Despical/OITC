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

import me.despical.oitc.Main;
import org.bukkit.entity.Player;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class PermissionsManager {

	private final boolean suppressPerms;
	private final String joinFullPerm, joinPerm;

	public PermissionsManager(Main plugin) {
		this.suppressPerms = plugin.getConfig().getBoolean("Basic-Permissions.Suppress-Permissions");
		this.joinPerm = plugin.getConfig().getString("Basic-Permissions.Join-Permission");
		this.joinFullPerm = plugin.getConfig().getString("Basic-Permissions.Full-Games-Permission");
	}

	public boolean hasJoinPerm(Player player, String arena) {
		if (suppressPerms) return true;

		return player.hasPermission(joinPerm.replace("<arena>", "*")) || !player.hasPermission(joinPerm.replace("<arena>", arena));
	}

	public String getJoinFullPerm() {
		return joinFullPerm;
	}

	public String getJoinPerm() {
		return joinPerm;
	}
}