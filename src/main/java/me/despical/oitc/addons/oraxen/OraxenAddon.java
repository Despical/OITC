package me.despical.oitc.addons.oraxen;

import me.despical.oitc.Main;
import me.despical.oitc.addons.Addon;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * @author Despical
 * <p>
 * Created at 28.08.2024
 */
public class OraxenAddon extends Addon<Main> {

	public OraxenAddon(Main plugin) {
		super(plugin, "Oraxen", Main.class);
	}

	@Nullable
	public ItemStack getItem(String material) {
		try {
			Class<?> oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
			Method getItemByIdMethod = oraxenItemsClass.getMethod("getItemById", String.class);
			Object itemBuilder = getItemByIdMethod.invoke(null, material);

			if (itemBuilder == null) {
				plugin.getLogger().log(Level.WARNING, "We could not find an item called ''{0}'' using the Oraxen API!", material);
				return null;
			}

			Method buildMethod = itemBuilder.getClass().getMethod("build");

			return (ItemStack) buildMethod.invoke(itemBuilder);
		} catch (Exception exception) {
			exception.printStackTrace();

			plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to get the item: " + exception.getMessage());
			return null;
		}
	}
}