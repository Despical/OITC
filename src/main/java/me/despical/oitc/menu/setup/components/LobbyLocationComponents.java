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

package me.despical.oitc.menu.setup.components;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.item.ItemBuilder;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.PaginatedPane;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.menu.setup.AbstractComponent;
import me.despical.oitc.menu.setup.ArenaEditorMenu;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class LobbyLocationComponents extends AbstractComponent {

	public LobbyLocationComponents(ArenaEditorMenu menu) {
		super(menu);
	}

	@Override
	public void registerComponents(PaginatedPane paginatedPane) {
		final StaticPane pane = new StaticPane(9, 3);
		final FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
		final boolean backgroundDone = isOptionDoneBoolean("lobbyLocation", config) && isOptionDoneBoolean("endLocation", config);

		final ItemBuilder backgroundItem = backgroundDone ?
			new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE).name("&aGame locations set properly!") :
			new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&cSet game locations properly!");

		final ItemBuilder endLocationItem = new ItemBuilder(XMaterial.ORANGE_TERRACOTTA)
			.name("&e&l      Set Ending Location")
			.lore("&7Click to set the ending location on")
			.lore("&7the place where you are standing.")
			.lore("", isOptionDoneBool("endLocation", config));

		final ItemBuilder lobbyLocationItem = new ItemBuilder(XMaterial.CYAN_TERRACOTTA)
			.name("&e&l      Set Lobby Location")
			.lore("&7Click to set lobby location on the")
			.lore("&7place where you are standing.")
			.lore("", isOptionDoneBool("lobbyLocation", config));

		pane.fillWith(backgroundItem.build(), event -> event.setCancelled(true));
		pane.addItem(GuiItem.of(mainMenuItem, event -> this.gui.restorePage()), 8, 2);
		pane.addItem(this.buildPinnedItem(user, 1), 0, 0);

		pane.addItem(GuiItem.of(lobbyLocationItem.build(), event -> {
			final Location location = user.getLocation();

			if (!event.isShiftClick()) user.closeOpenedInventory();

			config.set(path + "lobbyLocation", LocationSerializer.toString(location));
			ConfigUtils.saveConfig(plugin, config, "arenas");

			arena.setLobbyLocation(location);

			user.sendRawMessage("&e✔ Completed | &aLobby location for arena &e{0} &aset at your location!", arena.getId());
		}), 3, 1);

		pane.addItem(GuiItem.of(endLocationItem.build(), event -> {
			final Location location = user.getLocation();

			if (!event.isShiftClick()) user.closeOpenedInventory();

			config.set(path + "endLocation", LocationSerializer.toString(location));
			ConfigUtils.saveConfig(plugin, config, "arenas");

			arena.setEndLocation(location);

			user.sendRawMessage("&e✔ Completed | &aEnding location for arena &e{0} &aset at your location!", arena.getId());
		}), 5, 1);

		paginatedPane.addPane(1, pane);
	}
}