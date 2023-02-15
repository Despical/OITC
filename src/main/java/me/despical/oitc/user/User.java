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

package me.despical.oitc.user;

import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.api.events.player.OITCPlayerStatisticChangeEvent;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.handlers.items.GameItem;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class User {

	private final static Main plugin = JavaPlugin.getPlugin(Main.class);

	private final UUID uuid;
	private final Player player;
	private final Map<StatsStorage.StatisticType, Integer> stats;

	private boolean spectator;

	public User(Player player) {
		this.player = player;
		this.uuid = player.getUniqueId();
		this.stats = new EnumMap<>(StatsStorage.StatisticType.class);
	}

	public Arena getArena() {
		return ArenaRegistry.getArena(player);
	}

	public Player getPlayer() {
		return player;
	}

	public UUID getUniqueId() {
		return uuid;
	}

	public boolean isSpectator() {
		return spectator;
	}
	
	public void setSpectator(boolean spectating) {
		spectator = spectating;
	}

	public int getStat(StatsStorage.StatisticType statisticType) {
		Integer statistic = stats.get(statisticType);

		if (statistic == null) {
			stats.put(statisticType, 0);
			return 0;
		}

		return statistic;
	}
	
	public void setStat(StatsStorage.StatisticType stat, int value) {
		stats.put(stat, value);

		plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(new OITCPlayerStatisticChangeEvent(getArena(), player, stat, value)));
	}

	public void addStat(StatsStorage.StatisticType stat, int value) {
		setStat(stat, getStat(stat) + value);
	}

	public void removeGameItem(final String id) {
		final GameItem gameItem = plugin.getGameItemManager().getGameItem(id);

		if (gameItem == null) return;

		this.player.getInventory().setItem(gameItem.getSlot(), null);
	}

	public void addGameItems(final String... ids) {
		this.player.getInventory().clear();

		for (final String id : ids) {
			this.addGameItem(id);
		}

		this.player.updateInventory();
	}

	public void addGameItem(final String id) {
		final GameItem gameItem = plugin.getGameItemManager().getGameItem(id);

		if (gameItem == null) return;

		this.player.getInventory().setItem(gameItem.getSlot(), gameItem.getItemStack());
	}

	public void resetStats() {
		for (StatsStorage.StatisticType statistic : StatsStorage.StatisticType.values()) {
			if (statistic.isPersistent()) continue;

			setStat(statistic, 0);
		}
	}
}