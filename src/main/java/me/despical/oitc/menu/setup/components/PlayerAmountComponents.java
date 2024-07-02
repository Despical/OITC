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
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.PaginatedPane;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.menu.Page;
import me.despical.oitc.menu.setup.AbstractComponent;
import me.despical.oitc.menu.setup.ArenaEditorMenu;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class PlayerAmountComponents extends AbstractComponent {

	public PlayerAmountComponents(ArenaEditorMenu menu) {
		super(menu);
	}

	@Override
	public void registerComponents(PaginatedPane paginatedPane) {
		final StaticPane pane = new StaticPane(9, 3);
		final FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");

		final ItemBuilder backgroundItem = new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE).name("&aSet min/max player amounts!");
		final ItemBuilder minPlayersItem = new ItemBuilder(XMaterial.GLOWSTONE_DUST)
			.name("&e&l       Set Minimum Players")
			.lore("&8• &7LEFT  click to increase")
			.lore("&8• &7RIGHT click to decrease", "")
			.lore("&8• &7How many players are needed for")
			.lore("&7game to start the lobby countdown.")
			.lore("", isOptionDone("minimumPlayers", config))
			.amount(minValueHigherThan("minimumPlayers", 2, config));

		final ItemBuilder maxPlayersItem = new ItemBuilder(XMaterial.GLOWSTONE_DUST)
			.name("&e&l      Set Maximum Players")
			.lore("&8• &7LEFT  click to increase")
			.lore("&8• &7RIGHT click to decrease", "")
			.lore("&8• &7Maximum player amount that arena", "&7can hold.")
			.lore("", isOptionDone("maximumPlayers", config))
			.amount(minValueHigherThan("maximumPlayers", arena.getMinimumPlayers(), config));

		pane.fillWith(backgroundItem.build(), event -> event.setCancelled(true));
		pane.addItem(GuiItem.of(mainMenuItem, event -> this.gui.restorePage()), 8, 2);
		pane.addItem(buildPinnedItem(user, 3), 0, 0);

		pane.addItem(GuiItem.of(minPlayersItem.build(), event -> {
			int amount = event.getCurrentItem().getAmount();
			ItemStack item = event.getCurrentItem();
			ClickType click = event.getClick();

			item.setAmount(click.isRightClick() ? --amount : click.isLeftClick() ? ++amount : amount);

			if (event.getCurrentItem().getAmount() < 2) {
				user.sendRawMessage("&c&l✘ Minimum player amount cannot be less than 2!");

				amount = 2;
				item.setAmount(2);
			}

			if (item.getAmount() > arena.getMaximumPlayers()) {
				user.sendRawMessage("&c&l✘ Minimum player amount cannot be higher than maximum players amount! Setting both as the same value!");

				arena.setMaximumPlayers(amount);

				config.set(path + "maximumPlayers", amount);

				item.setAmount(amount);
			}

			arena.setMinimumPlayers(amount);
			arena.updateSigns();

			config.set(path + "minimumPlayers", amount);
			ConfigUtils.saveConfig(plugin, config, "arenas");

			new ArenaEditorMenu(plugin, user, arena).showGuiFromPage(new Page(arena, " Set MIN and MAX player amount", 3, 2));
		}), 3, 1);

		pane.addItem(GuiItem.of(maxPlayersItem.build(), event -> {
			ItemStack item = event.getCurrentItem();
			int amount = item.getAmount();
			ClickType click = event.getClick();

			item.setAmount(click.isRightClick() ? --amount : click.isLeftClick() ? ++amount : amount);

			if (item.getAmount() < 2) {
				user.sendRawMessage("&c&l✘ Maximum player amount cannot be less than 2!");

				amount = 2;
				item.setAmount(2);
			} else if (item.getAmount() < arena.getMinimumPlayers()) {
				user.sendRawMessage("&c&l✘ Maximum player amount cannot be less than minimum player amount! Setting both as the same value!");

				arena.setMinimumPlayers(amount);

				config.set(path + "minimumPlayers", amount);

				item.setAmount(amount);
			}

			arena.setMaximumPlayers(amount);
			arena.updateSigns();

			config.set(path + "maximumPlayers", amount);
			ConfigUtils.saveConfig(plugin, config, "arenas");

			new ArenaEditorMenu(plugin, user, arena).showGuiFromPage(new Page(arena, " Set MIN and MAX player amount", 3, 2));
		}), 5, 1);

		paginatedPane.addPane(2, pane);
	}
}