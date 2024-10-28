package me.despical.oitc.command;

import me.despical.commandframework.CommandArguments;
import me.despical.oitc.Main;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.handlers.ChatManager;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractCommandHandler {

	protected static final Main plugin = JavaPlugin.getPlugin(Main.class);
	protected static final ArenaRegistry arenaRegistry = plugin.getArenaRegistry();
	protected static final ChatManager chatManager = plugin.getChatManager();

	static {
		plugin.getCommandFramework().addCustomParameter("Player", CommandArguments::getSender);
	}

	public AbstractCommandHandler() {
		plugin.getCommandFramework().registerCommands(this);
	}
}