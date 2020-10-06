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

package me.despical.oitc.commands;

import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class TabCompletion implements TabCompleter {

	public CommandHandler commandHandler;
	
	public TabCompletion(CommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		List<String> completions = new ArrayList<>();
		List<String> commands = commandHandler.getSubCommands().stream().map(command -> command.getName().toLowerCase()).collect(Collectors.toList());

		if (args.length == 1) {
			StringUtil.copyPartialMatches(args[0], commands, completions);
		} 

		if (args.length == 2) {
			if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("list") ||
				args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("randomjoin") || args[0].equalsIgnoreCase("stop") ||
				args[0].equalsIgnoreCase("forcestart") || args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("arenas")) {
				return null;
			}

			if (args[0].equalsIgnoreCase("top")) {
				return Arrays.stream(StatsStorage.StatisticType.values()).filter(StatsStorage.StatisticType::isPersistent).map(statistic -> statistic.name().toLowerCase(Locale.ENGLISH)).collect(Collectors.toList());
			}

			List<String> arenas = ArenaRegistry.getArenas().stream().map(Arena::getId).collect(Collectors.toList());

			StringUtil.copyPartialMatches(args[1], arenas, completions);
			Collections.sort(arenas);
			return arenas;
		}

		Collections.sort(completions);
		return completions;
	}
}