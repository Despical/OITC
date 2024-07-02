package me.despical.oitc.menu.setup.components;

import me.despical.commons.compat.XMaterial;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.item.ItemBuilder;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.commons.util.Strings;
import me.despical.commons.util.conversation.ConversationBuilder;
import me.despical.inventoryframework.GuiItem;
import me.despical.inventoryframework.pane.PaginatedPane;
import me.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.ConfigPreferences;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.handlers.sign.ArenaSign;
import me.despical.oitc.handlers.sign.SignManager;
import me.despical.oitc.menu.setup.AbstractComponent;
import me.despical.oitc.menu.setup.ArenaEditorMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 2.07.2024
 */
public class MainMenuComponents extends AbstractComponent {

	public MainMenuComponents(ArenaEditorMenu menu) {
		super(menu);
	}

	@Override
	public void registerComponents(PaginatedPane paginatedPane) {
		final StaticPane pane = new StaticPane(9, 5);
		final FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
		final ItemBuilder readyItem = new ItemBuilder(XMaterial.LIME_STAINED_GLASS_PANE).name("&aArena is registered properly!");
		final ItemBuilder notReadyItem = new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).name("&cArena configuration is not validated yet!");
		final ItemBuilder lobbyLocationsItem = new ItemBuilder(XMaterial.WHITE_TERRACOTTA).name("    &e&lSet Lobby/End Locations").lore("&7Click to set start and end locations.").lore("", "&7Lobby Location: " + isOptionDoneBool("lobbyLocation", config), "&7End Location:    " + isOptionDoneBool("endLocation", config));
		final ItemBuilder spawnPointsItem = new ItemBuilder(XMaterial.BLACK_TERRACOTTA).name("&e&l       Add Player Spawn Point").lore("&7      Click to add player spawn point.").lore("", isOptionDoneList("playersSpawnPoints", arena.getMaximumPlayers(), config));
		final ItemBuilder playerAmountsItem = new ItemBuilder(XMaterial.GLOWSTONE_DUST).name("   &e&lSet Min/Max Players").lore(" &7Click to set player amounts.").lore("", "&a✔ &7Minimum  Players Amount: &8" + arena.getMinimumPlayers()).lore("&a✔ &7Maximum Players Amount: &8" + arena.getMaximumPlayers()).enchantment(Enchantment.ARROW_INFINITE).flag(ItemFlag.HIDE_ENCHANTS);
		final ItemBuilder mapNameItem = new ItemBuilder(XMaterial.NAME_TAG).name("    &e&lSet Map Name").lore("&7Click to set map name.").lore("", "&7Currently: " + arena.getMapName()).enchantment(Enchantment.ARROW_INFINITE).flag(ItemFlag.HIDE_ENCHANTS);

		if (isOptionDoneBoolean("lobbyLocation", config) && isOptionDoneBoolean("endLocation", config))
			lobbyLocationsItem.enchantment(Enchantment.ARROW_INFINITE).flag(ItemFlag.HIDE_ENCHANTS);

		if (isOptionDoneListBoolean("playerSpawnPoints", arena.getMaximumPlayers(), config))
			spawnPointsItem.enchantment(Enchantment.ARROW_INFINITE).flag(ItemFlag.HIDE_ENCHANTS);

		pane.fillWith(arena.isReady() ? readyItem.build() : notReadyItem.build());
		pane.fillProgressBorder(GuiItem.of(readyItem.build()), GuiItem.of(notReadyItem.build()), arena.isReady() ? 100 : 0);
		pane.addItem(GuiItem.of(lobbyLocationsItem.build(), event -> this.gui.setPage("   Set LOBBY and END locations", 3, 1)), 1, 1);
		pane.addItem(GuiItem.of(playerAmountsItem.build(), event -> this.gui.setPage(" Set MIN and MAX player amount", 3, 2)), 5, 1);

		pane.addItem(GuiItem.of(mapNameItem.build(), event -> {
			user.closeOpenedInventory();

			new ConversationBuilder(plugin).withPrompt(new StringPrompt() {

				@Override
				@NotNull
				public String getPromptText(@NotNull ConversationContext context) {
					return Strings.format("&ePlease type the map name of arena in the chat. You can use color codes.");
				}

				@Override
				public Prompt acceptInput(@NotNull ConversationContext context, String input) {
					String name = Strings.format(input);

					arena.setMapName(name);

					config.set(path + "mapName", name);
					ConfigUtils.saveConfig(plugin, config, "arenas");

					plugin.getServer().getScheduler().runTask(plugin, () -> user.sendRawMessage("&e✔ Completed | &aMap name of arena &e{0} &aset to &e{1}&a.", arena.getId(), name));
					return Prompt.END_OF_CONVERSATION;
				}
			}).buildFor(user.getPlayer());
		}), 3, 1);

		ItemBuilder gameSignItem = new ItemBuilder(XMaterial.OAK_SIGN).name("&e&l      Add Game Sign");

		if (!plugin.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
			gameSignItem
				.lore("&7Target a sign and click this.");
		} else {
			gameSignItem
				.lore("&cThis option disabled in Bungee-cord mode.", "")
				.lore("&8Bungee mode is meant to be one arena per server.")
				.lore("&8If you wish to have multi arena, disable bungee in config!");
		}

		pane.addItem(GuiItem.of(gameSignItem.build(), e -> {
			if (plugin.getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) return;

			user.closeOpenedInventory();

			Block block = user.getPlayer().getTargetBlock(null, 10);

			if (!(block.getState() instanceof Sign)) {
				user.sendMessage("Signs.Look-Sign");
				return;
			}

			final SignManager signManager = plugin.getSignManager();

			if (signManager.isGameSign(block)) {
				user.sendMessage("Signs.Already-A-Sign");
				return;
			}

			final String path = String.format("instances.%s.signs", arena.getId());
			final List<String> locations = config.getStringList(path);
			locations.add(LocationSerializer.toString(block.getLocation()));

			config.set(path, locations);
			ConfigUtils.saveConfig(plugin, config, "arenas");

			signManager.addArenaSign(block, arena);
			signManager.updateSign(arena);

			user.sendMessage("Signs.Sign-Created");
		}), 4, 2);

		ItemBuilder playerSpawnPointItem = new ItemBuilder(XMaterial.ARMOR_STAND)
			.name("&e&l       Add Player Spawn Point")
			.lore("&7      Click to add player spawn point.")
			.lore("", isOptionDoneList("playersSpawnPoints", arena.getMaximumPlayers(), config));

		if (!arena.getPlayerSpawnPoints().isEmpty()) {
			playerSpawnPointItem.lore("", "&8• &7Shift + Right Click to remove all spawns.");
		}

		pane.addItem(GuiItem.of(playerSpawnPointItem.build(), event -> {
			user.closeOpenedInventory();

			List<Location> emptyList = new ArrayList<>();

			if (event.getClick() == ClickType.SHIFT_RIGHT) {
				user.sendRawMessage("&e✔ Completed | &aAll the player spawn points removed, you can add them again now!");

				arena.setPlayerSpawnPoints(emptyList);
				arena.setReady(false);

				config.set(path + "playersSpawnPoints", emptyList);
				ConfigUtils.saveConfig(plugin, config, "arenas");
				return;
			}

			List<String> startingSpawns = config.getStringList(path + "playersSpawnPoints");
			startingSpawns.add(LocationSerializer.toString(user.getLocation()));

			config.set(path + "playersSpawnPoints", startingSpawns);

			int startingSpawnsSize = startingSpawns.size();
			int maxPlayers = arena.getMaximumPlayers();
			String progress = startingSpawnsSize >= maxPlayers ? "&e✔ Completed" : "&c✘ Not completed";
			user.sendRawMessage("{0} | &aPlayer spawn added! &8(&7{1}/{2}&8)", progress, startingSpawnsSize, maxPlayers);

			if (startingSpawnsSize == maxPlayers) {
				user.sendRawMessage("&eInfo | &aYou can add more than &e{0} &aplayer spawns! &e{0} &ais just a minimum value!", maxPlayers);
			}

			List<Location> spawns = new ArrayList<>(arena.getPlayerSpawnPoints());
			spawns.add(user.getLocation());

			arena.setPlayerSpawnPoints(spawns);
			ConfigUtils.saveConfig(plugin, config, "arenas");
		}), 7, 1);

		ItemStack registerItem;

		if (arena.isReady()) {
			registerItem = new ItemBuilder(XMaterial.BARRIER)
				.name("&a&l           Arena Registered")
				.lore("&7Good job, you went through whole setup!")
				.lore("&7      You can play on this arena now!")
				.enchantment(Enchantment.DURABILITY)
				.flag(ItemFlag.HIDE_ENCHANTS)
				.build();
		} else {
			registerItem = new ItemBuilder(XMaterial.FIREWORK_ROCKET)
				.name("       &e&lFinish Arena Setup")
				.lore("&7  Click this when you are done.")
				.lore("&7You'll still be able to edit arena.")
				.build();
		}

		pane.addItem(GuiItem.of(registerItem, e -> {
			user.closeOpenedInventory();

			if (arena.isReady()) {
				user.sendRawMessage("&a&l✔ &aThis arena was already validated and is ready to use!");
				return;
			}

			String[] locations = {"lobbyLocation", "endLocation"}, spawns = {"playersSpawnPoints"};

			for (String location : locations) {
				if (!config.isSet(path + location) || LocationSerializer.isDefaultLocation(config.getString(path + location))) {
					user.sendRawMessage("&c&l✘ &cArena validation failed! Please configure following spawn properly: {0} (cannot be world spawn location)", location);
					return;
				}
			}

			for (String spawn : spawns) {
				if (!config.isSet(path + spawn) || config.getStringList(path + spawn).size() < arena.getMaximumPlayers()) {
					user.sendRawMessage("&c&l✘ &cArena validation failed! Please configure following spawns properly: {0} (must be minimum {1} spawns)", spawn, arena.getMaximumPlayers());
					return;
				}
			}

			user.sendRawMessage("&a&l✔ &aValidation succeeded! new arena instance: &e{0}", arena.getId());

			config.set(path + "ready", true);
			ConfigUtils.saveConfig(plugin, config, "arenas");

			arena.setArenaState(ArenaState.WAITING_FOR_PLAYERS);
			arena.setReady(true);
			arena.setPlayerSpawnPoints(config.getStringList(path + "playersSpawnPoints").stream().map(LocationSerializer::fromString).collect(Collectors.toList()));
			arena.setMinimumPlayers(config.getInt(path + "minimumPlayers"));
			arena.setMaximumPlayers(config.getInt(path + "maximumPlayers"));
			arena.setMapName(config.getString(path + "mapName"));
			arena.setLobbyLocation(LocationSerializer.fromString(config.getString(path + "lobbyLocation")));
			arena.setEndLocation(LocationSerializer.fromString(config.getString(path + "endLocation")));
			arena.start();

			plugin.getSignManager().getArenaSigns().stream().filter(arenaSign -> arenaSign.getArena().equals(arena)).map(ArenaSign::getSign)
				.forEach(sign -> plugin.getSignManager().addArenaSign(sign.getBlock(), arena));
		}), 8, 4);

		paginatedPane.addPane(0, pane);
	}
}