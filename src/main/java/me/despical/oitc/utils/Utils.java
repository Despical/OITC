package me.despical.oitc.utils;

import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Utils {

	private static Main plugin;

	private Utils() {}

	public static void init(Main plugin) {
		Utils.plugin = plugin;
	}

	/**
	 * Serialize int to use it in Inventories size ex. you have 38 kits and it will
	 * serialize it to 45 (9*5) because it is valid inventory size next ex. you have
	 * 55 items and it will serialize it to 63 (9*7) not 54 because it's too less
	 *
	 * @param i integer to serialize
	 * @return serialized number
	 */
	public static int serializeInt(Integer i) {
		if (i == 0) return 9;
		return (i % 9) == 0 ? i : (i + 9 - 1) / 9 * 9;
	}

	public static boolean checkIsInGameInstance(Player player) {
		if (ArenaRegistry.getArena(player) == null) {
			player.sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("Commands.Not-Playing", player));
			return false;
		}

		return true;
	}

	public static boolean setPlayerHead(Player player, SkullMeta meta) {
		if (Bukkit.getServer().getVersion().contains("Paper") && player.getPlayerProfile().hasTextures()) {
			return CompletableFuture.supplyAsync(() -> {
				meta.setPlayerProfile(player.getPlayerProfile());
				return true;
			}).exceptionally(e -> {
				Debugger.debug(java.util.logging.Level.WARNING, "Retrieving player profile of "+ player.getName() +" failed!");
				return null;
			}).isDone();
		}

		meta.setOwningPlayer(player);
		return true;
	}

	public static String matchColorRegex(String s) {
		String regex = "&?#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})";
		Matcher matcher = Pattern.compile(regex).matcher(s);

		while (matcher.find()) {
			String group = matcher.group(0);
			String group2 = matcher.group(1);

			try {
				s = s.replace(group, net.md_5.bungee.api.ChatColor.of("#" + group2) + "");
			} catch (Exception e) {
				Debugger.debug("Wrong hex color match: " + group);
			}
		}

		return s;
	}
}