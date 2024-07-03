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

package me.despical.oitc.menu.setup;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.item.ItemBuilder;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.inventoryframework.Gui;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.PaginatedPane;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.menu.AbstractMenu;
import me.despical.oitc.menu.Page;
import me.despical.oitc.user.User;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public abstract class AbstractComponent {

	protected static final ItemStack mainMenuItem = new ItemBuilder(XMaterial.REDSTONE).name("&c&lReturn Main Menu").build();

	protected final AbstractMenu gui;
	protected final User user;
	protected final String path;
	protected final Arena arena;
	protected final Main plugin;

	public AbstractComponent(final AbstractMenu gui) {
		this.gui = gui;
		this.user = gui.getUser();
		this.arena = gui.getArena();
		this.path = String.format("instances.%s.", gui.getArena().getId());
		this.plugin = gui.getPlugin();
	}

	public abstract void registerComponents(PaginatedPane paginatedPane);

	protected String isOptionDone(String path, FileConfiguration config) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		return config.isSet(path) ? "&a✔ &lCompleted &7(value: &8" + config.getString(path) + "&7)" : "&c✘ &lNot Completed";
	}

	protected String isOptionDoneList(String path, int minimum, FileConfiguration config) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		if (config.isSet(path)) {
			int size = config.getStringList(path).size();

			return size < minimum ? "&c✘ &lNot Completed &c| &lAdd more spawns" : "&a✔ &lCompleted &7(value: &8" + size + "&7)";
		}

		return "&c✘ &lNot Completed";
	}

	protected boolean isOptionDoneListBoolean(String path, int minimum, FileConfiguration config) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		return config.isSet(path) && config.getStringList(path).size() >= minimum;
	}

	protected String isOptionDoneBool(String path, FileConfiguration config) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		return config.isSet(path) ? LocationSerializer.isDefaultLocation(config.getString(path)) ? "&c✘ &lNot Completed" : "&a✔ &lCompleted" : "&c✘ &lNot Completed";
	}

	protected boolean isOptionDoneBoolean(String path, FileConfiguration config) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		return config.isSet(path) && !LocationSerializer.isDefaultLocation(config.getString(path));
	}

	protected int minValueHigherThan(String path, int higher, FileConfiguration config) {
		path = String.format("instances.%s.%s", arena.getId(), path);

		return Math.max(higher, config.getInt(path));
	}

	protected GuiItem buildPinnedItem(final User user, final int page) {
		final Page pinnedPage = user.getPinnedPage();
		final boolean isPinnedPage = pinnedPage.getPage() == page;
		final Arena arena = gui.getArena();
		final ItemBuilder pinnedItem = new ItemBuilder(isPinnedPage ? XMaterial.ENDER_EYE : XMaterial.ENDER_PEARL).name(isPinnedPage ? "          &c&lUnpin This Page" : "           &c&lPin This Page").lore(" &7When you open the arena editor", "&7&nlast pinned page&r&7 will be displayed.").flag(ItemFlag.HIDE_ENCHANTS);

		return GuiItem.of(pinnedItem.build(), event -> {
			final Gui menu = gui.getGui();

			user.setPinnedPage(new Page(arena, menu.getTitle(), menu.getRows(), isPinnedPage ? 0 : page));

			new ArenaEditorMenu(plugin, user, arena).showGuiFromPage(new Page(arena, menu.getTitle(), menu.getRows(), page));
		});
	}
}