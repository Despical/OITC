package me.despical.oitc.commands;

import me.despical.commandframework.Command;
import me.despical.commandframework.CommandArguments;
import me.despical.commandframework.CommandFramework;
import me.despical.commandframework.Completer;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.commons.string.StringMatcher;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.menu.setup.ArenaEditorMenu;
import me.despical.oitc.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
		usage = "/oitc help",
		desc = "Main command of One in the Chamber."
	)
	public void mainCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.coloredRawMessage("&3This server is running &bOne in the Chamber " + plugin.getDescription().getVersion() + " &3by &bDespical&3!"));

			if (arguments.hasPermission("oitc.admin")) {
				arguments.sendMessage(chatManager.coloredRawMessage("&3Commands: &b/" + arguments.getLabel() + " help"));
			}

			return;
		}

		CommandFramework commandFramework = plugin.getCommandFramework();
		String label = arguments.getLabel(), arg = arguments.getArgument(0);
		List<String> commands = commandFramework.getCommands().stream().map(cmd -> cmd.name().replace(label + ".", "")).collect(Collectors.toList());
		List<StringMatcher.Match> matches = StringMatcher.match(arg, commands);

		if (!matches.isEmpty()) {
			Optional<Command> optionalMatch = commandFramework.getCommands().stream().filter(cmd -> cmd.name().equals(label + "." + matches.get(0).getMatch())).findFirst();

			if (optionalMatch.isPresent()) {
				String matchedName = getMatchingParts(optionalMatch.get().name(), label + "." + String.join(".", arguments.getArguments()));
				Optional<Command> matchedCommand = commandFramework.getSubCommands().stream().filter(cmd -> cmd.name().equals(matchedName)).findFirst();

				if (matchedCommand.isPresent()) {
					arguments.sendMessage(chatManager.prefixedMessage("Commands.Correct-Usage").replace("%usage%", matchedCommand.get().usage()));
					return;
				}

				arguments.sendMessage(chatManager.prefixedMessage("Commands.Did-You-Mean").replace("%command%", optionalMatch.get().usage()));
				return;
			}

			arguments.sendMessage(chatManager.prefixedMessage("Commands.Did-You-Mean").replace("%command%", '/' + label));
		}
	}

	@Command(
		name = "oitc.create",
		permission = "oitc.admin.create",
		desc = "Create an arena with default configuration.",
		usage = "/oitc create <arena name>",
		senderType = Command.SenderType.PLAYER
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

		final String path = "instances." + id + ".";
		final FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");

		config.set(path + "ready", false);
		config.set(path + "mapName", id);
		config.set(path + "minimumPlayers", 2);
		config.set(path + "maximumPlayers", 10);
		config.set(path + "lobbyLocation", LocationSerializer.SERIALIZED_LOCATION);
		config.set(path + "endLocation", LocationSerializer.SERIALIZED_LOCATION);
		config.set(path + "playersSpawnPoints", new ArrayList<>());
		config.set(path + "signs", new ArrayList<>());

		ConfigUtils.saveConfig(plugin, config, "arenas");

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
		senderType = Command.SenderType.PLAYER
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

		final FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
		config.set("instances." + arenaName, null);
		ConfigUtils.saveConfig(plugin, config, "arenas");

		plugin.getSignManager().loadSigns();

		arguments.sendMessage(chatManager.prefixedMessage("commands.removed_game_instance"));
	}

	@Command(
		name = "oitc.list",
		permission = "oitc.admin.list",
		desc = "Get a list of registered arenas and their status",
		usage = "/oitc list",
		senderType = Command.SenderType.PLAYER
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
		senderType = Command.SenderType.PLAYER
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
		senderType = Command.SenderType.PLAYER
	)
	public void stopCommand(CommandArguments arguments) {
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
		senderType = Command.SenderType.PLAYER
	)
	public void editCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("commands.type_arena_name"));
			return;
		}

		Arena arena = arenaRegistry.getArena(arguments.getArgument(0));

		if (arena == null) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.no_arena_like_that"));
			return;
		}

		final User user = plugin.getUserManager().getUser(arguments.getSender());

		new ArenaEditorMenu(plugin, user, arena).showGui();
	}

	@SuppressWarnings("deprecation")
	@Command(
		name = "oitc.help",
		usage = "/oitc help",
		permission = "oitc.admin.help"
	)
	public void helpCommand(CommandArguments arguments) {
		final boolean isPlayer = arguments.isSenderPlayer();
		final CommandSender sender = arguments.getSender();

		arguments.sendMessage("");
		MiscUtils.sendCenteredMessage(sender, "&3&l---- One in the Chamber ----");
		arguments.sendMessage("");

		for (final Command command : plugin.getCommandFramework().getSubCommands()) {
			String usage = command.usage(), desc = command.desc();

			if (desc.isEmpty() || usage.isEmpty()) continue;

			if (isPlayer) {
				((Player) sender).spigot().sendMessage(
					new ComponentBuilder(ChatColor.DARK_GRAY + " • ")
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

	@Completer(
		name = "oitc"
	)
	public List<String> onTabComplete(CommandArguments arguments) {
		final List<String> completions = new ArrayList<>(), commands = plugin.getCommandFramework().getSubCommands().stream().map(cmd -> cmd.name().replace(arguments.getLabel() + '.', "")).collect(Collectors.toList());
		final String args[] = arguments.getArguments(), arg = args[0];

		if (args.length == 1) {
			return StringUtil.copyPartialMatches(arg, arguments.hasPermission("oitc.admin") ? commands : Arrays.asList("top", "stats", "join", "leave", "randomjoin"), completions);
		}

		if (args.length == 2) {
			if (arg.equalsIgnoreCase("top")) {
				return StringUtil.copyPartialMatches(args[1], Arrays.asList("kills", "deaths", "games_played", "highest_score", "loses", "wins"), completions);
			}

			if (arg.equalsIgnoreCase("stats")) {
				return StringUtil.copyPartialMatches(args[1], plugin.getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), completions);
			}

			if (Arrays.asList("delete", "edit", "join").contains(arg)) {
				final List<String> arenas = arenaRegistry.getArenas().stream().map(Arena::getId).sorted().collect(Collectors.toList());

				return StringUtil.copyPartialMatches(args[1], arenas, completions);
			}
		}

		return completions;
	}

	public String getMatchingParts(String matched, String current) {
		String[] matchedArray = matched.split("\\."), currentArray = current.split("\\.");
		int max = Math.min(matchedArray.length, currentArray.length);
		List<String> matchingParts = new ArrayList<>();

		for (int i = 0; i < max; i++) {
			if (matchedArray[i].equals(currentArray[i])) {
				matchingParts.add(matchedArray[i]);
			}
		}

		return String.join(".", matchingParts);
	}
}