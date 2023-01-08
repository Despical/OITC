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

package me.despical.oitc.commands.admin;

import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 23.06.2022
 */
public class ListCommand extends SubCommand {

	public ListCommand() {
		super ("list");

		setPermission("oitc.admin.list");
	}

	@Override
	public String getPossibleArguments() {
		return null;
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public void execute(CommandSender sender, String label, String[] args) {
		List<Arena> arenas = ArenaRegistry.getArenas();

		if (arenas.isEmpty()) {
			sender.sendMessage(chatManager.prefixedMessage("commands.admin_commands.list_command.no_arenas_created"));
			return;
		}

		String arenaNames = arenas.stream().map(Arena::getId).collect(Collectors.joining(", "));
		sender.sendMessage(chatManager.prefixedMessage("commands.admin_commands.list_command.format").replace("%list%", arenaNames));
	}

	@Override
	public String getTutorial() {
		return "Shows all of the existing arenas";
	}

	@Override
	public int getType() {
		return GENERIC;
	}

	@Override
	public int getSenderType() {
		return BOTH;
	}
}