package me.despical.oitc.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author Despical
 * <p>
 * Created at 03.17.2020
 */
public enum ItemPosition {

	SWORD(0), BOW(1), ARROW(7);
	
	private int itemPosition;

	ItemPosition(int itemPosition) {
		this.itemPosition = itemPosition;
	}

	/**
	 * Adds target item to specified hotbar position.
	 * Item will be added if there is already set or 
	 * will be set when no item is set in the position.
	 *
	 * @param player       player to add item to
	 * @param itemPosition position of item to set/add
	 * @param itemStack    itemstack to be added at itemPostion or set at
	 *                     itemPosition
	 */
	public static void addItem(Player player, ItemPosition itemPosition, ItemStack itemStack) {
		if (player == null) {
			return;
		}
		Inventory inv = player.getInventory();
		if (inv.getItem(itemPosition.getItemPosition()) != null) {
			inv.getItem(itemPosition.getItemPosition()).setAmount(inv.getItem(itemPosition.getItemPosition()).getAmount() + itemStack.getAmount());
			return;
		} else {
			inv.setItem(itemPosition.getItemPosition(), itemStack);
			return;
		}
	}

	/**
	 * Sets target item in specified hotbar position.
	 * If item there is already set it will be incremented 
	 * by itemStack amount if possible.
	 *
	 * @param player       player to set item to
	 * @param itemPosition position of item to set
	 * @param itemStack    itemstack to set at itemPosition
	 */
	public static void setItem(Player player, ItemPosition itemPosition, ItemStack itemStack) {
		if (player == null) {
			return;
		}
		Inventory inv = player.getInventory();
		inv.setItem(itemPosition.getItemPosition(), itemStack);
	}
	
	public int getItemPosition() {
		return itemPosition;
	}
}