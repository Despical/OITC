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

package me.despical.oitc.events;

import me.despical.commons.miscellaneous.PlayerUtils;
import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.util.UpdateChecker;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaRegistry;
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
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED) || e.getResult() != PlayerLoginEvent.Result.KICK_WHITELIST) {
			return;
		}

		if (e.getPlayer().hasPermission(plugin.getPermissionsManager().joinFullPerm)) {
			e.setResult(PlayerLoginEvent.Result.ALLOWED);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player eventPlayer = event.getPlayer();
		plugin.getUserManager().loadStatistics(plugin.getUserManager().getUser(eventPlayer));

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			ArenaRegistry.getBungeeArena().teleportToLobby(eventPlayer);
			return;
		}

		for (Player player : plugin.getServer().getOnlinePlayers()) {
			if (!ArenaRegistry.isInArena(player)) {
				continue;
			}

			PlayerUtils.hidePlayer(player, eventPlayer, plugin);
			PlayerUtils.hidePlayer(eventPlayer, player, plugin);
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, eventPlayer);
		}
	}
	
	@EventHandler
	public void onJoinCheckVersion(PlayerJoinEvent event) {
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.UPDATE_NOTIFIER_ENABLED) || !event.getPlayer().hasPermission("oitc.updatenotify")) {
			return;
		}

		Bukkit.getScheduler().runTaskLater(plugin, () -> UpdateChecker.init(plugin, 81185).requestUpdateCheck().whenComplete((result, exception) -> {
			if (!result.requiresUpdate()) {
				return;
			}

			String newestVersion = result.getNewestVersion();
			Player player = event.getPlayer();

			if (newestVersion.contains("b")) {
				player.sendMessage(plugin.getChatManager().coloredRawMessage("&3[OITC] &bFound a beta update: v" + newestVersion + " Download:"));
			} else {
				player.sendMessage(plugin.getChatManager().coloredRawMessage("&3[OITC] &bFound an update: v" + newestVersion + " Download:"));
			}

			player.sendMessage(plugin.getChatManager().coloredRawMessage("&3>> &bhttps://www.spigotmc.org/resources/one-in-the-chamber-1-12-1-16-5.81185/"));
		}), 25);
	}
}