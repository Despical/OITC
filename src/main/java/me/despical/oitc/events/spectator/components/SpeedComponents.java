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

import me.despical.commonsbox.compat.XMaterial;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.Main;
import me.despical.oitc.events.spectator.SpectatorSettingsMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * @author Despical
 * <p>
 * Created at 03.10.2020
 */
public class SpeedComponents implements SpectatorSettingComponent {

	@Override
	public void registerComponent(SpectatorSettingsMenu spectatorSettingsMenu, StaticPane pane) {
		Main plugin = spectatorSettingsMenu.getPlugin();
		Player player = spectatorSettingsMenu.getPlayer();
		String speedPrefix = plugin.getChatManager().colorMessage("In-Game.Spectator.Settings-Menu.Speed-Name");

		pane.addItem(new GuiItem(new ItemBuilder(Material.LEATHER_BOOTS)
			.name(plugin.getChatManager().colorMessage("In-Game.Spectator.Settings-Menu.No-Speed"))
			.build(), e -> {
				player.closeInventory();
				player.removePotionEffect(PotionEffectType.SPEED);
				player.setFlySpeed(0.1f);
		}),2,1);

		pane.addItem(new GuiItem(new ItemBuilder(Material.CHAINMAIL_BOOTS)
			.name(speedPrefix + " I")
			.build(), e -> {
				player.closeInventory();
				player.removePotionEffect(PotionEffectType.SPEED);
				player.setFlySpeed(0.2f);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
		}),3,1);

		pane.addItem(new GuiItem(new ItemBuilder(Material.IRON_BOOTS)
			.name(speedPrefix + " II")
			.build(), e -> {
				player.closeInventory();
				player.removePotionEffect(PotionEffectType.SPEED);
				player.setFlySpeed(0.25f);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false));
		}),4,1);

		pane.addItem(new GuiItem(new ItemBuilder(XMaterial.GOLDEN_BOOTS.parseMaterial())
			.name(speedPrefix + " III")
			.build(), e -> {
				player.closeInventory();
				player.removePotionEffect(PotionEffectType.SPEED);
				player.setFlySpeed(0.3f);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3, false, false));
		}),5,1);

		pane.addItem(new GuiItem(new ItemBuilder(Material.DIAMOND_BOOTS)
			.name(speedPrefix + " IV")
			.build(), e -> {
				player.closeInventory();
				player.removePotionEffect(PotionEffectType.SPEED);
				player.setFlySpeed(0.35f);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 4, false, false));
		}),6,1);
	}
}
