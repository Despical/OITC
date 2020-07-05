package me.despical.oitc.api.events.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.api.events.OITCEvent;
import me.despical.oitc.arena.Arena;

/**
 * @author Despical
 * @see StatsStorage.StatisticType
 * @since 1.0.0
 * <p>
 * Called when player receive new statistic.
 */
public class OITCPlayerStatisticChangeEvent extends OITCEvent {

	private static final HandlerList HANDLERS = new HandlerList();
	private Player player;
	private StatsStorage.StatisticType statisticType;
	private int number;

	public OITCPlayerStatisticChangeEvent(Arena eventArena, Player player, StatsStorage.StatisticType statisticType, int number) {
		super(eventArena);
		this.player = player;
		this.statisticType = statisticType;
		this.number = number;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public Player getPlayer() {
		return player;
	}

	public StatsStorage.StatisticType getStatisticType() {
		return statisticType;
	}

	public int getNumber() {
		return number;
	}
}