package me.despical.oitc.commands;

import me.despical.commandframework.CommandArguments;
import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.handlers.ChatManager;

public abstract class AbstractCommand {

	protected final Main plugin;
	protected final ArenaRegistry arenaRegistry;
	protected final ChatManager chatManager;

	public AbstractCommand(final Main plugin) {
		this.plugin = plugin;
		this.arenaRegistry = plugin.getArenaRegistry();
		this.chatManager = plugin.getChatManager();
		this.plugin.getCommandFramework().registerCommands(this);
	}

	public static void registerCommands(final Main plugin) {
		plugin.getCommandFramework().addCustomParameter("Player", CommandArguments::getSender);

		new AdminCommands(plugin);
		new PlayerCommands(plugin);
	}
}