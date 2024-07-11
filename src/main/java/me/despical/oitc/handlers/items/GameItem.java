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

package me.despical.oitc.handlers.items;

import me.despical.commons.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class GameItem {

	private final ItemStack itemStack;
	private final int slot;
	private final List<Action> actions;

	public GameItem(String displayName, Material material, int slot, List<String> lore, List<String> actions) {
		this.itemStack = new ItemBuilder(material).name(displayName).lore(lore).unbreakable(true).flag(ItemFlag.HIDE_UNBREAKABLE).build();
		this.slot = slot;
		this.actions = actions.stream().map(Action::valueOf).collect(Collectors.toList());
	}

	public GameItem(String displayName, Material material, int slot, List<String> lore, List<ItemFlag> flags, Map<Enchantment, Integer> enchants) {
		ItemBuilder builder = new ItemBuilder(material).name(displayName).lore(lore).unbreakable(true).flag(ItemFlag.HIDE_UNBREAKABLE);

		enchants.forEach(builder::enchantment);
		flags.forEach(builder::flag);

		this.itemStack = builder.build();
		this.slot = slot;
		this.actions = new ArrayList<>();
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public int getSlot() {
		return slot;
	}

	public boolean checkAction(Action action) {
		return !(actions.isEmpty() || actions.contains(action));
	}

	public boolean equals(ItemStack item) {
		final ItemMeta meta = item.getItemMeta();
		final ItemMeta itemStackMeta = itemStack.getItemMeta();

		return
			item.getType() == itemStack.getType() &&
			meta.getDisplayName().equals(itemStackMeta.getDisplayName()) &&
			meta.getLore() != null && meta.getLore().equals(itemStackMeta.getLore());
	}
}