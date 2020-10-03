package me.despical.oitc.handlers.setup;

import com.github.despical.inventoryframework.Gui;
import com.github.despical.inventoryframework.pane.StaticPane;
import me.despical.commonsbox.configuration.ConfigUtils;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.handlers.setup.components.ArenaRegisterComponent;
import me.despical.oitc.handlers.setup.components.MiscComponents;
import me.despical.oitc.handlers.setup.components.PlayerAmountComponents;
import me.despical.oitc.handlers.setup.components.SpawnComponents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public class SetupInventory {

	private final Random random = new Random();
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
		this.gui = new Gui(plugin, 3, "OITC Arena Editor");
		this.gui.setOnGlobalClick(e -> e.setCancelled(true));
		StaticPane pane = new StaticPane(9, 3);
		this.gui.addPane(pane);
		prepareComponents(pane);
	}

	private void prepareComponents(StaticPane pane) {
		SpawnComponents spawnComponents = new SpawnComponents();
		spawnComponents.prepare(this);
		spawnComponents.injectComponents(pane);

		PlayerAmountComponents playerAmountComponents = new PlayerAmountComponents();
		playerAmountComponents.prepare(this);
		playerAmountComponents.injectComponents(pane);

		MiscComponents miscComponents = new MiscComponents();
		miscComponents.prepare(this);
		miscComponents.injectComponents(pane);

		ArenaRegisterComponent arenaRegisterComponent = new ArenaRegisterComponent();
		arenaRegisterComponent.prepare(this);
		arenaRegisterComponent.injectComponents(pane);
	}

	private void sendProTip(Player p) {
		int rand = random.nextInt(8 + 1);

		switch (rand) {
			case 0:
				p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7We are open source! You can always help us by contributing! Check https://github.com/Despical/OITC"));
				break;
			case 1:
				p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Need help? Join our discord server: https://discordapp.com/invite/Vhyy4HA"));
				break;
			case 2:
				p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Need help? Check our wiki: https://github.com/Despical/OITC/wiki"));
				break;
			case 3:
				p.sendMessage(plugin.getChatManager().colorRawMessage("&e&lTIP: &7Want to access exclusive maps, addons and more? Check our Patreon page: https://www.patreon.com/despical"));
				break;
			default:
				break;
		}
	}

	public void openInventory() {
		sendProTip(player);
		gui.show(player);
	}

	public Main getPlugin() {
		return plugin;
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