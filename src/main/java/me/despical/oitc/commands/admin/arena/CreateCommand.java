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

package me.despical.oitc.commands.admin.arena;

import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.miscellaneous.MiscUtils;
import me.despical.commonsbox.serializer.LocationSerializer;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.commands.SubCommand;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class CreateCommand extends SubCommand {

	public CreateCommand() {
		super("create");

		setPermission("admin.create");
	}

	@Override
	public String getPossibleArguments() {
		return "<ID>";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("Commands.Type-Arena-Name"));
			return;
		}
		
		Player player = (Player) sender;

		for (Arena arena : ArenaRegistry.getArenas()) {
			if (arena.getId().equalsIgnoreCase(args[0])) {
				player.sendMessage(plugin.getChatManager().getPrefix() + ChatColor.RED + "Arena with that ID already exists!");
				player.sendMessage(plugin.getChatManager().getPrefix() + ChatColor.RED + "Usage: /oitc create <ID>");
				return;
			}
		}

		if (ConfigUtils.getConfig(plugin, "arenas").contains("instances." + args[0])) {
			player.sendMessage(plugin.getChatManager().getPrefix() + ChatColor.RED + "Instance/Arena already exists! Use another ID or delete it first!");
		} else {
			createInstanceInConfig(args[0]);
			player.sendMessage(ChatColor.BOLD + "----------------------------------------");
			MiscUtils.sendCenteredMessage(player, ChatColor.YELLOW + "      Instance " + args[0] + " created!");
			player.sendMessage("");
			MiscUtils.sendCenteredMessage(player, ChatColor.GREEN + "Edit this arena via " + ChatColor.GOLD + "/oitc edit "  + args[0] + ChatColor.GREEN + "!");
			player.sendMessage(ChatColor.BOLD + "----------------------------------------");
		}
	}
	
	private void createInstanceInConfig(String id) {
		String path = "instances." + id + ".";
		FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");

		config.set(path + "lobbylocation", LocationSerializer.locationToString(Bukkit.getServer().getWorlds().get(0).getSpawnLocation()));
		config.set(path + "Endlocation", LocationSerializer.locationToString(Bukkit.getServer().getWorlds().get(0).getSpawnLocation()));
		config.set(path + "playerspawnpoints", new ArrayList<>());
		config.set(path + "minimumplayers", 2);
		config.set(path + "maximumplayers", 10);
		config.set(path + "mapname", id);
		config.set(path + "signs", new ArrayList<>());
		config.set(path + "isdone", false);

		ConfigUtils.saveConfig(plugin, config, "arenas");

		Arena arena = new Arena(id);
		List<Location> playerSpawnPoints = new ArrayList<>();

		for (String loc : config.getStringList(path + "playerspawnpoints")) {
			playerSpawnPoints.add(LocationSerializer.locationFromString(loc));
		}

		arena.setPlayerSpawnPoints(playerSpawnPoints);
		arena.setMapName(config.getString(path + "mapname"));
		arena.setLobbyLocation(LocationSerializer.locationFromString(config.getString(path + "lobbylocation")));
		arena.setEndLocation(LocationSerializer.locationFromString(config.getString(path + "Endlocation")));
		arena.setReady(false);

		ArenaRegistry.registerArena(arena);
	}

	@Override
	public List<String> getTutorial() {
		return Collections.singletonList("Create new arena");
	}

	@Override
	public CommandType getType() {
		return CommandType.GENERIC;
	}

	@Override
	public SenderType getSenderType() {
		return SenderType.PLAYER;
	}
}