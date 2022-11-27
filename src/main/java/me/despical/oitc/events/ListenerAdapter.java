package me.despical.oitc.events;

import me.despical.commons.util.LogUtils;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.handlers.ChatManager;
import me.despical.oitc.user.UserManager;
import org.bukkit.event.Listener;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Despical
 * <p>
 * Created at 31.07.2022
 */
public abstract class ListenerAdapter implements Listener {

	protected final Main plugin;
	protected final ChatManager chatManager;
	protected final UserManager userManager;
	protected final ConfigPreferences preferences;

	public ListenerAdapter(Main plugin) {
		this.plugin = plugin;
		this.chatManager = plugin.getChatManager();
		this.userManager = plugin.getUserManager();
		this.preferences = plugin.getConfigPreferences();
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	protected void registerIf(Predicate<Boolean> predicate, Supplier<Listener> supplier) {
		if (predicate.test(false)) return;

		plugin.getServer().getPluginManager().registerEvents(supplier.get(), plugin);
	}

	@SafeVarargs
	public static void registerEvents(Main plugin, Class<? extends ListenerAdapter>... listenerAdapters) {
		try {
			for (Class<? extends ListenerAdapter> listenerAdapter : listenerAdapters) {
				listenerAdapter.getConstructor(Main.class).newInstance(plugin);
			}
		} catch (Exception ignored) {
			LogUtils.sendConsoleMessage("&cAn exception occured on event registering.");
		}
	}
}