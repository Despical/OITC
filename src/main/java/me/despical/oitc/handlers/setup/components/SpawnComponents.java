package me.despical.oitc.handlers.setup.components;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;

import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.commonsbox.item.ItemBuilder;
import me.despical.commonsbox.serializer.LocationSerializer;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.handlers.setup.SetupInventory;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SpawnComponents implements SetupComponent {

	private SetupInventory setupInventory;

	@Override
	public void prepare(SetupInventory setupInventory) {
		this.setupInventory = setupInventory;
	}

	@Override
	public void injectComponents(StaticPane pane) {
		Player player = setupInventory.getPlayer();
		FileConfiguration config = setupInventory.getConfig();
		Arena arena = setupInventory.getArena();
		Main plugin = setupInventory.getPlugin();
		String serializedLocation = LocationSerializer.locationToString(player.getLocation());
		pane.addItem(new GuiItem(new ItemBuilder(Material.REDSTONE_BLOCK)
			.name(plugin.getChatManager().colorRawMessage("&e&lSet Ending Location"))
			.lore(ChatColor.GRAY + "Click to set the ending location")
			.lore(ChatColor.GRAY + "on the place where you are standing.")
			.lore(ChatColor.DARK_GRAY + "(location where players will be")
			.lore(ChatColor.DARK_GRAY + "teleported after the game)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool("instances." + arena.getId() + ".Endlocation"))
			.build(), e -> {
				e.getWhoClicked().closeInventory();
				config.set("instances." + arena.getId() + ".Endlocation", serializedLocation);
				arena.setEndLocation(player.getLocation());
				player.sendMessage(plugin.getChatManager().colorRawMessage("&e✔ Completed | &aEnding location for arena " + arena.getId() + " set at your location!"));
				ConfigUtils.saveConfig(plugin, config, "arenas");
			}), 0, 0);

		pane.addItem(new GuiItem(new ItemBuilder(Material.LAPIS_BLOCK)
			.name(plugin.getChatManager().colorRawMessage("&e&lSet Lobby Location"))
			.lore(ChatColor.GRAY + "Click to set the lobby location")
			.lore(ChatColor.GRAY + "on the place where you are standing")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneBool("instances." + arena.getId() + ".lobbylocation"))
			.build(), e -> {
				e.getWhoClicked().closeInventory();
				config.set("instances." + arena.getId() + ".lobbylocation", serializedLocation);
				arena.setLobbyLocation(player.getLocation());
				player.sendMessage(plugin.getChatManager().colorRawMessage("&e✔ Completed | &aLobby location for arena " + arena.getId() + " set at your location!"));
				ConfigUtils.saveConfig(plugin, config, "arenas");
			}), 1, 0);

		pane.addItem(new GuiItem(new ItemBuilder(Material.EMERALD_BLOCK)
			.name(plugin.getChatManager().colorRawMessage("&e&lAdd Starting Location"))
			.lore(ChatColor.GRAY + "Click to add the starting location")
			.lore(ChatColor.GRAY + "on the place where you are standing.")
			.lore(ChatColor.DARK_GRAY + "(locations where players will be")
			.lore(ChatColor.DARK_GRAY + "teleported when game starts)")
			.lore("", setupInventory.getSetupUtilities().isOptionDoneList("instances." + arena.getId() + ".playerspawnpoints", arena.getMaximumPlayers()))
			.lore("", plugin.getChatManager().colorRawMessage("&8Shift + Right Click to remove all spawns"))
			.build(), e -> {
				e.getWhoClicked().closeInventory();
				if (e.getClick() == ClickType.SHIFT_RIGHT) {
					config.set("instances." + arena.getId() + ".playerspawnpoints", new ArrayList<>());
					arena.setPlayerSpawnPoints(new ArrayList<>());
					player.sendMessage(plugin.getChatManager().colorRawMessage("&eDone | &aPlayer spawn points deleted, you can add them again now!"));
					arena.setReady(false);
					ConfigUtils.saveConfig(plugin, config, "arenas");
					return;
				}
				List<String> startingSpawns = config.getStringList("instances." + arena.getId() + ".playerspawnpoints");
				startingSpawns.add(LocationSerializer.locationToString(player.getLocation()));
				config.set("instances." + arena.getId() + ".playerspawnpoints", startingSpawns);
				String startingProgress = startingSpawns.size() >= arena.getMaximumPlayers() ? "&e✔ Completed | " : "&c✘ Not completed | ";
				player.sendMessage(plugin.getChatManager().colorRawMessage(
				startingProgress + "&aPlayer spawn added! &8(&7" + startingSpawns.size() + "/" + arena.getMaximumPlayers() + "&8)"));
				if (startingSpawns.size() == arena.getMaximumPlayers()) {
					player.sendMessage(plugin.getChatManager().colorRawMessage("&eInfo | &aYou can add more than " + arena.getMaximumPlayers() + " player spawns! " + arena.getMaximumPlayers() + " is just a minimum!"));
				}
				List<Location> spawns = new ArrayList<>(arena.getPlayerSpawnPoints());
				spawns.add(player.getLocation());
				arena.setPlayerSpawnPoints(spawns);
				ConfigUtils.saveConfig(plugin, config, "arenas");
			}), 2, 0);
	}
}