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

import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * @author Despical
 * <p>
 * Created at 23.06.2022
 */
public class CreateCommand extends SubCommand {

	public CreateCommand() {
		super ("create");

		setPermission("oitc.admin.create");
	}

	@Override
	public String getPossibleArguments() {
		return "<id>";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public void execute(CommandSender sender, String label, String[] args) {
		Player player = (Player) sender;

		if (args.length == 0) {
			player.sendMessage(chatManager.prefixedRawMessage("&cPlease enter an name to create an arena!"));
			return;
		}

		String arg = args[0];

		if (ArenaRegistry.isArena(arg)) {
			player.sendMessage(chatManager.prefixedRawMessage("&cArena with that ID already contains!"));
			player.sendMessage(chatManager.prefixedRawMessage("&cTo check existing arenas use: /oitc list"));
			return;
		}

		createArenaConfiguration(arg);

		player.sendMessage(chatManager.coloredRawMessage("&l--------------------------------------------"));
		MiscUtils.sendCenteredMessage(player, "&eInstance &a&l" + arg + " &ecreated!");
		player.sendMessage("");
		MiscUtils.sendCenteredMessage(player, "&aEdit this arena via /oitc edit &6" + arg + "&a!");
		player.sendMessage("");
		MiscUtils.sendCenteredMessage(player, "&6Don't know where to start? Check out our wiki:");
		MiscUtils.sendCenteredMessage(player, "&7https://www.github.com/Despical/OITC/wiki");
		player.sendMessage(chatManager.coloredRawMessage("&l--------------------------------------------"));
	}

	@Override
	public String getTutorial() {
		return "Creates a new arena with default configuration";
	}

	@Override
	public int getType() {
		return GENERIC;
	}

	@Override
	public int getSenderType() {
		return PLAYER;
	}

	private void createArenaConfiguration(String id) {
		String path = "instances." + id + ".";

		config.set(path + "ready", false);
		config.set(path + "mapName", id);
		config.set(path + "minimumPlayers", 2);
		config.set(path + "maximumPlayers", 10);
		config.set(path + "lobbyLocation", LocationSerializer.SERIALIZED_LOCATION);
		config.set(path + "endLocation", LocationSerializer.SERIALIZED_LOCATION);
		config.set(path + "playersSpawnPoints", new ArrayList<>());
		config.set(path + "signs", new ArrayList<>());

		ConfigUtils.saveConfig(plugin, config, "arenas");

		Arena arena = new Arena(id);
		arena.setMapName(id);
		arena.setLobbyLocation(LocationSerializer.DEFAULT_LOCATION);
		arena.setEndLocation(LocationSerializer.DEFAULT_LOCATION);

		ArenaRegistry.registerArena(arena);
	}
}