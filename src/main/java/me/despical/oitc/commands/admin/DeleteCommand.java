package me.despical.oitc.commands.admin;

import me.despical.commons.configuration.ConfigUtils;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.arena.ArenaManager;
import me.despical.oitc.arena.ArenaRegistry;
import me.despical.oitc.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 23.06.2022
 */
public class DeleteCommand extends SubCommand {

	private final Set<CommandSender> confirmations;

	public DeleteCommand() {
		super ("delete");
		this.confirmations = new HashSet<>();

		setPermission("oitc.admin.delete");
	}

	@Override
	public String getPossibleArguments() {
		return "<arena>";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public void execute(CommandSender sender, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(chatManager.prefixedMessage("commands.type_arena_name"));
			return;
		}

		String arenaName = args[0];
		Arena arena = ArenaRegistry.getArena(arenaName);

		if (arena == null) {
			sender.sendMessage(chatManager.prefixedMessage("commands.no_arena_like_that"));
			return;
		}

		if (!confirmations.contains(sender)) {
			confirmations.add(sender);
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> confirmations.remove(sender), 200);
			sender.sendMessage(chatManager.prefixedMessage("commands.are_you_sure"));
			return;
		}

		confirmations.remove(sender);

		ArenaManager.stopGame(true, arena);
		ArenaRegistry.unregisterArena(arena);

		config.set("instances." + arenaName, null);
		ConfigUtils.saveConfig(plugin, config, "arenas");

		plugin.getSignManager().loadSigns();

		sender.sendMessage(chatManager.prefixedMessage("commands.removed_game_instance"));
	}

	@Override
	public String getTutorial() {
		return "Deletes arena with the current configuration";
	}

	@Override
	public int getType() {
		return GENERIC;
	}

	@Override
	public int getSenderType() {
		return BOTH;
	}
}