package me.despical.oitc.events.spectator.components;

import com.github.despical.inventoryframework.pane.StaticPane;
import me.despical.oitc.events.spectator.SpectatorSettingsMenu;

/**
 * @author Despical
 * <p>
 * Created at 03.10.2020
 */
public interface SpectatorSettingComponent {

	void prepare(SpectatorSettingsMenu spectatorSettingsMenu);

	void injectComponents(StaticPane pane);
}