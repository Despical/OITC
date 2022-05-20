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

package me.despical.oitc.handlers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class BungeeManager implements Listener {

	private final Main plugin;
	private final Map<ArenaState, String> gameStates;
	private final FileConfiguration config;

	private final String motd, hubName;

	public BungeeManager(Main plugin) {
		this.plugin = plugin;
		this.gameStates = new EnumMap<>(ArenaState.class);
		this.config = ConfigUtils.getConfig(plugin, "bungee");
		this.motd = plugin.getChatManager().message("MOTD.Message");
		this.hubName = config.getString("Hub");

		for (ArenaState state : ArenaState.values()) {
			gameStates.put(state, plugin.getChatManager().message("MOTD.Game-States." + state.getFormattedName()));
		}

		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void connectToHub(Player player) {
		if (!config.getBoolean("Connect-To-Hub", true)) {
			return;
		}

		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(getHubServerName());

		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	}

	public void shutdownIfEnabled() {
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) return;

		if (config.getBoolean("Shutdown-When-Game-Ends")) {
			plugin.getServer().shutdown();
		}
	}

	private String getHubServerName() {
		return hubName;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onServerListPing(ServerListPingEvent event) {
		if (!config.getBoolean("MOTD.Manager")) {
			return;
		}

		if (ArenaRegistry.getArenas().isEmpty()) {
			return;
		}

		Arena bungeeArena = ArenaRegistry.getBungeeArena(); // Do not cache in constructor

		event.setMaxPlayers(bungeeArena.getMaximumPlayers());
		event.setMotd(motd.replace("%state%", gameStates.get(bungeeArena.getArenaState())));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoin(PlayerJoinEvent event) {
		if (ArenaRegistry.getArenas().isEmpty()) {
			return;
		}

		event.setJoinMessage("");
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> ArenaManager.joinAttempt(event.getPlayer(), ArenaRegistry.getBungeeArena()), 1L);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onQuit(PlayerQuitEvent event) {
		if (ArenaRegistry.getArenas().isEmpty()) {
			return;
		}

		event.setQuitMessage("");

		if (ArenaRegistry.isInArena(event.getPlayer())) {
			ArenaManager.leaveAttempt(event.getPlayer(), ArenaRegistry.getBungeeArena());
		}
	}
}