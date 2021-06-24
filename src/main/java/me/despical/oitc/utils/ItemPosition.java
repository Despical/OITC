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

package me.despical.oitc.utils;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.item.ItemBuilder;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 * @author Despical
 * <p>
 * Created at 03.17.2020
 */
public enum ItemPosition {

	SWORD(0, new ItemBuilder(XMaterial.WOODEN_SWORD).unbreakable(true)),
	BOW(1, new ItemBuilder(XMaterial.BOW).enchantment(Enchantment.LUCK).flag(ItemFlag.HIDE_ENCHANTS).unbreakable(true)),
	ARROW(7, new ItemBuilder(XMaterial.ARROW));
	
	private final int itemPosition;
	private final ItemStack itemStack;

	ItemPosition(int itemPosition, ItemBuilder itemBuilder) {
		this.itemPosition = itemPosition;
		this.itemStack = itemBuilder.build();
	}

	/**
	 * Adds target item to specified hot bar position.
	 * Item will be added if there is already set or 
	 * will be set when no item is set in the position.
	 *
	 * @param player to add item to
	 * @param itemPosition position of item to set/add
	 * @param itemStack to be added at item position or set at
	 */
	public static void addItem(Player player, ItemPosition itemPosition, ItemStack itemStack) {
		if (player == null) {
			return;
		}

		Inventory inv = player.getInventory();

		if (inv.getItem(itemPosition.getItemPosition()) != null) {
			inv.getItem(itemPosition.getItemPosition()).setAmount(inv.getItem(itemPosition.getItemPosition()).getAmount() + itemStack.getAmount());
		} else {
			inv.setItem(itemPosition.getItemPosition(), itemStack);
		}
	}

	/**
	 * Sets target item in specified hot bar position.
	 * If item there is already set it will be incremented 
	 * by itemStack amount if possible.
	 *
	 * @param player to set item to
	 * @param itemPositions array of item to set
	 */
	public static void setItem(Player player, ItemPosition... itemPositions) {
		if (player == null) {
			return;
		}

		for (ItemPosition itemPosition : itemPositions) {
			player.getInventory().setItem(itemPosition.getItemPosition(), itemPosition.itemStack);
		}
	}

	/**
	 * Gives game kit to specified player.
	 *
	 * @param player to give kit
	 */
	public static void giveKit(Player player) {
		player.getInventory().clear();
		setItem(player, SWORD, BOW, ARROW);
		player.updateInventory();
	}
	
	public int getItemPosition() {
		return itemPosition;
	}

	public ItemStack getItem() {
		return itemStack;
	}
}