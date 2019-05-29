package com.gracefulcode.opengine.v2.vulkan;

import com.gracefulcode.opengine.v2.PhysicalDeviceSelector;

/**
 * The default physical device selector should work well for graphics or
 * compute workloads. For that reason, we actually only key off of two things.
 *
 * First, we prefer dedicated graphics cards. For the vast majority of cases,
 * that will be enough. Most computers have only one integrated GPU (in which
 * case we'll choost that one no matter what), one dedicated GPU (in which case
 * we'll select that one no matter what), or one of each (in which case we'll
 * grab the dedciated one thanks to this rule).
 *
 * Secondly, because it's easy, we prefer the device that supports the highest
 * version of the API.
 *
 * If you need something more, you will need to implement your own
 * PhysicalDeviceSelector.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1
 */
public class DefaultPhysicalDeviceSelector implements PhysicalDeviceSelector<PhysicalDevice> {
	public int compare(PhysicalDevice a, PhysicalDevice b) {
		// Return the dedicated graphics card if one exists.
		int comparison = Integer.compare(b.deviceType(), a.deviceType());
		if (comparison != 0) return comparison;

		// If one supports a higher level of API, it's probably a better card?
		// Or at least the one with the most up to date graphics driver.
		comparison = Integer.compare(b.apiVersion(), a.apiVersion());
		return comparison;
	}
}