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

package me.despical.oitc.handlers.items;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.util.Collections;
import me.despical.oitc.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SpecialItem {

	private static Main plugin;
	private ItemStack itemStack;
	private int slot;
	private final String name;

	public SpecialItem(String name) {
		this.name = name;
	}

	public static void init(Main plugin) {
		SpecialItem.plugin = plugin;

		new SpecialItem("Leave").load("&c&lReturn to Lobby &7(Right Click)", XMaterial.RED_BED, 8, "&7Right-click to leave to the lobby!");
		new SpecialItem("Teleporter").load("&a&lTeleporter &7(Right Click) ", XMaterial.COMPASS, 0, "&7Right-click to spectate players!");
		new SpecialItem("Spectator-Settings").load("&b&lSpectator Settings &7(Right Click)", XMaterial.REPEATER, 4, "&7Right-click to change your spectator settings!");
		new SpecialItem("Play-Again").load("&b&lPlay Again &7(Right Click)", XMaterial.PAPER, 7, "&7Right-click to play another game!");
	}

	public void load(String displayName, XMaterial material, int slot, String... lore) {
		FileConfiguration config = ConfigUtils.getConfig(plugin, "items");

		if (!config.contains(name)) {
			config.set(name + ".displayname", displayName);
			config.set(name + ".lore", Collections.listOf(lore));
			config.set(name + ".material-name", material);
			config.set(name + ".slot", slot);
		}

		ConfigUtils.saveConfig(plugin, config, "items");
		ItemStack stack = XMaterial.matchXMaterial(config.getString(name + ".material-name")).orElse(XMaterial.STONE).parseItem();
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName(plugin.getChatManager().coloredRawMessage(config.getString(name + ".displayname")));
		meta.setLore(config.getStringList(name + ".lore").stream().map(plugin.getChatManager()::coloredRawMessage).collect(Collectors.toList()));
		stack.setItemMeta(meta);

		SpecialItem item = new SpecialItem(name);
		item.itemStack = stack;
		item.slot = slot;

		SpecialItemManager.addItem(name, item);
	}

	public int getSlot() {
		return slot;
	}

	public ItemStack getItemStack() {
		return itemStack;
	}
}