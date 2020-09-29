package me.despical.oitc.arena.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.scoreboard.ScoreboardLib;
import me.despical.commonsbox.scoreboard.common.EntryBuilder;
import me.despical.commonsbox.scoreboard.type.Entry;
import me.despical.commonsbox.scoreboard.type.Scoreboard;
import me.despical.commonsbox.scoreboard.type.ScoreboardHandler;
import me.despical.commonsbox.string.StringFormatUtils;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.user.User;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 03.07.2029
 */
public class ScoreboardManager {

	private final Main plugin = JavaPlugin.getPlugin(Main.class);
	private final List<Scoreboard> scoreboards = new ArrayList<>();
	private final Arena arena;
	private final FileConfiguration config = ConfigUtils.getConfig(plugin, "messages");
	
	public ScoreboardManager(Arena arena) {
		this.arena = arena;
	}

	/**
	 * Creates arena scoreboard for target user
	 *
	 * @param user user that represents game player
	 * @see User
	 */
	public void createScoreboard(User user) {
		Scoreboard scoreboard = ScoreboardLib.createScoreboard(user.getPlayer()).setHandler(new ScoreboardHandler() {

			@Override
			public String getTitle(Player player) {
				return plugin.getChatManager().colorMessage("Scoreboard.Title");
			}

			@Override
			public List<Entry> getEntries(Player player) {
				return formatScoreboard(user);
			}
		});

		scoreboard.activate();
		scoreboards.add(scoreboard);
	}

	/**
	 * Removes scoreboard of user
	 *
	 * @param user user that represents game player
	 * @see User
	 */
	public void removeScoreboard(User user) {
		for (Scoreboard board : scoreboards) {
			if (board.getHolder().equals(user.getPlayer())) {
				scoreboards.remove(board);
				board.deactivate();
				return;
			}
		}
	}

	/**
	 * Forces all scoreboards to deactivate.
	 */
	public void stopAllScoreboards() {
		for (Scoreboard board : scoreboards) {
			board.deactivate();
		}
		scoreboards.clear();
	}

	private List<Entry> formatScoreboard(User user) {
		EntryBuilder builder = new EntryBuilder();
		List<String> lines;

		if (arena.getArenaState() == ArenaState.IN_GAME) {
			lines = config.getStringList("Scoreboard.Content.Playing");
		} else {
			if (arena.getArenaState() == ArenaState.ENDING) {
				lines = config.getStringList("Scoreboard.Content.Playing");
			} else {
				lines = config.getStringList("Scoreboard.Content." + arena.getArenaState().getFormattedName());
			}
		}

		for (String line : lines) {
			if (!formatScoreboardLine(line, user).equals("%empty%")) {
				builder.next(formatScoreboardLine(line, user));
			}
		}

		return builder.build();
	}

	private String formatScoreboardLine(String line, User user) {
		String formattedLine = line;
		formattedLine = StringUtils.replace(formattedLine, "%time%", String.valueOf(arena.getTimer()));
		formattedLine = StringUtils.replace(formattedLine, "%formatted_time%",StringFormatUtils.formatIntoMMSS(arena.getTimer()));
		formattedLine = StringUtils.replace(formattedLine, "%mapname%", arena.getMapName());
		formattedLine = StringUtils.replace(formattedLine, "%players%", String.valueOf(arena.getPlayers().size()));
		formattedLine = StringUtils.replace(formattedLine, "%max_players%", String.valueOf(arena.getMaximumPlayers()));
		formattedLine = StringUtils.replace(formattedLine, "%min_players%", String.valueOf(arena.getMinimumPlayers()));
		formattedLine = StringUtils.replace(formattedLine, "%kills%", String.valueOf(StatsStorage.getUserStats(user.getPlayer(), StatsStorage.StatisticType.LOCAL_KILLS)));
		formattedLine = StringUtils.replace(formattedLine, "%deaths%", String.valueOf(StatsStorage.getUserStats(user.getPlayer(), StatsStorage.StatisticType.LOCAL_DEATHS)));
		formattedLine = StringUtils.replace(formattedLine, "%kill_streak%", String.valueOf(StatsStorage.getUserStats(user.getPlayer(), StatsStorage.StatisticType.LOCAL_KILL_STREAK)));

		for (int i = 0; i <= arena.getMaximumPlayers(); i++) {
			formattedLine = StringUtils.replace(formattedLine, "%top_player_" + (i + 1) + "%", arena.getPlayersLeft().size() > i ? formatTopPlayer(getTopPlayerName(i), i) : "%empty%");
		}

		formattedLine = plugin.getChatManager().colorRawMessage(formattedLine);

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			formattedLine = PlaceholderAPI.setPlaceholders(user.getPlayer(), formattedLine);
		}
		return formattedLine;
	}
	
	private Map<Player, Integer> getSortedLeaderboard(){
		Map<Player, Integer> statistics = new HashMap<>();

		for (Player player : arena.getPlayersLeft()) {
			statistics.put(player, StatsStorage.getUserStats(player, StatsStorage.StatisticType.LOCAL_KILLS));
		}

		return statistics.entrySet().stream().sorted(Map.Entry.<Player, Integer>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));	
	}
	
	public String getTopPlayerName(int rank) {
		List<String> names = new ArrayList<>();

		for(Map.Entry<Player, Integer> entry : getSortedLeaderboard().entrySet()) {
			names.add(entry.getKey().getName());
		}

		if(rank < getSortedLeaderboard().size()) {
			return names.get(rank);
		}

		return "";
	}

	public int getTopPlayerScore(int rank) {
		List<Integer> scores = new ArrayList<>();

		for(Map.Entry<Player, Integer> entry : getSortedLeaderboard().entrySet()) {
			scores.add(entry.getValue());
		}

		if(rank < getSortedLeaderboard().size()) {
			return scores.get(rank);
		}

		return 0;
	}
	
	public int getRank(Player player) {
		List<Player> ranks = new ArrayList<>();

		for(Map.Entry<Player, Integer> entry : getSortedLeaderboard().entrySet()) {
			ranks.add(entry.getKey());
		}

		for(int i = 0; i <= ranks.size(); i++) {
			if(ranks.get(i) == player) {
				return i + 1;
			}
		}

		return 0;
	}
	
	private String formatTopPlayer(String player, int rank) {
		String formatted = plugin.getChatManager().colorMessage("Scoreboard.Top-Player-Format");
		formatted = StringUtils.replace(formatted, "%player%", player);
		formatted = StringUtils.replace(formatted, "%score%", String.valueOf(getTopPlayerScore(rank)));
		return formatted;
	}
}