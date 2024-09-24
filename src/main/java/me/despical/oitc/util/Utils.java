package me.despical.oitc.util;

import me.despical.commons.compat.XMaterial;
import me.despical.oitc.Main;
import me.despical.oitc.addons.oraxen.OraxenAddon;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author Despical
 * <p>
 * Created at 2.07.2024
 */
public final class Utils {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);

	private Utils() {
	}

	public static void addPotionEffect(Player player, PotionEffectType type, int duration, int amplifier) {
		PotionEffect effect;

		try {
			effect = new PotionEffect(type, duration, amplifier, false, false, false);
		} catch (Throwable throwable) {
			effect = new PotionEffect(type, duration, amplifier, false, false);
		}

		player.addPotionEffect(effect);
	}

	public static void addItem(Player player, ItemStack itemStack, int slot) {
		if (player == null) {
			return;
		}

		Inventory inv = player.getInventory();
		ItemStack item = inv.getItem(slot);

		if (item != null) {
			item.setAmount(item.getAmount() + itemStack.getAmount());
		} else {
			inv.setItem(slot, itemStack);
		}
	}

	public static ItemStack getItem(String material) {
		if (material.startsWith("oraxen:")) {
			material = material.substring(7);

			return plugin.getAddonManager().<OraxenAddon>getAddon("Oraxen").orElseThrow().getItem(material);
		}

		return XMaterial.matchXMaterial(material).orElseThrow().parseItem();
	}
}