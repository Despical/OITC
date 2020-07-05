package me.despical.oitc.handlers;

import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import me.despical.oitc.Main;
import me.despical.oitc.utils.Debugger;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class PermissionsManager {

	private static Main plugin = JavaPlugin.getPlugin(Main.class);
	private static String joinFullPerm = "oitc.fullgames";
	private static String joinPerm = "oitc.join.<arena>";

	public static void init() {
		setupPermissions();
	}

	public static String getJoinFullGames() {
		return joinFullPerm;
	}

	private static void setJoinFullGames(String joinFullGames) {
		PermissionsManager.joinFullPerm = joinFullGames;
	}

	public static String getJoinPerm() {
		return joinPerm;
	}

	private static void setJoinPerm(String joinPerm) {
		PermissionsManager.joinPerm = joinPerm;
	}

	private static void setupPermissions() {
		PermissionsManager.setJoinFullGames(plugin.getConfig().getString("Basic-Permissions.Full-Games-Permission", "oitc.fullgames"));
		PermissionsManager.setJoinPerm(plugin.getConfig().getString("Basic-Permissions.Join-Permission", "oitc.join.<arena>"));
		Debugger.debug(Level.INFO, "Basic permissions registered");
	}
}