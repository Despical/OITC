package me.despical.oitc.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import me.despical.commonsbox.serializer.InventorySerializer;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.handlers.PermissionsManager;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class JoinEvent implements Listener {

	private Main plugin;

	public JoinEvent(Main plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent e) {
		if (!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED) && !plugin.getServer().hasWhitelist() || e.getResult() != PlayerLoginEvent.Result.KICK_WHITELIST) {
			return;
		}
		if (e.getPlayer().hasPermission(PermissionsManager.getJoinFullGames())) {
			e.setResult(PlayerLoginEvent.Result.ALLOWED);
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		plugin.getUserManager().loadStatistics(plugin.getUserManager().getUser(event.getPlayer()));
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			ArenaRegistry.getArenas().get(ArenaRegistry.getBungeeArena()).teleportToLobby(event.getPlayer());
			return;
		}
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			if (ArenaRegistry.getArena(player) == null) {
				continue;
			}
			player.hidePlayer(plugin, event.getPlayer());
			event.getPlayer().hidePlayer(plugin, player);
		}
		if (plugin.getConfigPreferences().getOption(ConfigPreferences.Option.INVENTORY_MANAGER_ENABLED)) {
			InventorySerializer.loadInventory(plugin, event.getPlayer());
		}
	}
	
//	@EventHandler not implemented yet
//	public void onJoinCheckVersion(final PlayerJoinEvent event) {
//		if (!plugin.getConfig().getBoolean("Update-Notifier.Enabled", true) || !event.getPlayer().hasPermission("oitc.updatenotify")) {
//			return;
//		}
//		Bukkit.getScheduler().runTaskLater(plugin, () -> UpdateChecker.init(plugin, 1).requestUpdateCheck().whenComplete((result, exception) -> {
//			if (!result.requiresUpdate()) {
//				return;
//			}
//			if (result.getNewestVersion().contains("b")) {
//				} else {
//			}
//		}), 25);
//	}
}