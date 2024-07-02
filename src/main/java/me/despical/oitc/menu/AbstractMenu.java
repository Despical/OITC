package me.despical.oitc.menu;

import me.despical.inventoryframework.Gui;
import me.despical.inventoryframework.pane.PaginatedPane;
import me.despical.oitc.Main;
import me.despical.oitc.arena.Arena;
import me.despical.oitc.user.User;
import org.jetbrains.annotations.Nullable;

/**
 * @author Despical
 * <p>
 * Created at 2.07.2024
 */
public abstract class AbstractMenu {

	protected final Gui gui;
	protected final int rows;
	protected final User user;
	protected final Arena arena;
	protected final String title;
	protected final Main plugin;
	protected final PaginatedPane paginatedPane;

	public AbstractMenu(Main plugin, User user, String title, int rows) {
		this(plugin, user, null, title, rows);
	}

	public AbstractMenu(Main plugin, User user, Arena arena, String title, int rows) {
		this.plugin = plugin;
		this.user = user;
		this.arena = arena;
		this.gui = new Gui(plugin, this.rows = rows, this.title = title);
		this.paginatedPane = new PaginatedPane(9, rows);
		this.gui.setOnGlobalClick(event -> event.setCancelled(true));
		this.gui.addPane(paginatedPane);
	}

	public final void showGui() {
		Page pinnedPage = user.getPinnedPage();

		if (pinnedPage.getPage() != 0) this.setPage(pinnedPage.getTitle(), pinnedPage.getRows(), pinnedPage.getPage());

		this.gui.show(this.user.getPlayer());
	}

	public final void showGui(@Nullable String title, int rows, int page) {
		this.setPage(title, rows, page, false);

		this.showGui();
	}

	public final void showGuiFromPage(final Page page) {
		this.setPage(page.getTitle(), page.getRows(), page.getPage(), true);

		this.gui.show(this.user.getPlayer());
	}

	public final void setPage(@Nullable String title, int rows, int page) {
		this.setPage(title, rows, page, true);
	}

	public final void setPage(int page) {
		this.paginatedPane.setPage(page);
		this.gui.update();
	}

	private void setPage(@Nullable String title, int rows, int page, boolean update) {
		this.gui.setTitle(title != null ? title : this.gui.getTitle());
		this.gui.setRows(rows);
		this.paginatedPane.setPage(page);

		if (update) this.gui.update();
	}

	public final void restorePage() {
		paginatedPane.setPage(0);
		gui.setRows(rows);
		gui.setTitle(title);
		gui.update();
	}

	public final Main getPlugin() {
		return plugin;
	}

	public final User getUser() {
		return user;
	}

	public final Arena getArena() {
		return arena;
	}

	public final PaginatedPane getPaginatedPane() {
		return paginatedPane;
	}

	public final Gui getGui() {
		return gui;
	}
}