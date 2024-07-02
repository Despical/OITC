package me.despical.oitc.menu;

import me.despical.oitc.arena.Arena;
import org.jetbrains.annotations.Nullable;

/**
 * @author Despical
 * <p>
 * Created at 2.07.2024
 */
public class Page {

	private final Arena arena;
	private final String title;
	private final int rows;
	private final int page;

	public Page(String title, int rows, int page) {
		this(null, title, rows, page);
	}

	public Page(Arena arena, String title, int rows, int page) {
		this.arena = arena;
		this.title = title;
		this.rows = rows;
		this.page = page;
	}

	@Nullable
	public Arena getArena() {
		return arena;
	}

	public String getTitle() {
		return title;
	}

	public int getRows() {
		return rows;
	}

	public int getPage() {
		return page;
	}
}