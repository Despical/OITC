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

package me.despical.oitc.arena;

import me.despical.commons.compat.VersionResolver;
import me.despical.commons.miscellaneous.PlayerUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ArenaUtils {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	public static void hidePlayer(Player p, Arena arena) {
		if (isLegacy) return;

		for (Player player : arena.getPlayers()) {
			PlayerUtils.hidePlayer(player , p, plugin);
		}
	}

	public static void showPlayer(Player p, Arena arena) {
		if (isLegacy) return;

		for (Player player : arena.getPlayers()) {
			PlayerUtils.showPlayer(player, p, plugin);
		}
	}

	public static void hidePlayersOutsideTheGame(Player player, Arena arena) {
		if (isLegacy) return;

		for (Player players : plugin.getServer().getOnlinePlayers()) {
			if (arena.getPlayers().contains(players)) {
				continue;
			}

			PlayerUtils.hidePlayer(player, players, plugin);
			PlayerUtils.hidePlayer(players, player, plugin);
		}
	}

	public static void showPlayersOutsideTheGame(Player player, Arena arena) {
		if (isLegacy) return;

		for (Player players : plugin.getServer().getOnlinePlayers()) {
			if (arena.getPlayers().contains(players)) {
				continue;
			}

			player.showPlayer(plugin, players);
			players.showPlayer(plugin, player);
		}
	}

	private final static boolean isLegacy = VersionResolver.isCurrentEqual(VersionResolver.ServerVersion.v1_8_R3);
	private final static boolean isNameTagsSupported = !isLegacy && plugin.getConfigPreferences().getOption(ConfigPreferences.Option.NAME_TAGS_HIDDEN);

	public static boolean isLegacy() {
		return isLegacy;
	}

	public static void updateNameTagsVisibility(Player p) {
		if (!isNameTagsSupported) return;

		for (Player players : plugin.getServer().getOnlinePlayers()) {
			Arena arena = ArenaRegistry.getArena(players);

			if (arena == null) {
				continue;
			}

			Scoreboard scoreboard = players.getScoreboard();

			if (scoreboard == plugin.getServer().getScoreboardManager().getMainScoreboard()) {
				scoreboard = plugin.getServer().getScoreboardManager().getNewScoreboard();
			}

			Team team = scoreboard.getTeam("OITCHide");

			if (team == null) {
				team = scoreboard.registerNewTeam("OITCHide");
			}

			team.setCanSeeFriendlyInvisibles(false);
			team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

			if (arena.getArenaState() == ArenaState.IN_GAME) {
				team.addEntry(p.getName());
			} else {
				team.removeEntry(p.getName());
			}

			players.setScoreboard(scoreboard);
		}
	}
}