package me.despical.oitc.addons.oraxen;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import me.despical.oitc.Main;
import me.despical.oitc.addons.Addon;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * @author Despical
 * <p>
 * Created at 28.08.2024
 */
public class OraxenAddon extends Addon<OraxenPlugin> {

	public OraxenAddon(Main plugin) {
		super(plugin, "Oraxen", OraxenPlugin.class);
	}

	@Nullable
	public ItemStack getItem(String material) {
		var itemBuilder = OraxenItems.getItemById(material);

		if (itemBuilder == null) {
			plugin.getLogger().log(Level.WARNING, "We could not find an item called ''{0}'' using the Oraxen API!", material);
			return null;
		}

		return itemBuilder.build();
	}
}