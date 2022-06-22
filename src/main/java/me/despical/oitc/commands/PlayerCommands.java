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

package me.despical.oitc.commands;

import me.despical.commandframework.Command;
import me.despical.commandframework.CommandArguments;
import me.despical.commons.string.StringMatcher;
import me.despical.commons.util.Collections;
import me.despical.commons.util.LogUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.handlers.ChatManager;
import me.despical.oitc.user.User;
import me.despical.oitc.user.data.MysqlManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 18.05.2021
 */
public class PlayerCommands {

	private final Main plugin;
	private final ChatManager chatManager;

	public PlayerCommands(Main plugin) {
		this.plugin = plugin;
		this.chatManager = plugin.getChatManager();

		plugin.getCommandFramework().registerCommands(this);
		plugin.getCommandFramework().setAnyMatch(arguments -> {
			if (arguments.isArgumentsEmpty()) return;

			String label = arguments.getLabel();
			List<StringMatcher.Match> matches = StringMatcher.match(arguments.getArgument(0), plugin.getCommandFramework().getCommands().stream().map(cmd -> cmd.name().replace(label + '.', "")).collect(Collectors.toList()));

			if (!matches.isEmpty()) {
				arguments.sendMessage(chatManager.message("Commands.Did-You-Mean").replace("%command%", label + " " + matches.get(0).getMatch()));
			}
		});
	}

	@Command(
		name = "oitc.join",
		desc = "Attemps player to join specified arena",
		usage = "/oitc join <arena>",
		senderType = Command.SenderType.PLAYER
	)
	public void joinCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.Type-Arena-Name"));
			return;
		}

		Arena arena = ArenaRegistry.getArena(arguments.getArgument(0));

		if (arena != null) {
			ArenaManager.joinAttempt(arguments.getSender(), arena);
			return;
		}

		arguments.sendMessage(chatManager.prefixedMessage("Commands.No-Arena-Like-That"));
	}

	@Command(
		name = "oitc.randomjoin",
		desc = "Attemps player to join random arena",
		usage = "/oitc randomjoin",
		senderType = Command.SenderType.PLAYER
	)
	public void randomJoinCommand(CommandArguments arguments) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			return;
		}

		List<Arena> arenas = ArenaRegistry.getArenas().stream().filter(arena -> Collections.contains(arena.getArenaState(), ArenaState.WAITING_FOR_PLAYERS, ArenaState.STARTING)
			&& arena.getPlayers().size() < arena.getMaximumPlayers()).collect(Collectors.toList());

		if (!arenas.isEmpty()) {
			Arena arena = arenas.get(ThreadLocalRandom.current().nextInt(arenas.size()));
			ArenaManager.joinAttempt(arguments.getSender(), arena);
			return;
		}

		arguments.sendMessage(chatManager.prefixedMessage("Commands.No-Free-Arenas"));
	}

	@Command(
		name = "oitc.leave",
		desc = "Attemps player to leave arena that player in",
		usage = "/oitc leave",
		senderType = Command.SenderType.PLAYER
	)
	public void leaveCommand(CommandArguments arguments) {
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_LEAVE_COMMAND)) {
			Player player = arguments.getSender();
			Arena arena = ArenaRegistry.getArena(player);

			if (arena == null) {
				player.sendMessage(chatManager.prefixedMessage("Commands.Not-Playing", player));
				return;
			}

			player.sendMessage(chatManager.prefixedMessage("Commands.Teleported-To-The-Lobby", player));

			if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
				plugin.getBungeeManager().connectToHub(player);
				LogUtils.log("{0} was teleported to the Hub server", player.getName());
				return;
			}

			ArenaManager.leaveAttempt(player, arena);
			LogUtils.log("{0} has left the arena {1}! Teleported to end location.", player.getName(), arena.getId());
		}
	}

	@Command(
		name = "oitc.stats",
		desc = "Shows player's or specified player's statistics",
		usage = "/oitc stats [<player>]",
		senderType = Command.SenderType.PLAYER
	)
	public void statsCommand(CommandArguments arguments) {
		Player sender = arguments.getSender(), player = arguments.isArgumentsEmpty() ? sender : plugin.getServer().getPlayerExact(arguments.getArgument(0));
		String path = "Commands.Stats-Command.";

		if (player == null) {
			sender.sendMessage(chatManager.prefixedMessage(path + "Player-Not-Found"));
			return;
		}

		User user = plugin.getUserManager().getUser(player);

		if (player.equals(sender)) {
			player.sendMessage(chatManager.message(path + "Header", player));
		} else {
			player.sendMessage(chatManager.message("Commands.Stats-Command.Header-Other", player).replace("%player%", player.getName()));
		}

		sender.sendMessage(chatManager.message(path + "Kills", player) + user.getStat(StatsStorage.StatisticType.KILLS));
		sender.sendMessage(chatManager.message(path + "Deaths", player) + user.getStat(StatsStorage.StatisticType.DEATHS));
		sender.sendMessage(chatManager.message(path + "Wins", player) + user.getStat(StatsStorage.StatisticType.WINS));
		sender.sendMessage(chatManager.message(path + "Loses", player) + user.getStat(StatsStorage.StatisticType.LOSES));
		sender.sendMessage(chatManager.message(path + "Games-Played", player) + user.getStat(StatsStorage.StatisticType.GAMES_PLAYED));
		sender.sendMessage(chatManager.message(path + "Highest-Score", player) + user.getStat(StatsStorage.StatisticType.HIGHEST_SCORE));
		sender.sendMessage(chatManager.message(path + "Footer", player));
	}

	@Command(
		name = "oitc.top",
		desc = "Shows top 10 players in specified statistic.",
		usage = "/oitc top <statistic>",
		senderType = Command.SenderType.PLAYER
	)
	public void leaderBoardCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.Statistics.Type-Name"));
			return;
		}

		try {
			StatsStorage.StatisticType statisticType = StatsStorage.StatisticType.valueOf(arguments.getArgument(0).toUpperCase(java.util.Locale.ENGLISH));

			if (!statisticType.isPersistent()) {
				arguments.sendMessage(chatManager.prefixedMessage("Commands.Statistics.Invalid-Name"));
				return;
			}

			printLeaderboard(arguments.getSender(), statisticType);
		} catch (IllegalArgumentException e) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.Statistics.Invalid-Name"));
		}
	}

	private void printLeaderboard(CommandSender sender, StatsStorage.StatisticType statisticType) {
		Map<UUID, Integer> stats = StatsStorage.getStats(statisticType);
		sender.sendMessage(chatManager.message("Commands.Statistics.Header"));
		String statistic = StringUtils.capitalize(statisticType.name().toLowerCase(java.util.Locale.ENGLISH).replace("_", " "));

		Object[] array = stats.keySet().toArray();
		UUID current = (UUID) array[array.length - 1];

		for (int i = 0; i < 10; i++) {
			try {
				sender.sendMessage(formatMessage(statistic, plugin.getServer().getOfflinePlayer(current).getName(), i + 1, stats.remove(current)));
			} catch (IndexOutOfBoundsException ex) {
				sender.sendMessage(formatMessage(statistic, "Empty", i + 1, 0));
			} catch (NullPointerException ex) {
				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
					try (Connection connection = plugin.getMysqlDatabase().getConnection()) {
						Statement statement = connection.createStatement();
						ResultSet set = statement.executeQuery("SELECT name FROM " + ((MysqlManager) plugin.getUserManager().getDatabase()).getTableName() + " WHERE UUID='" + current.toString() + "'");

						if (set.next()) {
							sender.sendMessage(formatMessage(statistic, set.getString(1), i + 1, stats.get(current)));
							continue;
						}
					} catch (SQLException ignored) {
						// Ignore exception
					}
				}

				sender.sendMessage(formatMessage(statistic, "Unknown Player", i + 1, stats.get(current)));
			}
		}
	}

	private String formatMessage(String statisticName, String playerName, int position, int value) {
		String message = chatManager.message("Commands.Statistics.Format");

		message = StringUtils.replace(message, "%position%", Integer.toString(position));
		message = StringUtils.replace(message, "%name%", playerName);
		message = StringUtils.replace(message, "%value%", Integer.toString(value));
		message = StringUtils.replace(message, "%statistic%", statisticName);
		return message;
	}
}