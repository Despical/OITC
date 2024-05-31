package me.despical.oitc.util;

import org.bukkit.inventory.ItemStack;

/**
 * @author Despical
 * <p>
 * Created at 31.05.2024
 */
public class Utils {

	private Utils() {
	}

	public static boolean checkItemsEqual(ItemStack first, ItemStack second) {
		if (first == null || second == null) return false;
		if (first.getType() != second.getType()) return false;
		if (first.getAmount() != second.getAmount()) return false;
		if (!first.getItemMeta().getDisplayName().equals(second.getItemMeta().getDisplayName())) return false;
		return first.getItemMeta().getLore() == null || second.getItemMeta().getLore() == null || first.getItemMeta().getLore().equals(second.getItemMeta().getLore());
	}
}