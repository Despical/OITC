package me.despical.oitc.addons;

import me.despical.commons.configuration.ConfigUtils;
import me.despical.oitc.Main;
import me.despical.oitc.addons.oraxen.OraxenAddon;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 28.08.2024
 */
public class AddonManager {

	private final Main plugin;
	private final Set<Addon<?>> addons;

	public AddonManager(Main plugin) {
		this.plugin = plugin;
		this.addons = new HashSet<>();

		this.registerAddons();
	}

	private void registerAddons() {
		if (isEnabled("Oraxen")) this.addons.add(new OraxenAddon(plugin));
	}

	public Set<Addon<?>> getAddons() {
		return addons;
	}

	@SuppressWarnings("unchecked")
	public <A extends Addon<?>> Optional<A> getAddon(String name) {
		return (Optional<A>) this.addons.stream().filter(addon -> name.equals(addon.getName())).findFirst();
	}

	public boolean isEnabled(String name) {
		final FileConfiguration config = ConfigUtils.getConfig(plugin, "addons");
		return plugin.getServer().getPluginManager().isPluginEnabled(name) && config.getBoolean(String.format("%s.Enabled", name));
	}

	public void reload() {
		this.addons.forEach(Addon::reload);
	}
}