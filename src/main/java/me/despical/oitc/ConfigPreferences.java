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

package me.despical.oitc;

import me.despical.commons.serializer.InventorySerializer;
import me.despical.commons.string.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ConfigPreferences {

	private final Main plugin;
	private final Map<Option, Boolean> options;

	public ConfigPreferences(Main plugin) {
		this.plugin = plugin;
		this.options = new HashMap<>();
		this.loadOptions();
	}

	private void loadOptions() {
		for (Option option : Option.values()) {
			options.put(option, plugin.getConfig().getBoolean(option.path, option.def));
		}
	}

	public void reload() {
		this.options.clear();
		this.loadOptions();
	}

	public boolean getOption(Option option) {
		return options.get(option);
	}

	public enum Option {

		BLOCK_COMMANDS,
		BUNGEE_ENABLED(false),
		CHAT_FORMAT_ENABLED,
		DATABASE_ENABLED(false),
		DISABLE_FALL_DAMAGE(false),
		DISABLE_LEAVE_COMMAND(false),
		DISABLE_SEPARATE_CHAT(false),
		DISABLE_SPECTATING_ON_BUNGEE(false),
		ENABLE_ARROW_PICKUPS(false),
		ENABLE_SHORT_COMMANDS,
		GAME_BAR_ENABLED,
		HEAL_ON_KILL(false),
		HEAL_PLAYER((config) -> {
			final List<String> list = config.getStringList("Inventory-Manager.Do-Not-Restore");
			list.forEach(InventorySerializer::addNonSerializableElements);

			return !list.contains("health");
		}),
		HIDE_PLAYERS,
		INSTANT_LEAVE(false),
		INVENTORY_MANAGER_ENABLED("Inventory-Manager.Enabled"),
		LEVEL_COUNTDOWN_ENABLED(false),
		NAME_TAGS_HIDDEN,
		REGEN_ENABLED(false),
		UPDATE_NOTIFIER_ENABLED;

		final String path;
		final boolean def;

		Option() {
			this(true);
		}

		Option(boolean def) {
			this.def = def;
			this.path = StringUtils.capitalize(name().replace('_', '-').toLowerCase(Locale.ENGLISH), '-', '.');
		}

		Option(String path) {
			this.def = true;
			this.path = path;
		}

		Option(Function<FileConfiguration, Boolean> supplier) {
			this.path = "";
			this.def = supplier.apply(JavaPlugin.getPlugin(Main.class).getConfig());
		}
	}
}