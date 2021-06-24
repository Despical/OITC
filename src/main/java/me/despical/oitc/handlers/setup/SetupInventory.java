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

package me.despical.oitc.handlers.setup;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.inventoryframework.Gui;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.handlers.ChatManager;
import me.despical.oitc.handlers.setup.components.ArenaRegisterComponent;
import me.despical.oitc.handlers.setup.components.MiscComponents;
import me.despical.oitc.handlers.setup.components.PlayerAmountComponents;
import me.despical.oitc.handlers.setup.components.SpawnComponents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SetupInventory {

	private final Main plugin = JavaPlugin.getPlugin(Main.class);
	private final FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
	private final Arena arena;
	private final Player player;
	private Gui gui;
	private final SetupUtilities setupUtilities;

	public SetupInventory(Arena arena, Player player) {
		this.arena = arena;
		this.player = player;
		this.setupUtilities = new SetupUtilities(config, arena);

		prepareGui();
	}

	private void prepareGui() {
		gui = new Gui(plugin, 5, "OITC Arena Editor");
		gui.setOnGlobalClick(e -> e.setCancelled(true));

		StaticPane pane = new StaticPane(9, 5);
		pane.fillProgressBorder(GuiItem.of(XMaterial.GREEN_STAINED_GLASS_PANE.parseItem()), GuiItem.of(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem()), arena.isReady() ? 100 : 0);

		gui.addPane(pane);

		prepareComponents(pane);
	}


	private void prepareComponents(StaticPane pane) {
		SpawnComponents spawnComponents = new SpawnComponents();
		spawnComponents.registerComponent(this, pane);

		PlayerAmountComponents playerAmountComponents = new PlayerAmountComponents();
		playerAmountComponents.registerComponent(this, pane);

		MiscComponents miscComponents = new MiscComponents();
		miscComponents.registerComponent(this, pane);

		ArenaRegisterComponent arenaRegisterComponent = new ArenaRegisterComponent();
		arenaRegisterComponent.registerComponent(this, pane);
	}

	private void sendProTip(Player player) {
		ChatManager chatManager = plugin.getChatManager();

		switch (ThreadLocalRandom.current().nextInt(16 + 1)) {
			case 0:
				player.sendMessage(chatManager.coloredRawMessage("&e&lTIP: &7We are open source! You can always help us by contributing! Check https://github.com/Despical/OITC"));
				break;
			case 1:
				player.sendMessage(chatManager.coloredRawMessage("&e&lTIP: &7Need help? Join our discord server: https://discordapp.com/invite/Vhyy4HA"));
				break;
			case 2:
				player.sendMessage(chatManager.coloredRawMessage("&e&lTIP: &7Need help? Check our wiki: https://github.com/Despical/OITC/wiki"));
				break;
			case 3:
				player.sendMessage(chatManager.coloredRawMessage("&e&lTIP: &7Help us translating plugin to your language here: https://github.com/Despical/LocaleStorage"));
				break;
			default:
				break;
		}
	}

	public void openInventory() {
		sendProTip(player);
		gui.show(player);
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public Arena getArena() {
		return arena;
	}

	public Player getPlayer() {
		return player;
	}

	public SetupUtilities getSetupUtilities() {
		return setupUtilities;
	}
}