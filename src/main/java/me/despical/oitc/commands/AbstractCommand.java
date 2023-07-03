package me.despical.oitc.commands;

import me.despical.commons.configuration.ConfigUtils;
import me.despical.oitc.Main;
import me.despical.oitc.commands.commands.*;
import me.despical.oitc.handlers.ChatManager;
import org.bukkit.configuration.file.FileConfiguration;

public abstract class AbstractCommand {

	protected final Main plugin;
	protected final ChatManager chatManager;
	protected final FileConfiguration arenaConfig;

	public AbstractCommand(final Main plugin) {
		this.plugin = plugin;
		this.chatManager = plugin.getChatManager();
		this.arenaConfig = ConfigUtils.getConfig(plugin, "arenas");
		this.plugin.getCommandFramework().registerCommands(this);
	}

	protected void saveConfig() {
		ConfigUtils.saveConfig(plugin, arenaConfig, "arenas");
	}

	public static void registerCommands(final Main plugin) {
		final Class<?>[] commandClasses = new Class[] {AdminCommands.class, PlayerCommands.class, TabCompleter.class};

		for (Class<?> clazz : commandClasses) {
			try {
				clazz.getConstructor(Main.class).newInstance(plugin);
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}
	}
}