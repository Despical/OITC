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

package me.despical.oitc.handlers.rewards;

import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.engine.ScriptEngine;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.user.User;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class RewardsFactory {

	private final Main plugin;
	private final Set<Reward> rewards;

	public RewardsFactory(final Main plugin) {
		this.plugin = plugin;
		this.rewards = new HashSet<>();

		registerRewards();
	}

	public void performReward(final User user, final Reward.RewardType type) {
		final List<Reward> rewardList = rewards.stream().filter(rew -> rew.getType() == type).collect(Collectors.toList());

		if (rewardList.isEmpty()) return;

		for (final Reward mainRewards : rewardList) {
			for (final Reward.SubReward reward : mainRewards.getRewards()){
				if (ThreadLocalRandom.current().nextInt(0, 100) > reward.getChance()) continue;

				final Arena arena = user.getArena();
				final Player player = user.getPlayer();
				final String command = formatCommandPlaceholders(reward, user);

				switch (reward.getExecutor()) {
					case 1:
						plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
						break;
					case 2:
						player.performCommand(command);
						break;
					case 3:
						final ScriptEngine engine = new ScriptEngine();
						engine.setValue("player", player);
						engine.setValue("server", plugin.getServer());
						engine.setValue("arena", arena);
						engine.execute(command);
				}
			}
		}
	}

	private String formatCommandPlaceholders(final Reward.SubReward reward, final User user) {
		Arena arena = user.getArena();
		String formatted = reward.getExecutableCode();

		formatted = formatted.replace("%arena%", arena.getId());
		formatted = formatted.replace("%map_name%", arena.getMapName());
		formatted = formatted.replace("%player%", user.getName());
		formatted = formatted.replace("%players%", Integer.toString(arena.getPlayers().size()));
		return formatted;
	}

	private void registerRewards() {
		final FileConfiguration config = ConfigUtils.getConfig(plugin, "rewards");

		if (!config.getBoolean("Rewards-Enabled")) return;

		for (final Reward.RewardType rewardType : Reward.RewardType.values()) {
			rewards.add(new Reward(plugin, rewardType, config.getStringList(rewardType.path)));
		}
	}
}