/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2024 Despical
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

import me.despical.commons.compat.Titles;
import me.despical.commons.miscellaneous.AttributeUtils;
import me.despical.commons.reflection.XReflection;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.Main;
import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.api.events.player.OITCPlayerStatisticChangeEvent;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.handlers.items.GameItem;
import me.despical.oitc.handlers.rewards.Reward;
import me.despical.oitc.menu.Page;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.text.MessageFormat;
import java.util.*;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class User {

	private static final Main plugin = JavaPlugin.getPlugin(Main.class);
	private static long cooldownCounter = 0;

	private final UUID uuid;
	private final String name;
	private final Map<String, Double> cooldowns;
	private final Map<StatsStorage.StatisticType, Integer> stats;

	private Page pinnedPage;
	private boolean spectator;
	private double attackCooldown;
	private Scoreboard cachedScoreboard;

	User(Player player) {
		this.uuid = player.getUniqueId();
		this.name = player.getName();
		this.pinnedPage = new Page(null, "", 0, 0);
		this.cooldowns = new HashMap<>();
		this.stats = new EnumMap<>(StatsStorage.StatisticType.class);
	}

	public Arena getArena() {
		return plugin.getArenaRegistry().getArena(getPlayer());
	}

	public Player getPlayer() {
		return plugin.getServer().getPlayer(uuid);
	}

	public String getName() {
		return this.name;
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
		return stats.computeIfAbsent(statisticType, stat -> 0);
	}
	
	public void setStat(StatsStorage.StatisticType stat, int value) {
		stats.put(stat, value);

		plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(new OITCPlayerStatisticChangeEvent(getArena(), getPlayer(), stat, value)));
	}

	public void addStat(StatsStorage.StatisticType stat, int value) {
		setStat(stat, getStat(stat) + value);
	}

	public void addGameItems(final String... ids) {
		this.getPlayer().getInventory().clear();

		for (final String id : ids) {
			this.addGameItem(id);
		}

		this.getPlayer().updateInventory();
	}

	public void addGameItem(final String id) {
		final GameItem gameItem = plugin.getGameItemManager().getGameItem(id);

		if (gameItem == null) return;

		this.getPlayer().getInventory().setItem(gameItem.getSlot(), gameItem.getItemStack());
	}

	public void resetStats() {
		for (StatsStorage.StatisticType statistic : StatsStorage.StatisticType.values()) {
			if (statistic.isPersistent()) continue;

			setStat(statistic, 0);
		}
	}

	public void performReward(Reward.RewardType rewardType) {
		plugin.getRewardsFactory().performReward(this, rewardType);
	}

	public void heal() {
		if (plugin.getOption(ConfigPreferences.Option.HEAL_PLAYER)) AttributeUtils.healPlayer(getPlayer());
	}

	public void cacheScoreboard() {
		this.cachedScoreboard = getPlayer().getScoreboard();
	}

	public void removeScoreboard() {
		if (cachedScoreboard != null) {
			getPlayer().setScoreboard(cachedScoreboard);

			cachedScoreboard = null;
		}
	}

	public void sendTitle(String title, String subTitle) {
		Titles.sendTitle(this.getPlayer(), 10, 40, 10, title, subTitle);
	}

	public void updateAttackCooldown() {
		if (!XReflection.supports(9)) return;

		Player player = this.getPlayer();

		if (player == null) return;

		Optional.ofNullable(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).ifPresent(attribute -> {
			this.attackCooldown = attribute.getBaseValue();

			attribute.setBaseValue(plugin.getConfig().getDouble("Hit-Cooldown-Delay", 20));
		});
	}

	public void resetAttackCooldown() {
		if (!XReflection.supports(9)) return;

		Player player = this.getPlayer();

		if (player == null) return;

		Optional.ofNullable(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).ifPresent(attribute -> {
			if (attackCooldown == 0) {
				attackCooldown = attribute.getDefaultValue();
			}

			attribute.setBaseValue(attackCooldown);
		});
	}

	public void setPinnedPage(final Page pinnedPage) {
		this.pinnedPage = pinnedPage;
	}

	public Page getPinnedPage() {
		return pinnedPage;
	}

	public Location getLocation() {
		return this.getPlayer().getLocation();
	}

	public void closeOpenedInventory() {
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> getPlayer().closeInventory(), 1L);
	}

	public void sendMessage(final String path) {
		Optional.ofNullable(this.getPlayer()).ifPresent(player -> player.sendMessage(plugin.getChatManager().prefixedMessage(path, player)));
	}

	public void sendRawMessage(final String message) {
		this.getPlayer().sendMessage(plugin.getChatManager().coloredRawMessage(message));
	}

	public void sendRawMessage(final String message, final Object... args) {
		this.getPlayer().sendMessage(plugin.getChatManager().coloredRawMessage(MessageFormat.format(message, args)));
	}

	public void setCooldown(String s, double seconds) {
		cooldowns.put(s, seconds + cooldownCounter);
	}

	public double getCooldown(String s) {
		final Double cooldown = cooldowns.get(s);

		return (cooldown == null || cooldown <= cooldownCounter) ? 0 : cooldown - cooldownCounter;
	}

	public static void cooldownHandlerTask() {
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> cooldownCounter++, 20, 20);
	}
}