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

package me.despical.oitc.arena;

import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.commons.util.LogUtils;
import me.despical.oitc.Main;
import me.despical.oitc.handlers.ChatManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ArenaRegistry {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);
	private static final List<Arena> arenas = new ArrayList<>();
	private static int bungeeArena = -1;

	public static boolean isInArena(Player player) {
		return arenas.stream().anyMatch(arena -> arena.getPlayers().contains(player));
	}

	public static Arena getArena(Player p) {
		return arenas.stream().filter(arena -> arena.getPlayers().stream().anyMatch(player -> player.equals(p))).findFirst().orElse(null);
	}

	public static Arena getArena(String id) {
		return arenas.stream().filter(arena -> arena.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
	}

	public static boolean isArena(String id) {
		return arenas.stream().anyMatch(arena -> arena.getId().equalsIgnoreCase(id));
	}

	public static void registerArena(Arena arena) {
		LogUtils.log("Registering new game instance {0}", arena.getId());
		arenas.add(arena);
	}

	public static void unregisterArena(Arena arena) {
		LogUtils.log("Unregistering game instance {0}", arena.getId());
		arenas.remove(arena);
	}

	public static void registerArenas() {
		LogUtils.log("Initial arenas registration");
		long start = System.currentTimeMillis();

		arenas.clear();

		FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
		ChatManager chatManager = plugin.getChatManager();

		if (!config.contains("instances")) {
			LogUtils.sendConsoleMessage(chatManager.message("Validator.No-Instances-Created"));
			return;
		}

		ConfigurationSection section = config.getConfigurationSection("instances");

		if (section == null) {
			LogUtils.sendConsoleMessage(chatManager.message("Validator.No-Instances-Created"));
			return;
		}

		for (String id : section.getKeys(false)) {
			String s = "instances." + id + ".";

			if (s.contains("default")) {
				continue;
			}

			Arena arena = new Arena(id);
			arena.setReady(true);
			arena.setMinimumPlayers(config.getInt(s + "minimumplayers", 2));
			arena.setMaximumPlayers(config.getInt(s + "maximumplayers", 10));
			arena.setMapName(config.getString(s + "mapname", "undefined"));
			arena.setPlayerSpawnPoints(config.getStringList(s + "playerspawnpoints").stream().map(LocationSerializer::fromString).collect(Collectors.toList()));
			arena.setLobbyLocation(LocationSerializer.fromString(config.getString(s + "lobbylocation")));
			arena.setEndLocation(LocationSerializer.fromString(config.getString(s + "Endlocation")));

			if (!config.getBoolean(s + "isdone")) {
				LogUtils.sendConsoleMessage(chatManager.message("Validator.Invalid-Arena-Configuration").replace("%arena%", id).replace("%error%", "NOT VALIDATED"));
				arena.setReady(false);
				registerArena(arena);
				continue;
			}

			arena.setArenaState(ArenaState.WAITING_FOR_PLAYERS);
			registerArena(arena);
			arena.start();
			LogUtils.sendConsoleMessage(chatManager.message("Validator.Instance-Started").replace("%arena%", id));
		}

		LogUtils.log("Arenas registration completed, took {0} ms", System.currentTimeMillis() - start);
	}

	public static List<Arena> getArenas() {
		return arenas;
	}

	public static void shuffleBungeeArena() {
		bungeeArena = ThreadLocalRandom.current().nextInt(arenas.size());
	}

	public static Arena getBungeeArena() {
		return arenas.get(bungeeArena == -1 ? bungeeArena = ThreadLocalRandom.current().nextInt(arenas.size()) : bungeeArena);
	}
}