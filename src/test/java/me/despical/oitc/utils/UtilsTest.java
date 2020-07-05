package me.despical.oitc.utils;

import org.junit.Test;

import junit.framework.Assert;

/**
 * @author Despical
 * <p>
 * Created at 05.07.2020
 */
public class UtilsTest {

	@Test
	public void serializeInt() {
		Assert.assertEquals(9, Utils.serializeInt(3));
		Assert.assertEquals(45, Utils.serializeInt(37));
		Assert.assertEquals(45, Utils.serializeInt(43));
	}
}
