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

package me.despical.oitc.commands.player;

import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.commands.SubCommand;
import me.despical.oitc.user.User;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Despical
 * <p>
 * Created at 23.06.2022
 */
public class StatsCommand extends SubCommand {

	public StatsCommand() {
		super ("stats");
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
		Player player = args.length == 0 ? (Player) sender : plugin.getServer().getPlayer(args[0]);
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

	@Override
	public String getTutorial() {
		return null;
	}

	@Override
	public int getType() {
		return HIDDEN;
	}

	@Override
	public int getSenderType() {
		return PLAYER;
	}
}