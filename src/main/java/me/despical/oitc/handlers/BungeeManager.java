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

package me.despical.oitc.handlers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.events.ListenerAdapter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
public class BungeeManager extends ListenerAdapter {

	private final String motd, hubName;
	private final boolean motdEnabled, shutdownWhenGameEnds, connectToHub;
	private final Map<ArenaState, String> gameStates;

	public BungeeManager(Main plugin) {
		super(plugin);
		this.gameStates = new EnumMap<>(ArenaState.class);

		final FileConfiguration config = ConfigUtils.getConfig(plugin, "bungee");

		this.motd = plugin.getChatManager().coloredRawMessage(config.getString("MOTD.Message"));
		this.hubName = config.getString("Hub");
		this.motdEnabled = config.getBoolean("MOTD.Enabled");
		this.shutdownWhenGameEnds = config.getBoolean("Shutdown-When-Game-Ends");
		this.connectToHub = config.getBoolean("Connect-To-Hub");

		for (final ArenaState state : ArenaState.values()) {
			gameStates.put(state, plugin.getChatManager().coloredRawMessage(config.getString("MOTD.Game-States." + state.getFormattedName())));
		}

		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
	}

	public void connectToHub(final Player player) {
		if (!connectToHub) return;

		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(hubName);

		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	}

	public boolean isShutdownWhenGameEnds() {
		return shutdownWhenGameEnds;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onServerListPing(final ServerListPingEvent event) {
		if (!motdEnabled) return;

		final ArenaRegistry arenaRegistry = plugin.getArenaRegistry();

		if (arenaRegistry.getArenas().isEmpty()) return;

		final Arena bungeeArena = arenaRegistry.getBungeeArena();

		event.setMaxPlayers(bungeeArena.getMaximumPlayers());
		event.setMotd(motd.replace("%state%", gameStates.get(bungeeArena.getArenaState())));
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoin(PlayerJoinEvent event) {
		final ArenaRegistry arenaRegistry = plugin.getArenaRegistry();

		if (arenaRegistry.getArenas().isEmpty()) return;

		event.setJoinMessage("");
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> ArenaManager.joinAttempt(event.getPlayer(), arenaRegistry.getBungeeArena()), 1L);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onQuit(PlayerQuitEvent event) {
		final ArenaRegistry arenaRegistry = plugin.getArenaRegistry();

		if (arenaRegistry.getArenas().isEmpty()) return;

		event.setQuitMessage("");

		final Player player = event.getPlayer();

		if (arenaRegistry.isInArena(player)) {
			ArenaManager.leaveAttempt(player, arenaRegistry.getBungeeArena());
		}
	}
}