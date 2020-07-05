package me.despical.oitc.api.events.game;

import org.bukkit.event.HandlerList;

import me.despical.oitc.api.events.OITCEvent;
import me.despical.oitc.arena.Arena;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 * Called when arena is stopped.
 */
public class OITCGameStopEvent extends OITCEvent {

	private static final HandlerList HANDLERS = new HandlerList();

	public OITCGameStopEvent(Arena arena) {
		super(arena);
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	public HandlerList getHandlers() {
		return HANDLERS;
	}
}