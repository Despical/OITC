package me.despical.oitc.api.events.game;

import me.despical.oitc.api.events.OITCEvent;
import me.despical.oitc.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Called when player is attempting to join arena.
 */
public class OITCGameJoinAttemptEvent extends OITCEvent implements Cancellable {

	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private boolean isCancelled;

	public OITCGameJoinAttemptEvent(Player player, Arena targetArena) {
		super(targetArena);
		this.player = player;
		this.isCancelled = false;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	public boolean isCancelled() {
		return this.isCancelled;
	}

	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}

	public Player getPlayer() {
		return player;
	}

	public HandlerList getHandlers() {
		return HANDLERS;
	}
}