package me.despical.oitc.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.user.User;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class QuitEvent implements Listener {

	private Main plugin;

	public QuitEvent(Main plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if (ArenaRegistry.isInArena(event.getPlayer())) {
			ArenaManager.leaveAttempt(event.getPlayer(), ArenaRegistry.getArena(event.getPlayer()));
		}
		final User user = plugin.getUserManager().getUser(event.getPlayer());
		plugin.getUserManager().removeUser(user);
	}
}