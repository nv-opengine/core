package com.gracefulcode.opengine.vulkan;

import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

/**
 * The deafult physical device selector should work well for graphics or
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
 */
public class DefaultPhysicalDeviceSelector implements PhysicalDeviceSelector {
	public int compare(VkPhysicalDeviceProperties a, VkPhysicalDeviceProperties b) {
		// Return the dedicated graphics card if one exists.
		int comparison = Integer.compare(a.deviceType(), b.deviceType());
		if (comparison != 0) return comparison;

		// If one supports a higher level of API, it's probably a better card?
		// Or at least the one with the most up to date graphics driver.
		comparison = Integer.compare(a.apiVersion(), b.apiVersion());
		return comparison;
	}
}