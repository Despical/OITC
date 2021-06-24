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

package me.despical.oitc.arena.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.commons.scoreboard.ScoreboardLib;
import me.despical.commons.scoreboard.common.EntryBuilder;
import me.despical.commons.scoreboard.type.Entry;
import me.despical.commons.scoreboard.type.Scoreboard;
import me.despical.commons.scoreboard.type.ScoreboardHandler;
import me.despical.commons.string.StringFormatUtils;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaState;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 03.07.2029
 */
public class ScoreboardManager {

	private final Main plugin;
	private final Set<Scoreboard> scoreboards;
	private final Arena arena;

	public ScoreboardManager(Main plugin, Arena arena) {
		this.plugin = plugin;
		this.arena = arena;
		this.scoreboards = new HashSet<>();
	}

	public void createScoreboard(Player player) {
		Scoreboard scoreboard = ScoreboardLib.createScoreboard(player).setHandler(new ScoreboardHandler() {

			@Override
			public String getTitle(Player player) {
				return plugin.getChatManager().message("Scoreboard.Title");
			}

			@Override
			public List<Entry> getEntries(Player player) {
				return formatScoreboard(player);
			}
		});

		scoreboard.activate();
		scoreboards.add(scoreboard);
	}

	public void removeScoreboard(Player player) {
		for (Scoreboard board : scoreboards) {
			if (board.getHolder().equals(player)) {
				scoreboards.remove(board);
				board.deactivate();
				return;
			}
		}
	}

	public void stopAllScoreboards() {
		scoreboards.forEach(Scoreboard::deactivate);
		scoreboards.clear();
	}

	private List<Entry> formatScoreboard(Player player) {
		EntryBuilder builder = new EntryBuilder();
		List<String> lines;

		if (arena.getArenaState() == ArenaState.IN_GAME || arena.getArenaState() == ArenaState.ENDING) {
			lines = plugin.getChatManager().getStringList("Scoreboard.Content.Playing");
		} else {
			lines = plugin.getChatManager().getStringList("Scoreboard.Content." + arena.getArenaState().getFormattedName());
		}

		for (String line : lines) {
			if (!formatScoreboardLine(line, player).equals("%empty%")) {
				builder.next(formatScoreboardLine(line, player));
			}
		}

		return builder.build();
	}

	private String formatScoreboardLine(String line, Player player) {
		String formattedLine = line;

		formattedLine = StringUtils.replace(formattedLine, "%time%", Integer.toString(arena.getTimer()));
		formattedLine = StringUtils.replace(formattedLine, "%formatted_time%", StringFormatUtils.formatIntoMMSS(arena.getTimer()));
		formattedLine = StringUtils.replace(formattedLine, "%map_name%", arena.getMapName());
		formattedLine = StringUtils.replace(formattedLine, "%players%", Integer.toString(arena.getPlayers().size()));
		formattedLine = StringUtils.replace(formattedLine, "%max_players%", Integer.toString(arena.getMaximumPlayers()));
		formattedLine = StringUtils.replace(formattedLine, "%min_players%", Integer.toString(arena.getMinimumPlayers()));
		formattedLine = StringUtils.replace(formattedLine, "%kills%", Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.LOCAL_KILLS)));
		formattedLine = StringUtils.replace(formattedLine, "%deaths%", Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.LOCAL_DEATHS)));
		formattedLine = StringUtils.replace(formattedLine, "%kill_streak%", Integer.toString(StatsStorage.getUserStats(player, StatsStorage.StatisticType.LOCAL_KILL_STREAK)));

		for (int i = 0; i <= arena.getMaximumPlayers(); i++) {
			formattedLine = StringUtils.replace(formattedLine, "%top_player_" + (i + 1) + "%", arena.getPlayersLeft().size() > i ? formatTopPlayer(getTopPlayerName(i), i) : "%empty%");
		}

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			formattedLine = PlaceholderAPI.setPlaceholders(player, formattedLine);
		}

		return plugin.getChatManager().coloredRawMessage(formattedLine);
	}

	private Map<Player, Integer> getSortedLeaderboard() {
		Map<Player, Integer> statistics = new HashMap<>();

		for (Player player : arena.getPlayersLeft()) {
			statistics.put(player, StatsStorage.getUserStats(player, StatsStorage.StatisticType.LOCAL_KILLS));
		}

		return statistics.entrySet().stream().sorted(Map.Entry.<Player, Integer>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public String getTopPlayerName(int rank) {
		List<String> names = getSortedLeaderboard().keySet().stream().map(Player::getName).collect(Collectors.toList());

		return rank < getSortedLeaderboard().size() ? names.get(rank) : "";
	}

	public int getTopPlayerScore(int rank) {
		Map<Player, Integer> leaderboard = getSortedLeaderboard();
		List<Integer> scores = new ArrayList<>(leaderboard.values());

		return rank < leaderboard.size() ? scores.get(rank) : 0;
	}

	public int getRank(Player player) {
		List<Player> ranks = new ArrayList<>(getSortedLeaderboard().keySet());

		for (int i = 0; i <= ranks.size(); i++) {
			if (ranks.get(i).equals(player)) {
				return i + 1;
			}
		}

		return 0;
	}

	private String formatTopPlayer(String player, int rank) {
		String formatted = plugin.getChatManager().message("Scoreboard.Top-Player-Format");

		formatted = StringUtils.replace(formatted, "%player%", player);
		formatted = StringUtils.replace(formatted, "%score%", Integer.toString(getTopPlayerScore(rank)));
		return formatted;
	}
}