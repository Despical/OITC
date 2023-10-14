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

import me.despical.oitc.Main;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class Reward {

	private final RewardType type;
	private final List<SubReward> rewards;

	public Reward(final Main plugin, final RewardType type, final List<String> rawCodes) {
		this.type = type;
		this.rewards = new ArrayList<>();

		for (final String rawCode : rawCodes) {
			this.rewards.add(new SubReward(plugin, rawCode));
		}
	}

	public List<SubReward> getRewards() {
		return rewards;
	}

	public RewardType getType() {
		return type;
	}

	public final static class SubReward {

		private String executableCode;
		private final int chance, executor;

		public SubReward(final Main plugin, final String rawCode) {
			String processedCode = rawCode;

			if (rawCode.contains("p:")) {
				this.executor = 2;

				processedCode = processedCode.replace("p:", "");
			} else if (rawCode.contains("script:")) {
				this.executor = 3;

				processedCode = processedCode.replace("script:", "");
			} else {
				this.executor = 1;
			}

			if (processedCode.contains("chance(")) {
				int loc = processedCode.indexOf(")");

				if (loc == -1) {
					plugin.getLogger().warning(String.format("Second '')'' is not found in chance condition! Command: %s", rawCode));

					this.chance = 101;
					return;
				}

				String chanceStr = processedCode;
				chanceStr = chanceStr.substring(0, loc).replaceAll("[^0-9]+", "");

				processedCode = processedCode.replace(String.format("chance(%s):", chanceStr), "");

				this.chance = Integer.parseInt(chanceStr);
			} else {
				this.chance = 100;
			}

			this.executableCode = processedCode;
		}

		public String getExecutableCode() {
			return executableCode;
		}

		public int getExecutor() {
			return executor;
		}

		public int getChance() {
			return chance;
		}
	}

	public enum RewardType {

		KILL("kill"),
		DEATH("death"),
		END_GAME("endgame"),
		LOSE("lose"),
		WIN("win");

		final String path;

		RewardType(String path) {
			this.path = "rewards." + path;
		}
	}
}