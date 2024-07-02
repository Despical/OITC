package me.despical.oitc.arena.managers;

import me.despical.commons.reflection.XReflection;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.user.User;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * @author Despical
 * <p>
 * Created at 2.07.2024
 */
public final class GameBarManager {

	@Nullable
	private BossBar gameBar;

	private final Arena arena;
	private final Main plugin;
	private final boolean enabled;

	public GameBarManager(final Arena arena, final Main plugin) {
		this.arena = arena;
		this.plugin = plugin;
		this.enabled = XReflection.supports(9) && plugin.getOption(ConfigPreferences.Option.GAME_BAR_ENABLED);

		if (enabled) {
			this.gameBar = plugin.getServer().createBossBar("", BarColor.BLUE, BarStyle.SOLID);
		}
	}

	public void doBarAction(final User user, int action) {
		if (!enabled) return;

		final Player player = user.getPlayer();

		if (action == 1) {
			this.gameBar.addPlayer(player);
		} else {
			this.gameBar.removePlayer(player);
		}
	}

	public void removeAll() {
		if (this.gameBar != null) this.gameBar.removeAll();
	}

	public void handleGameBar() {
		if (this.gameBar == null) return;

		switch (arena.getArenaState()) {
			case WAITING_FOR_PLAYERS:
				setTitle("Game-Bar.Waiting-For-Players");
				break;
			case STARTING:
				setTitle("Game-Bar.Starting");
				break;
			case IN_GAME:
				setTitle("Game-Bar.In-Game");
				break;
			case ENDING:
				setTitle("Game-Bar.Ending");
				break;
		}

		gameBar.setVisible(!this.gameBar.getTitle().isEmpty());
	}

	private void setTitle(final String path) {
		this.gameBar.setTitle(plugin.getChatManager().formatMessage(arena, path));
	}
}