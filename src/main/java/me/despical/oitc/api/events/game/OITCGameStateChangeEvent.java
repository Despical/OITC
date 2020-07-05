package me.despical.oitc.api.events.game;

import org.bukkit.event.HandlerList;

import me.despical.oitc.api.events.OITCEvent;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaState;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Called when arena game state has changed.
 */
public class OITCGameStateChangeEvent extends OITCEvent {

	private static final HandlerList HANDLERS = new HandlerList();
	private ArenaState arenaState;

	public OITCGameStateChangeEvent(Arena eventArena, ArenaState arenaState) {
		super(eventArena);
		this.arenaState = arenaState;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public ArenaState getArenaState() {
		return arenaState;
	}
}