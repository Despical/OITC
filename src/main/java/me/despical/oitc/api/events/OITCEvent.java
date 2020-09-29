package me.despical.oitc.api.events;

import me.despical.oitc.arena.Arena;
import org.bukkit.event.Event;

/**
 * Represents One in the Chamber game related events.
 */
public abstract class OITCEvent extends Event {

	protected Arena arena;

	public OITCEvent(Arena eventArena) {
		arena = eventArena;
	}

	/**
	 * Returns event arena
	 *
	 * @return event arena
	 */
	public Arena getArena() {
		return arena;
	}
}