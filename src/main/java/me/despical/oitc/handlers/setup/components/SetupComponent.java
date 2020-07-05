package me.despical.oitc.handlers.setup.components;

import com.github.stefvanschie.inventoryframework.pane.StaticPane;

import me.despical.oitc.handlers.setup.SetupInventory;

/**
 * @author Despical
 * <p>
 * Created at 02.07.2020
 */
public interface SetupComponent {

	void prepare(SetupInventory setupInventory);

	void injectComponents(StaticPane pane);

}