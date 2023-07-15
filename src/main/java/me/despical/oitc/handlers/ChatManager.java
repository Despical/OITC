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

package me.despical.oitc.handlers;

import me.clip.placeholderapi.PlaceholderAPI;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.string.StringFormatUtils;
import me.despical.commons.string.StringUtils;
import me.despical.commons.util.Strings;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.user.User;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ChatManager {

	private FileConfiguration config;

	private final Main plugin;
	private final String prefix;
	private final boolean papiEnabled;

	public ChatManager(Main plugin) {
		this.plugin = plugin;
		this.config = ConfigUtils.getConfig(plugin, "messages");
		this.prefix = message("in_game.plugin_prefix");
		this.papiEnabled = plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
	}

	public boolean isPapiEnabled() {
		return papiEnabled;
	}

	public String coloredRawMessage(String message) {
		return Strings.format(message);
	}

	public String prefixedRawMessage(String message) {
		return prefix + coloredRawMessage(message);
	}

	public String message(String path) {
		path = me.despical.commons.string.StringUtils.capitalize(path.replace('_', '-'), '-', '.');
		return coloredRawMessage(config.getString(path));
	}

	public String prefixedMessage(String path) {
		return prefix + message(path);
	}
	
	public String message(String message, Player player) {
		String returnString = message(message);

		if (papiEnabled) {
			returnString = PlaceholderAPI.setPlaceholders(player, returnString);
		}

		return coloredRawMessage(returnString);
	}

	public String prefixedMessage(String message, Player player) {
		return prefix + message(message, player);
	}

	public String prefixedMessage(String message, int value) {
		return prefix + message(message, value);
	}

	public String message(String path, int integer) {
		return formatMessage(null, message(path), integer);
	}

	public String formatMessage(Arena arena, String message, Player player) {
		String returnString = message;

		returnString = returnString.replace("%player%", player.getName());
		returnString = formatPlaceholders(returnString, arena);

		if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			returnString = PlaceholderAPI.setPlaceholders(player, returnString);
		}

		return coloredRawMessage(returnString);
	}

	public String formatMessage(Arena arena, String path) {
		String returnString = message(path);

		returnString = formatPlaceholders(returnString, arena);

		return coloredRawMessage(returnString);
	}

	public String prefixedFormattedMessage(Arena arena, String message, Player player) {
		return prefix + formatMessage(arena, message, player);
	}

	public String prefixedFormattedMessage(Arena arena, String path, int value) {
		return prefix + formatMessage(arena, message(path), value);
	}


	private String formatPlaceholders(String message, Arena arena) {
		String returnString = message;

		returnString = returnString.replace("%arena%", arena.getMapName());
		returnString = returnString.replace("%time%", Integer.toString(arena.getTimer()));
		returnString = returnString.replace("%formatted_time%", StringFormatUtils.formatIntoMMSS(arena.getTimer()));
		returnString = returnString.replace("%players%", Integer.toString(arena.getPlayers().size()));
		returnString = returnString.replace("%maxplayers%", Integer.toString(arena.getMaximumPlayers()));
		returnString = returnString.replace("%minplayers%", Integer.toString(arena.getMinimumPlayers()));
		return returnString;
	}

	public String formatMessage(Arena arena, String message, int integer) {
		String returnString = message;

		returnString = returnString.replace("%number%", Integer.toString(integer));
		return arena != null ? formatPlaceholders(returnString, arena) : returnString;
	}

	public String getStreakMessage() {
		final List<String> quotes = getStringList("In-Game.Messages.Kill-Streak");

		return quotes.get(ThreadLocalRandom.current().nextInt(quotes.size()));
	}

	public List<String> getStringList(String path) {
		path = me.despical.commons.string.StringUtils.capitalize(path.replace('_', '-'), '-', '.');
		return config.getStringList(path);
	}

	public void broadcastAction(Arena arena, User user, ActionType action) {
		if (!user.isSpectator()) {
			arena.broadcastMessage(formatMessage(arena, message("in_game.messages." + StringUtils.capitalize(action.name().toLowerCase(Locale.ENGLISH))), user.getPlayer()));
		}
	}
	
	public void reloadConfig() {
		config = ConfigUtils.getConfig(plugin, "messages");
	}

	public enum ActionType {
		JOIN, LEAVE
	}
}