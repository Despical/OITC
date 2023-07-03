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

package me.despical.oitc.commands;

import me.despical.commandframework.CommandArguments;
import me.despical.commandframework.Completer;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class TabCompleter extends AbstractCommand {

	public TabCompleter(Main plugin) {
		super(plugin);
	}

	@Completer(
		name = "oitc"
	)
	public List<String> onTabComplete(CommandArguments arguments) {
		final List<String> completions = new ArrayList<>(), commands = plugin.getCommandFramework().getCommands().stream().map(cmd -> cmd.name().replace(arguments.getLabel() + '.', "")).collect(Collectors.toList());
		final String args[] = arguments.getArguments(), arg = args[0];

		commands.remove("oitc");

		if (args.length == 1) {
			StringUtil.copyPartialMatches(arg, arguments.hasPermission("oitc.admin") || arguments.getSender().isOp() ? commands : Arrays.asList("top", "stats", "join", "leave", "randomjoin"), completions);
		}

		if (args.length == 2) {
			if (Arrays.asList("create", "list", "randomjoin", "leave").contains(arg)) return null;

			if (arg.equalsIgnoreCase("top")) {
				return Arrays.asList("kills", "deaths", "games_played", "highest_score", "loses", "wins");
			}

			if (arg.equalsIgnoreCase("stats")) {
				return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
			}

			final List<String> arenas = ArenaRegistry.getArenas().stream().map(Arena::getId).collect(Collectors.toList());

			StringUtil.copyPartialMatches(args[1], arenas, completions);
			arenas.sort(null);
			return arenas;
		}

		completions.sort(null);
		return completions;
	}
}