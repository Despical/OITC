package me.despical.oitc.addons;

import me.despical.commons.configuration.ConfigUtils;
import me.despical.oitc.Main;
import me.despical.oitc.events.EventListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Despical
 * <p>
 * Created at 28.08.2024
 */
public abstract class Addon<T extends JavaPlugin> extends EventListener {

	protected final T addonPlugin;
	protected final String addonName;
	protected FileConfiguration config;

	public Addon(Main plugin, String addonName, Class<T> clazz) {
		super(plugin);
		this.addonPlugin = (T) JavaPlugin.getProvidingPlugin(clazz);
		this.config = ConfigUtils.getConfig(plugin, "addons");
		this.addonName = addonName;
	}

	public final String getName() {
		return addonName;
	}

	@SuppressWarnings("unchecked")
	protected final <A> A get(String path) {
		return (A) config.get(String.format("%s.%s", addonName, path));
	}

	public void reload() {
		this.config = ConfigUtils.getConfig(plugin, "addons");
	}
}