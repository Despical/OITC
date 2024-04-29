/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2024 Despical
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
import me.despical.oitc.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ArenaRegistry {

	@NotNull
	private final Main plugin;

	@NotNull
	private final Set<Arena> arenas;

	private int bungeeArena = -1;

	public ArenaRegistry(Main plugin) {
		this.plugin = plugin;
		this.arenas = new HashSet<>();

		this.registerArenas();
	}

	public void registerArena(final Arena arena) {
		this.arenas.add(arena);
	}

	public void unregisterArena(final Arena arena) {
		this.arenas.remove(arena);
	}

	@NotNull
	public Set<Arena> getArenas() {
		return new HashSet<>(arenas);
	}

	@Nullable
	public Arena getArena(final String id) {
		if (id == null) return null;

		return this.arenas.stream().filter(arena -> arena.getId().equals(id)).findFirst().orElse(null);
	}

	@Nullable
	public Arena getArena(final Player player) {
		if (player == null) return null;

		return this.arenas.stream().filter(arena -> arena.getPlayers().contains(player)).findFirst().orElse(null);
	}

	public boolean isArena(final String arenaId) {
		return arenaId != null && getArena(arenaId) != null;
	}

	public boolean isInArena(final Player player) {
		return this.getArena(player) != null;
	}

	private void registerArenas() {
		this.arenas.clear();

		FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
		ConfigurationSection section = config.getConfigurationSection("instances");

		if (section == null) {
			plugin.getLogger().warning("Couldn't find 'instances' section in arena.yml, delete the file to regenerate it!");
			return;
		}

		for (String id : section.getKeys(false)) {
			if (id.equals("default")) continue;

			String path = "instances." + id + ".";
			Arena arena = new Arena(id);

			this.registerArena(arena);

			arena.setReady(config.getBoolean(path + "ready"));
			arena.setMinimumPlayers(config.getInt(path + "minimumPlayers", 2));
			arena.setMaximumPlayers(config.getInt(path + "maximumPlayers", 10));
			arena.setMapName(config.getString(path + "mapName", "undefined"));
			arena.setPlayerSpawnPoints(config.getStringList(path + "playersSpawnPoints").stream().map(LocationSerializer::fromString).collect(Collectors.toList()));
			arena.setLobbyLocation(LocationSerializer.fromString(config.getString(path + "lobbyLocation")));
			arena.setEndLocation(LocationSerializer.fromString(config.getString(path + "endLocation")));


			if (!arena.isReady()) {
				plugin.getLogger().log(Level.WARNING, "Setup of arena ''{0}'' is not finished yet!", id);
				continue;
			}

			arena.start();
		}
	}

	// Bungee methods
	public void shuffleBungeeArena() {
		bungeeArena = ThreadLocalRandom.current().nextInt(arenas.size());
	}

	public Arena getBungeeArena() {
		return new ArrayList<>(arenas).get(bungeeArena == -1 ? bungeeArena = ThreadLocalRandom.current().nextInt(arenas.size()) : bungeeArena);
	}
}