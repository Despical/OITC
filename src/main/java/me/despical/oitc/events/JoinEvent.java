/*
 * OITC - Reach 25 points to win!
 * Copyright (C) 2020 Despical
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.oitc.events;

import me.despical.commonsbox.serializer.InventorySerializer;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.handlers.PermissionsManager;
import me.despical.oitc.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class JoinEvent implements Listener {

	private final Main plugin;

	public JoinEvent(Main plugin) {
		this.plugin = plugin;

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent e) {
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED) && !plugin.getServer().hasWhitelist() || e.getResult() != PlayerLoginEvent.Result.KICK_WHITELIST) {
			return;
		}

		if (e.getPlayer().hasPermission(PermissionsManager.getJoinFullGames())) {
			e.setResult(PlayerLoginEvent.Result.ALLOWED);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		plugin.getUserManager().loadStatistics(plugin.getUserManager().getUser(event.getPlayer()));

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			ArenaRegistry.getArenas().get(ArenaRegistry.getBungeeArena()).teleportToLobby(event.getPlayer());
			return;
		}

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			if (!ArenaRegistry.isInArena(player)) {
				continue;
			}

			player.hidePlayer(plugin, event.getPlayer());
			event.getPlayer().hidePlayer(plugin, player);
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, event.getPlayer());
		}
	}
	
	@EventHandler
	public void onJoinCheckVersion(final PlayerJoinEvent event) {
		if (!plugin.getConfig().getBoolean("Update-Notifier.Enabled", true) || !event.getPlayer().hasPermission("oitc.updatenotify")) {
			return;
		}

		Bukkit.getScheduler().runTaskLater(plugin, () -> UpdateChecker.init(plugin, 81185).requestUpdateCheck().whenComplete((result, exception) -> {
			if (!result.requiresUpdate()) {
				return;
			}

			if (result.getNewestVersion().contains("b")) {
				event.getPlayer().sendMessage(plugin.getChatManager().colorRawMessage("&3[OITC] &bFound a beta update: v" + result.getNewestVersion() + " Download"));
			} else {
				event.getPlayer().sendMessage(plugin.getChatManager().colorRawMessage("&3[OITC] &bFound an update: v" + result.getNewestVersion() + " Download:"));
			}

			event.getPlayer().sendMessage(plugin.getChatManager().colorRawMessage("&3>> &bhttps://www.spigotmc.org/resources/one-in-the-chamber-1-12-1-16-3.81185/"));
		}), 25);
	}
}