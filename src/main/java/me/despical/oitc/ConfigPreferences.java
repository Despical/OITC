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

package me.despical.oitc;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class ConfigPreferences {

	private final Main plugin;
	private final Map<Option, Boolean> options;
	private final Map<IntOption, Integer> intOptions;

	public ConfigPreferences(Main plugin) {
		this.plugin = plugin;
		this.options = new HashMap<>();
		this.intOptions = new HashMap<>();

		loadOptions();
	}

	public boolean getOption(Option option) {
		return options.get(option);
	}

	public int getIntOption(IntOption option) {
		return intOptions.get(option);
	}

	private void loadOptions() {
		for (Option option : Option.values()) {
			options.put(option, plugin.getConfig().getBoolean(option.getPath(), option.getDefault()));
		}

		for (IntOption option : IntOption.values()) {
			intOptions.put(option, plugin.getConfig().getInt(option.getPath(), option.getDefault()));
		}
	}

	public enum IntOption {
		CLASSIC_GAMEPLAY_TIME("Classic-Gameplay-Time", 600), STARTING_WAITING_TIME("Starting-Waiting-Time", 60),
		STARTING_TIME_ON_FULL_LOBBY("Start-Time-On-Full-Lobby", 15);

		private final String path;
		private final int def;

		IntOption(String path, int def) {
			this.path = path;
			this.def = def;
		}

		public String getPath() {
			return path;
		}

		public int getDefault() {
			return def;
		}
	}

	public enum Option {
		BOSS_BAR_ENABLED("Boss-Bar-Enabled"), BUNGEE_ENABLED("BungeeActivated", false),
		CHAT_FORMAT_ENABLED("ChatFormat-Enabled"), DATABASE_ENABLED("DatabaseActivated", false),
		DISABLE_FALL_DAMAGE("Disable-Fall-Damage", false), DISABLE_SEPARATE_CHAT("Disable-Separate-Chat", false),
		ENABLE_SHORT_COMMANDS("Enable-Short-Commands", false), INVENTORY_MANAGER_ENABLED("InventoryManager"),
		NAMETAGS_HIDDEN("Nametags-Hidden"), UPDATE_NOTIFIER_ENABLED("Update-Notifier.Enabled"),
		BLOCK_COMMANDS("Block-Commands-In-Game"), DISABLE_LEAVE_COMMAND("Disable-Leave-Command"),
		SIGNS_BLOCK_STATES_ENABLED("Signs-Block-States-Enabled"), REWARDS_ENABLED("Rewards-Enabled", false);

		private final String path;
		private final boolean def;

		Option(String path) {
			this(path, true);
		}

		Option(String path, boolean def) {
			this.path = path;
			this.def = def;
		}

		public String getPath() {
			return path;
		}

		public boolean getDefault() {
			return def;
		}
	}
}