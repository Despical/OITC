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

package me.despical.oitc.commands.admin;

import me.despical.commandframework.Command;
import me.despical.commandframework.CommandArguments;
import me.despical.commons.configuration.ConfigUtils;
import me.despical.commons.miscellaneous.MiscUtils;
import me.despical.commons.serializer.LocationSerializer;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.arena.ArenaState;
import me.despical.oitc.handlers.ChatManager;
import me.despical.oitc.handlers.setup.SetupInventory;
import me.despical.oitc.utils.Debugger;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Despical
 * <p>
 * Created at 18.05.2021
 */
public class AdminCommands {

	private final Main plugin;
	private final ChatManager chatManager;
	private final FileConfiguration config;
	private final List<CommandSender> deleteConfirmations;

	public AdminCommands(Main plugin) {
		this.plugin = plugin;
		this.chatManager = plugin.getChatManager();
		this.config = ConfigUtils.getConfig(plugin, "arenas");
		this.deleteConfirmations = new ArrayList<>();

		plugin.getCommandFramework().registerCommands(this);
	}
	
	@Command(
		name = "oitc.create",
		permission = "oitc.admin.create",
		desc = "Creates new arena with default configuration",
		usage = "/oitc create <arenaName>",
		senderType = Command.SenderType.PLAYER
	)
	public void createCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.Type-Arena-Name"));
			return;
		}

		String arenaName = arguments.getArgument(0);
		Player player = arguments.getSender();

		if (ArenaRegistry.isArena(arenaName)) {
			arguments.sendMessage(chatManager.prefixedRawMessage("&cArena with that ID already exists!"));
			arguments.sendMessage(chatManager.prefixedRawMessage("&cUsage: /oitc create <ID>"));
			return;
		}

		if (config.contains("instances." + arenaName)) {
			player.sendMessage(chatManager.prefixedRawMessage("&cArena already exists! Use another ID or delete it first!"));
		} else {
			createArenaConfiguration(arenaName);
			player.sendMessage(ChatColor.BOLD + "----------------------------------------");
			MiscUtils.sendCenteredMessage(player, "&eInstance " + arenaName + " created!");
			player.sendMessage("");
			MiscUtils.sendCenteredMessage(player, "&aEdit this arena via &6/oitc edit "  + arenaName + "&a!");
			player.sendMessage(ChatColor.BOLD + "----------------------------------------");
		}
	}

	private void createArenaConfiguration(String id) {
		String path = "instances." + id + ".";

		config.set(path + "lobbylocation", LocationSerializer.SERIALIZED_LOCATION);
		config.set(path + "Endlocation", LocationSerializer.SERIALIZED_LOCATION);
		config.set(path + "playerspawnpoints", new ArrayList<>());
		config.set(path + "minimumplayers", 2);
		config.set(path + "maximumplayers", 10);
		config.set(path + "mapname", id);
		config.set(path + "signs", new ArrayList<>());
		config.set(path + "isdone", false);

		ConfigUtils.saveConfig(plugin, config, "arenas");

		Arena arena = new Arena(id);
		arena.setPlayerSpawnPoints(new ArrayList<>());
		arena.setMapName(config.getString(path + "mapname"));
		arena.setLobbyLocation(LocationSerializer.DEFAULT_LOCATION);
		arena.setEndLocation(LocationSerializer.DEFAULT_LOCATION);

		ArenaRegistry.registerArena(arena);
	}

	@Command(
		name = "oitc.delete",
		permission = "oitc.admin.delete",
		usage = "/oitc delete <arena>",
		desc = "Deletes arena that user specified"
	)
	public void deleteCommand(CommandArguments arguments) {
		if (arguments.isArgumentsEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.Type-Arena-Name"));
			return;
		}

		String arenaName = arguments.getArgument(0);
		Arena arena = ArenaRegistry.getArena(arenaName);

		if (arena == null) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.No-Arena-Like-That"));
			return;
		}

		CommandSender sender = arguments.getSender();

		if (!deleteConfirmations.contains(sender)) {
			deleteConfirmations.add(sender);
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> deleteConfirmations.remove(sender), 200);
			sender.sendMessage(chatManager.prefixedMessage("Commands.Are-You-Sure"));
			return;
		}

		deleteConfirmations.remove(sender);

		ArenaManager.stopGame(true, arena);
		ArenaRegistry.unregisterArena(arena);

		config.set("instances." + arenaName, null);
		ConfigUtils.saveConfig(plugin, config, "arenas");

		plugin.getSignManager().loadSigns();

		sender.sendMessage(chatManager.prefixedMessage("Commands.Removed-Game-Instance"));
	}

	@Command(
		name = "oitc.reload",
		permission = "oitc.admin",
		usage = "/oitc reload",
		desc = "Reloads all game arenas and configurations",
		cooldown = 5
	)
	public void reloadCommand(CommandArguments arguments) {
		Debugger.debug("Initiated plugin reload by {0}", arguments.getSender().getName());
		long start = System.currentTimeMillis();

		chatManager.reloadConfig();

		ArenaRegistry.getArenas().forEach(arena -> ArenaManager.stopGame(true, arena));
		ArenaRegistry.registerArenas();

		arguments.sendMessage(chatManager.prefixedMessage("Commands.Admin-Commands.Success-Reload"));
		Debugger.debug("[Reloader] Finished reloading took {0} ms", System.currentTimeMillis() - start);
	}

	@Command(
		name = "oitc.list",
		permission = "oitc.admin.list",
		usage = "/oitc list",
		desc = "Show all of the existing arenas"
	)
	public void listCommand(CommandArguments arguments) {
		List<Arena> arenas = ArenaRegistry.getArenas();

		if (arenas.isEmpty()) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.Admin-Commands.List-Command.No-Arenas-Created"));
			return;
		}

		String arenaNames = arenas.stream().map(Arena::getId).collect(Collectors.joining(", "));
		arguments.sendMessage(chatManager.prefixedMessage("Commands.Admin-Commands.List-Command.Format").replace("%list%", arenaNames));
	}

	@Command(
		name = "oitc.edit",
		permission = "oitc.admin.edit",
		min = 1,
		usage = "/oitc edit <arena>",
		desc = "Opens arena editor menu",
		senderType = Command.SenderType.PLAYER
	)
	public void editCommand(CommandArguments arguments) {
		Arena arena = ArenaRegistry.getArena(arguments.getArgument(0));

		if (arena == null) {
			arguments.sendMessage(chatManager.prefixedMessage("Commands.No-Arena-Like-That"));
			return;
		}

		new SetupInventory(arena, arguments.getSender()).openInventory();
	}

	@Command(
		name = "oitc.forcestart",
		permission = "oitc.admin",
		usage = "/oitc forcestart",
		desc = "Force start arena that user in",
		senderType = Command.SenderType.PLAYER
	)
	public void forceStartCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena == null) {
			player.sendMessage(chatManager.prefixedMessage("Commands.Not-Playing", player));
			return;
		}

		if (arena.getPlayers().size() < 2) {
			arena.broadcastMessage(chatManager.prefixedFormattedPathMessage(arena, "In-Game.Messages.Lobby-Messages.Waiting-For-Players", arena.getMinimumPlayers()));
			return;
		}

		if (arena.getArenaState() == ArenaState.WAITING_FOR_PLAYERS || arena.getArenaState() == ArenaState.STARTING) {
			arena.setArenaState(ArenaState.STARTING);
			arena.setForceStart(true);
			arena.setTimer(0);
			arena.broadcastMessage(chatManager.prefixedMessage("In-Game.Messages.Admin-Messages.Set-Starting-In-To-0"));
		}
	}

	@Command(
		name = "oitc.stop",
		permission = "oitc.admin",
		usage = "/oitc stop",
		desc = "Force stop arena that user in",
		senderType = Command.SenderType.PLAYER
	)
	public void stopCommand(CommandArguments arguments) {
		Player player = arguments.getSender();
		Arena arena = ArenaRegistry.getArena(player);

		if (arena == null) {
			player.sendMessage(chatManager.prefixedMessage("Commands.Not-Playing", player));
			return;
		}

		if (arena.getArenaState() != ArenaState.ENDING) {
			ArenaManager.stopGame(true, arena);
		}
	}

	@Command(
		name = "oitc.help",
		permission = "oitc.admin",
		usage = "/oitc help",
		desc = "Sends all of the command and their usages"
	)
	public void helpCommand(CommandArguments arguments) {
		arguments.sendMessage(chatManager.coloredRawMessage("&3&l---- One in the Chamber Admin Commands ----"));
		arguments.sendMessage("");

		boolean isPlayer = arguments.isSenderPlayer();

		for (Command command : plugin.getCommandFramework().getCommands()) {
			String usage = command.usage(), desc = command.desc();

			if (usage.isEmpty()) continue;

			if (isPlayer) {
				arguments.getSender().spigot().sendMessage(new ComponentBuilder(usage)
					.color(ChatColor.AQUA)
					.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, usage))
					.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(desc)))
					.create());
			} else {
				arguments.sendMessage(chatManager.coloredRawMessage("&b" + usage + " &3- &b" + desc));
			}
		}

		if (isPlayer) {
			arguments.sendMessage("");
			arguments.getSender().spigot().sendMessage(new ComponentBuilder("TIP:").color(ChatColor.YELLOW).bold(true)
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