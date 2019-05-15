package com.gracefulcode.opengine.vulkan;

import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

public class DefaultPhysicalDeviceSelector implements PhysicalDeviceSelector {
	public int compare(VkPhysicalDeviceProperties a, VkPhysicalDeviceProperties b) {
		// Return the dedicated graphics card if one exists.
		int comparison = Integer.compare(a.deviceType(), b.deviceType());
		if (comparison != 0) return comparison;

		// If one supporst a higher level of API, it's probably a better card? Or at least the one with the most up to date graphics.
		comparison = Integer.compare(a.apiVersion(), b.apiVersion());
		return comparison;
	}
}