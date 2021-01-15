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
import me.despical.commonsbox.configuration.ConfigUtils;
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
	private final Map<ArenaState, String> gameStateToString = new EnumMap<>(ArenaState.class);
	private final FileConfiguration config;
	private final String motd;

	public BungeeManager(Main plugin) {
		this.plugin = plugin;
		this.config = ConfigUtils.getConfig(plugin, "bungee");
		ChatManager chatManager = plugin.getChatManager();
		
		gameStateToString.put(ArenaState.WAITING_FOR_PLAYERS, chatManager.colorRawMessage(config.getString("MOTD.Game-States.Inactive", "Inactive")));
		gameStateToString.put(ArenaState.STARTING, chatManager.colorRawMessage(config.getString("MOTD.Game-States.Starting", "Starting")));
		gameStateToString.put(ArenaState.IN_GAME, chatManager.colorRawMessage(config.getString("MOTD.Game-States.In-Game", "In-Game")));
		gameStateToString.put(ArenaState.ENDING, chatManager.colorRawMessage(config.getString("MOTD.Game-States.Ending", "Ending")));
		gameStateToString.put(ArenaState.RESTARTING, chatManager.colorRawMessage(config.getString("MOTD.Game-States.Restarting", "Restarting")));
		motd = plugin.getChatManager().colorRawMessage(config.getString("MOTD.Message", "The actual game state of OITC is %state%"));

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

	private Arena getArena() {
		return ArenaRegistry.getArenas().get(ArenaRegistry.getBungeeArena());
	}

	private String getHubServerName() {
		return config.getString("Hub");
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onServerListPing(ServerListPingEvent event) {
		if (!config.getBoolean("MOTD.Manager")) {
			return;
		}

		if (ArenaRegistry.getArenas().isEmpty()) {
			return;
		}

		event.setMaxPlayers(getArena().getMaximumPlayers());
		event.setMotd(motd.replace("%state%", gameStateToString.get(getArena().getArenaState())));
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(final PlayerJoinEvent event) {
		event.setJoinMessage("");
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> ArenaManager.joinAttempt(event.getPlayer(), getArena()), 1L);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onQuit(PlayerQuitEvent event) {
		event.setQuitMessage("");

		if (ArenaRegistry.isInArena(event.getPlayer())) {
			ArenaManager.leaveAttempt(event.getPlayer(), getArena());
		}
	}
}