/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2023 Despical
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package me.despical.oitc.api.events.game;

import me.despical.oitc.api.events.OITCEvent;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaState;
import org.bukkit.event.HandlerList;

/**
 * @author Despical
 * @since 1.0.0
 * <p>
 */
public class OITCGameStateChangeEvent extends OITCEvent {

	private static final HandlerList HANDLERS = new HandlerList();
	private final ArenaState arenaState;

	public OITCGameStateChangeEvent(Arena eventArena, ArenaState arenaState) {
		super (eventArena);
		this.arenaState = arenaState;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public ArenaState getState() {
		return arenaState;
	}
}