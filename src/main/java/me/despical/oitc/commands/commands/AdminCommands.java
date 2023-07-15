package me.despical.oitc.commands.commands;

import me.despical.commandframework.Command;
import me.despical.commandframework.CommandArguments;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.commands.AbstractCommand;
import me.despical.oitc.handlers.setup.SetupInventory;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static me.despical.commandframework.Command.SenderType.PLAYER;

/**
 * @author Despical
 * <p>
 * Created at 3.07.2023
 */
public class AdminCommands extends AbstractCommand {

	public AdminCommands(Main plugin) {
		super(plugin);
	}

	@Command(
		name = "oitc",
		desc = "Main command of One in the Chamber."
	)
	public void mainCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.coloredRawMessage("&3This server is running &bOne in the Chamber " + plugin.getDescription().getVersion() + " &3by &bDespical."));

			if (arguments.hasPermission("oitc.admin")) {
				arguments.sendMessage(chatManager.coloredRawMessage("&3Commands: &b/" + arguments.getLabel() + " help"));
			}
		}
	}

	@Command(
		name = "oitc.create",
		permission = "oitc.admin.create",
		desc = "Create an arena with default configuration.",
		usage = "/oitc create <arena name>",
		senderType = PLAYER
	)
	public void createCommand(CommandArguments arguments) {
		Player player = arguments.getSender();

		if (arguments.isArgumentsEmpty()) {
			player.sendMessage(chatManager.prefixedRawMessage("&cPlease enter an name to create an arena!"));
			return;
		}

		String id = arguments.getArgument(0);

		if (arenaRegistry.isArena(id)) {
			player.sendMessage(chatManager.prefixedRawMessage("&cArena with that ID already contains!"));
			player.sendMessage(chatManager.prefixedRawMessage("&cTo check existing arenas use: /oitc list"));
			return;
		}

		String path = "instances." + id + ".";

		arenaConfig.set(path + "ready", false);
		arenaConfig.set(path + "mapName", id);
		arenaConfig.set(path + "minimumPlayers", 2);
		arenaConfig.set(path + "maximumPlayers", 10);
		arenaConfig.set(path + "lobbyLocation", LocationSerializer.SERIALIZED_LOCATION);
		arenaConfig.set(path + "endLocation", LocationSerializer.SERIALIZED_LOCATION);
		arenaConfig.set(path + "playersSpawnPoints", new ArrayList<>());
		arenaConfig.set(path + "signs", new ArrayList<>());

		saveConfig();

		Arena arena = new Arena(id);
		arena.setMapName(id);
		arena.setLobbyLocation(LocationSerializer.DEFAULT_LOCATION);
		arena.setEndLocation(LocationSerializer.DEFAULT_LOCATION);

		arenaRegistry.registerArena(arena);

		player.sendMessage(chatManager.coloredRawMessage("&l--------------------------------------------"));
		MiscUtils.sendCenteredMessage(player, "&eInstance &a&l" + id + " &ecreated!");
		player.sendMessage("");
		MiscUtils.sendCenteredMessage(player, "&aEdit this arena via /oitc edit &6" + id + "&a!");
		player.sendMessage("");
		MiscUtils.sendCenteredMessage(player, "&6Don't know where to start? Check out our wiki:");
		MiscUtils.sendCenteredMessage(player, "&7https://www.github.com/Despical/OITC/wiki");
		player.sendMessage(chatManager.coloredRawMessage("&l--------------------------------------------"));
	}

	@Command(
		name = "oitc.delete",
		permission = "oitc.admin.delete",
		desc = "Delete specified arena and its data",
		usage = "/oitc delete <arena name>",
		senderType = PLAYER
	)
	public void deleteCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.type_arena_name"));
			return;
		}

		String arenaName = arguments.getArgument(0);
		Arena arena = arenaRegistry.getArena(arenaName);

		if (arena == null) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.no_arena_like_that"));
			return;
		}

		ArenaManager.stopGame(true, arena);
		arenaRegistry.unregisterArena(arena);

		arenaConfig.set("instances." + arenaName, null);
		saveConfig();

		plugin.getSignManager().loadSigns();

		arguments.sendMessage(chatManager.prefixedMessage("commands.removed_game_instance"));
	}

	@Command(
		name = "oitc.list",
		permission = "oitc.admin.list",
		desc = "Get a list of registered arenas and their status",
		usage = "/oitc list",
		senderType = PLAYER
	)
	public void listCommand(CommandArguments arguments) {
		List<Arena> arenas = new ArrayList<>(arenaRegistry.getArenas());

		if (arenas.isEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.admin_commands.list_command.no_arenas_created"));
			return;
		}

		String arenaNames = arenas.stream().map(Arena::getId).collect(Collectors.joining(", "));
		arguments.sendMessage(chatManager.prefixedMessage("commands.admin_commands.list_command.format").replace("%list%", arenaNames));
	}

	@Command(
		name = "oitc.forcestart",
		permission = "oitc.admin.forcestart",
		desc = "Forces arena to start without waiting time",
		usage = "/oitc forcestart",
		senderType =  PLAYER
	)
	public void forceStartCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		Arena arena = arenaRegistry.getArena(player);

		if (arena == null) {
			player.sendMessage(chatManager.prefixedMessage("commands.not_playing", player));
			return;
		}

		if (arena.getPlayers().size() < 2) {
			arena.broadcastMessage(chatManager.formatMessage(arena, "in_game.messages.lobby_messages.waiting_for_players"));
			return;
		}

		if (arena.isArenaState(ArenaState.WAITING_FOR_PLAYERS, ArenaState.STARTING)) {
			if (arena.isForceStart()) {
				player.sendMessage(chatManager.message("in_game.messages.already-force-start"));
				return;
			}

			arena.setTimer(0);
			arena.setForceStart(true);
			arena.setArenaState(ArenaState.STARTING);
			arena.broadcastMessage(chatManager.prefixedMessage("in_game.messages.admin_messages.set_starting_in_to_0"));
		}
	}

	@Command(
		name = "oitc.stop",
		permission = "oitc.admin.stop",
		desc = "Stop the arena that you're in",
		usage = "/oitc stop",
		senderType = PLAYER
	)
	public void oitcStopCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		Arena arena = arenaRegistry.getArena(player);

		if (arena == null) {
			player.sendMessage(chatManager.prefixedMessage("commands.not_playing", player));
			return;
		}

		if (arena.getArenaState() != ArenaState.ENDING) {
			ArenaManager.stopGame(true, arena);
		}
	}

	@Command(
		name = "oitc.edit",
		permission = "oitc.admin.edit",
		desc = "Open arena editor for specified arena",
		usage = "/oitc edit <arena name>",
		senderType = PLAYER
	)
	public void editCommand(CommandArguments arguments) {
		Arena arena = arenaRegistry.getArena(arguments.getArgument(0));

		if (arena == null) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.no_arena_like_that"));
			return;
		}

		new SetupInventory(plugin, arena, arguments.getSender()).openInventory();
	}

	@SuppressWarnings("deprecation")
	@Command(
		name = "oitc.help",
		permission = "oitc.admin.help"
	)
	public void helpCommand(CommandArguments arguments) {
		final boolean isPlayer = arguments.isSenderPlayer();
		final CommandSender sender = arguments.getSender();
		final String message = chatManager.coloredRawMessage("&3&l---- One in the Chamber ----");

		arguments.sendMessage("");
		if (isPlayer) MiscUtils.sendCenteredMessage((Player) sender, message); else arguments.sendMessage(message);
		arguments.sendMessage("");

		for (final Command command : plugin.getCommandFramework().getCommands().stream().sorted(Collections
			.reverseOrder(Comparator.comparingInt(cmd -> cmd.usage().length()))).collect(Collectors.toList())) {
			String usage = command.usage(), desc = command.desc();

			if (usage.isEmpty() || usage.contains("help")) continue;

			if (isPlayer) {
				((Player) sender).spigot().sendMessage(new ComponentBuilder()
					.color(ChatColor.DARK_GRAY).append(" • ")
					.append(usage)
					.color(ChatColor.AQUA)
					.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, usage))
					.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(desc)))
					.create());
			} else {
				sender.sendMessage(chatManager.coloredRawMessage(" &8• &b" + usage + " &3- &b" + desc));
			}
		}

		if (isPlayer) {
			final Player player = arguments.getSender();
			player.sendMessage("");
			player.spigot().sendMessage(new ComponentBuilder("TIP:").color(ChatColor.YELLOW).bold(true)
				.append(" Try to ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GRAY)
				.append("hover").color(ChatColor.WHITE).underlined(true)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "Hover on the commands to get info about them.")))
				.append(" or ", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GRAY)
				.append("click").color(ChatColor.WHITE).underlined(true)
				.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "Click on the commands to insert them in the chat.")))
				.append(" on the commands!", ComponentBuilder.FormatRetention.NONE).color(ChatColor.GRAY)
				.create());
		}
	}
}