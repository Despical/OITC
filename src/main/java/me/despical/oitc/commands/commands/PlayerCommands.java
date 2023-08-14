package me.despical.oitc.commands.commands;

import me.despical.commandframework.Command;
import me.despical.commandframework.CommandArguments;
import me.despical.commons.string.StringMatcher;
import me.despical.commons.string.StringUtils;
import me.despical.commons.util.Collections;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.commands.AbstractCommand;
import me.despical.oitc.user.User;
import me.despical.oitc.user.data.MysqlManager;
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

import static me.despical.commandframework.Command.SenderType.PLAYER;

/**
 * @author Despical
 * <p>
 * Created at 3.07.2023
 */
public class PlayerCommands extends AbstractCommand {

	public PlayerCommands(Main plugin) {
		super(plugin);

		plugin.getCommandFramework().setMatchFunction(arguments -> {
			if (arguments.isArgumentsEmpty()) return false;

			String label = arguments.getLabel(), arg = arguments.getArgument(0);

			List<StringMatcher.Match> matches = StringMatcher.match(arg, plugin.getCommandFramework().getCommands().stream().filter(cmd -> !cmd.name().equals("oitc")).map(cmd -> cmd.name().replace(label + ".", "")).collect(Collectors.toList()));

			if (!matches.isEmpty()) {
				arguments.sendMessage(chatManager.message("commands.did-you-mean").replace("%command%", label + " " + matches.get(0).getMatch()));
				return true;
			}

			return false;
		});
	}

	@Command(
		name = "oitc.join",
		senderType = PLAYER
	)
	public void joinCommand(CommandArguments arguments) {
		Player player = arguments.getSender();

		if (arguments.isArgumentsEmpty()) {
			player.sendMessage(chatManager.prefixedMessage("commands.type_arena_name"));
			return;
		}

		Arena arena = plugin.getArenaRegistry().getArena(arguments.getArgument(0));

		if (arena != null) {
			ArenaManager.joinAttempt(player, arena);
			return;
		}

		player.sendMessage(chatManager.prefixedMessage("commands.no_arena_like_that"));
	}

	@Command(
		name = "oitc.randomjoin",
		senderType = PLAYER
	)
	public void randomJoinCommand(CommandArguments arguments) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			return;
		}

		List<Arena> arenas = plugin.getArenaRegistry().getArenas().stream().filter(arena -> Collections.contains(arena.getArenaState(), ArenaState.WAITING_FOR_PLAYERS, ArenaState.STARTING) && arena.getPlayers().size() < arena.getMaximumPlayers()).collect(Collectors.toList());
		Player player = arguments.getSender();

		if (!arenas.isEmpty()) {
			boolean noPlayers = arenas.stream().anyMatch(arena -> arena.getPlayers().isEmpty());
			Arena arena = arenas.get(noPlayers ? ThreadLocalRandom.current().nextInt(arenas.size()) : 0);

			ArenaManager.joinAttempt(player, arena);
			return;
		}

		player.sendMessage(chatManager.prefixedMessage("commands.no_free_arenas"));
	}

	@Command(
		name = "oitc.leave",
		senderType = PLAYER
	)
	public void leaveCommand(CommandArguments arguments) {
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_LEAVE_COMMAND)) {
			return;
		}

		Player player = arguments.getSender();
		Arena arena = plugin.getArenaRegistry().getArena(player);

		if (arena == null) {
			player.sendMessage(chatManager.prefixedMessage("commands.not_playing", player));
			return;
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			plugin.getBungeeManager().connectToHub(player);
			return;
		}

		ArenaManager.leaveAttempt(player, arena);
	}

	@Command(
		name = "oitc.stats",
		senderType = PLAYER
	)
	public void statsCommand(CommandArguments arguments) {
		final Player sender = arguments.getSender(), player = !arguments.isArgumentsEmpty() ? plugin.getServer().getPlayer(arguments.getArgument(0)) : sender;
		String path = "commands.stats_command.";

		if (player == null) {
			sender.sendMessage(chatManager.prefixedMessage(path + "player_not_found"));
			return;
		}

		User user = plugin.getUserManager().getUser(player);

		if (player.equals(sender)) {
			player.sendMessage(chatManager.message(path + "header", player));
		} else {
			sender.sendMessage(chatManager.message(path + "header_other", player).replace("%player%", player.getName()));
		}

		sender.sendMessage(chatManager.message(path + "kills", player) + user.getStat(StatsStorage.StatisticType.KILLS));
		sender.sendMessage(chatManager.message(path + "deaths", player) + user.getStat(StatsStorage.StatisticType.DEATHS));
		sender.sendMessage(chatManager.message(path + "wins", player) + user.getStat(StatsStorage.StatisticType.WINS));
		sender.sendMessage(chatManager.message(path + "loses", player) + user.getStat(StatsStorage.StatisticType.LOSES));
		sender.sendMessage(chatManager.message(path + "games_played", player) + user.getStat(StatsStorage.StatisticType.GAMES_PLAYED));
		sender.sendMessage(chatManager.message(path + "highest_score", player) + user.getStat(StatsStorage.StatisticType.HIGHEST_SCORE));
		sender.sendMessage(chatManager.message(path + "footer", player));
	}

	@Command(
		name = "oitc.top"
	)
	public void leaderboardCommand(CommandArguments arguments) {
		String path = "commands.statistics.";

		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage(path + "type_name"));
			return;
		}

		try {
			StatsStorage.StatisticType statisticType = StatsStorage.StatisticType.valueOf(arguments.getArgument(0).toUpperCase(java.util.Locale.ENGLISH));

			if (!statisticType.isPersistent()) {
				arguments.sendMessage(chatManager.prefixedMessage(path + "invalid_name"));
				return;
			}

			printLeaderboard(arguments.getSender(), statisticType);
		} catch (IllegalArgumentException e) {
			arguments.sendMessage(chatManager.prefixedMessage(path + "invalid_name"));
		}
	}

	private void printLeaderboard(CommandSender sender, StatsStorage.StatisticType statisticType) {
		Map<UUID, Integer> stats = StatsStorage.getStats(statisticType);
		sender.sendMessage(plugin.getChatManager().message("commands.statistics.header"));

		String statistic = StringUtils.capitalize(statisticType.name().toLowerCase(java.util.Locale.ENGLISH).replace("_", " "));

		for (int i = 0; i < 10; i++) {
			try {
				UUID current = (UUID) stats.keySet().toArray()[stats.keySet().toArray().length - 1];
				sender.sendMessage(formatMessage(statistic, plugin.getServer().getOfflinePlayer(current).getName(), i + 1, stats.get(current)));
				stats.remove(current);
			} catch (IndexOutOfBoundsException ex) {
				sender.sendMessage(formatMessage(statistic, "Empty", i + 1, 0));
			} catch (NullPointerException ex) {
				UUID current = (UUID) stats.keySet().toArray()[stats.keySet().toArray().length - 1];

				if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DATABASE_ENABLED)) {
					try (Connection connection = plugin.getMysqlDatabase().getConnection()) {
						Statement statement = connection.createStatement();
						ResultSet set = statement.executeQuery("SELECT name FROM " + ((MysqlManager) plugin.getUserManager().getDatabase()).getTableName() + " WHERE UUID='" + current.toString() + "'");

						if (set.next()) {
							sender.sendMessage(formatMessage(statistic, set.getString(1), i + 1, stats.get(current)));
							continue;
						}
					} catch (SQLException ignored) {}
				}

				sender.sendMessage(formatMessage(statistic, "Unknown Player", i + 1, stats.get(current)));
			}
		}
	}

	private String formatMessage(String statisticName, String playerName, int position, int value) {
		String message = chatManager.message("commands.statistics.format");

		message = message.replace("%position%", Integer.toString(position));
		message = message.replace("%name%", playerName);
		message = message.replace("%value%", Integer.toString(value));
		message = message.replace("%statistic%", statisticName);
		return message;
	}
}