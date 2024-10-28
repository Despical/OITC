package me.despical.oitc.commands;

import me.despical.commandframework.CommandArguments;
import me.despical.commandframework.annotations.Command;
import me.despical.commons.string.StringUtils;
import me.despical.commons.util.Collections;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.user.User;
import me.despical.oitc.user.data.MySQLStatistics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static me.despical.oitc.api.StatsStorage.StatisticType.*;

/**
 * @author Despical
 * <p>
 * Created at 3.07.2023
 */
public class PlayerCommands extends AbstractCommand {

	public PlayerCommands(Main plugin) {
		super(plugin);
	}

	@Command(
		name = "oitc.join",
		usage = "/oitc join <arena>",
		senderType = Command.SenderType.PLAYER
	)
	public void joinCommand(Player player, CommandArguments arguments) {
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
		usage = "/oitc randomjoin",
		senderType = Command.SenderType.PLAYER
	)
	public void randomJoinCommand(CommandArguments arguments) {
		if (plugin.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
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
		usage = "/oitc leave",
		senderType = Command.SenderType.PLAYER
	)
	public void leaveCommand(CommandArguments arguments) {
		if (plugin.getOption(ConfigPreferences.Option.DISABLE_LEAVE_COMMAND)) {
			return;
		}

		Player player = arguments.getSender();
		Arena arena = plugin.getArenaRegistry().getArena(player);

		if (arena == null) {
			player.sendMessage(chatManager.prefixedMessage("commands.not_playing", player));
			return;
		}

		if (plugin.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			plugin.getBungeeManager().connectToHub(player);
			return;
		}

		ArenaManager.leaveAttempt(player, arena);
	}

	@Command(
		name = "oitc.stats",
		usage = "/oitc stats <player>",
		senderType = Command.SenderType.PLAYER
	)
	public void statsCommand(CommandArguments arguments) {
		final Player sender = arguments.getSender();
		final User user = plugin.getUserManager().getUser(sender);

		if (arguments.isArgumentsEmpty()) {
			chatManager.getStringList("commands.stats_command.messages").stream().map(message -> formatStats(message, true, user)).forEach(sender::sendMessage);
			return;
		}

		Optional<Player> targetOptional = arguments.getPlayer(0);

		if (!targetOptional.isPresent()) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.admin-commands.player-not-found"));
			return;
		}

		targetOptional.ifPresent(player -> {
			final User targetUser = plugin.getUserManager().getUser(player);
			final boolean self = sender.equals(player);

			chatManager.getStringList("commands.stats_command.messages").stream().map(message -> formatStats(message, self, targetUser)).forEach(sender::sendMessage);
		});
	}

	private String formatStats(String message, boolean self, User user) {
		message = message.replace("%header%", chatManager.message("commands.stats_command." + (self ? "header" : "header_other")));
		message = message.replace("%kills%", KILLS.from(user));
		message = message.replace("%deaths%", DEATHS.from(user));
		message = message.replace("%wins%", WINS.from(user));
		message = message.replace("%loses%", LOSES.from(user));
		message = message.replace("%games_played%", GAMES_PLAYED.from(user));
		message = message.replace("%highest_score%", HIGHEST_SCORE.from(user));
		return chatManager.coloredRawMessage(message);
	}

	@Command(
		name = "oitc.top",
		usage = "/oitc top <statistic type>",
		senderType = Command.SenderType.PLAYER
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
		sender.sendMessage(chatManager.message("commands.statistics.header"));

		String statistic = StringUtils.capitalize(statisticType.name().toLowerCase(java.util.Locale.ENGLISH).replace("_", " "));
		String emptyEntry = chatManager.message("commands.leaderboard_command.empty_entry");
		String unknownEntry = chatManager.message("commands.leaderboard_command.unknown_entry");

		for (int i = 0; i < 10; i++) {
			try {
				UUID current = (UUID) stats.keySet().toArray()[stats.keySet().toArray().length - 1];
				sender.sendMessage(formatMessage(statistic, plugin.getServer().getOfflinePlayer(current).getName(), i + 1, stats.get(current)));
				stats.remove(current);
			} catch (IndexOutOfBoundsException ex) {
				sender.sendMessage(formatMessage(statistic, emptyEntry, i + 1, 0));
			} catch (NullPointerException ex) {
				UUID current = (UUID) stats.keySet().toArray()[stats.keySet().toArray().length - 1];

				if (plugin.getUserManager().getDatabase() instanceof MySQLStatistics mySQLManager) {
					try (Connection connection = mySQLManager.getDatabase().getConnection()) {
						Statement statement = connection.createStatement();
						ResultSet set = statement.executeQuery("SELECT name FROM " + ((MySQLStatistics) plugin.getUserManager().getDatabase()).getTableName() + " WHERE UUID='" + current.toString() + "'");

						if (set.next()) {
							sender.sendMessage(formatMessage(statistic, set.getString(1), i + 1, stats.get(current)));
							continue;
						}
					} catch (SQLException ignored) {}
				}

				sender.sendMessage(formatMessage(statistic, unknownEntry, i + 1, stats.get(current)));
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