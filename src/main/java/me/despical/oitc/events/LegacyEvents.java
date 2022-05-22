package me.despical.oitc.events;

import me.despical.commons.compat.VersionResolver;
import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * @author Despical
 * <p>
 * Created at 22.05.2022
 */
public class LegacyEvents {

	private final Main plugin;

	public LegacyEvents(Main plugin) {
		this.plugin = plugin;

		if (VersionResolver.isCurrentHigher(VersionResolver.ServerVersion.v1_9_R2)) {
			registerItemSwapEvent();
		}
	}

	public void registerItemSwapEvent() {
		plugin.getServer().getPluginManager().registerEvents(new Listener() {

			@EventHandler
			public void onItemSwap(PlayerSwapHandItemsEvent event) {
				if (ArenaRegistry.isInArena(event.getPlayer())) {
					event.setCancelled(true);
				}
			}
		}, plugin);
	}
}