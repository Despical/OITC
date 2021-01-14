/*
 * OITC - Kill your opponents and reach 25 points to win!
 * Copyright (C) 2021 Despical and contributors
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

package me.despical.oitc.user.data;

import me.despical.oitc.api.StatsStorage;
import me.despical.oitc.user.User;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public interface UserDatabase {

	/**
	 * Saves player statistic into yaml or MySQL storage based on user choice
	 *
	 * @param user user to retrieve statistic from
	 * @param stat stat to save to storage
	 */
	void saveStatistic(User user, StatsStorage.StatisticType stat);

	/**
	 * Saves player statistic into yaml or MySQL storage based on user choice
	 *
	 * @param user user to retrieve statistic from
	 */
	void saveAllStatistic(User user);

	/**
	 * Loads player statistic from yaml or MySQL storage based on user choice
	 *
	 * @param user user to load statistic for
	 */
	void loadStatistics(User user);

}