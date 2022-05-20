package me.despical.oitc.arena.managers;

import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import org.bukkit.boss.BossBar;

/**
 * @author Despical
 * <p>
 * Created at 19.05.2022
 */
public class BossBarManager {

	private final Main plugin;
	private final Arena arena;

	private BossBar bossBar;

	public BossBarManager(Main plugin, Arena arena) {
		this.plugin = plugin;
		this.arena = arena;
	}
}