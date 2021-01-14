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

package me.despical.oitc.events.spectator.components;

import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.events.spectator.SpectatorSettingsMenu;

/**
 * @author Despical
 * <p>
 * Created at 03.10.2020
 */
public interface SpectatorSettingComponent {

	void registerComponent(SpectatorSettingsMenu spectatorSettingsMenu, StaticPane pane);
}