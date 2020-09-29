package me.despical.oitc.utils;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

/**
 * @author Despical
 * <p>
 * Created at 05.07.2020
 */
public class UtilsTest {

	@Test
	public void serializeInt() {
		assertEquals(9, Utils.serializeInt(3));
		assertEquals(9, Utils.serializeInt(9));
		assertEquals(27, Utils.serializeInt(24));
		assertEquals(45, Utils.serializeInt(37));
		assertEquals(45, Utils.serializeInt(43));
	}
}
