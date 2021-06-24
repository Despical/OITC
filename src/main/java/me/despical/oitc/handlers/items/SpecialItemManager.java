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

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SpecialItemManager {

	private static final HashMap<String, SpecialItem> specialItems = new HashMap<>();

	public static void addItem(String name, SpecialItem entityItem) {
		specialItems.put(name, entityItem);
	}

	public static SpecialItem getSpecialItem(String name) {
		return specialItems.get(name);
	}

	public static String getRelatedSpecialItem(ItemStack itemStack) {
		for (String key : specialItems.keySet()) {
			SpecialItem entityItem = getSpecialItem(key);

			if (entityItem.getItemStack().getItemMeta().getDisplayName().equalsIgnoreCase(itemStack.getItemMeta().getDisplayName())) {
				return key;
			}
		}

		return null;
	}

	public static void giveItem(Player player, String... names) {
		for (String name : names) {
			SpecialItem item = getSpecialItem(name);

			if (item == null) continue;

			player.getInventory().setItem(item.getSlot(), item.getItemStack());
		}

		player.updateInventory();
	}
}