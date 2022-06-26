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

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Pattern;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ChatEvents implements Listener {

	private final Main plugin;

	public ChatEvents(Main plugin) {
		this.plugin = plugin;

		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onChatInGame(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		Arena arena = ArenaRegistry.getArena(player);
		boolean disabledSeparateChat = plugin.getConfigPreferences().getOption(ConfigPreferences.Option.DISABLE_SEPARATE_CHAT);

		if (arena == null) {
			if (!disabledSeparateChat) {
				ArenaRegistry.getArenas().forEach(loopArena -> loopArena.getPlayers().forEach(p -> event.getRecipients().remove(p)));
			}

			return;
		}

		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.CHAT_FORMAT_ENABLED)) {
			String message = formatChatPlaceholders(plugin.getChatManager().message("In-Game.Game-Chat-Format"), player, event.getMessage().replaceAll(Pattern.quote("[$\\]"), ""));

			if (!disabledSeparateChat) {
				event.setCancelled(true);

				boolean dead = plugin.getUserManager().getUser(player).isSpectator();

				for (Player p : arena.getPlayers()) {
					if (dead && arena.getPlayersLeft().contains(p)) {
						continue;
					}

					p.sendMessage(dead ? formatChatPlaceholders(plugin.getChatManager().message("In-Game.Game-Death-Format"), player, null) + message : message);
				}

				plugin.getServer().getConsoleSender().sendMessage(message);
			} else {
				event.setMessage(message);
			}
		}
	}

	private String formatChatPlaceholders(String message, Player player, String saidMessage) {
		String formatted = message;

		formatted = StringUtils.replace(formatted, "%player%", player.getName());
		formatted = StringUtils.replace(formatted, "%message%", ChatColor.stripColor(saidMessage));

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			formatted = PlaceholderAPI.setPlaceholders(player, formatted);
		}

		return plugin.getChatManager().coloredRawMessage(formatted);
	}
}