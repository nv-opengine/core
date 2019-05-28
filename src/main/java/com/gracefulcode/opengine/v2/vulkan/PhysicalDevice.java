package com.gracefulcode.opengine.v2.vulkan;

import org.lwjgl.vulkan.VkPhysicalDevice;

/**
 * A physical device is a graphics card. These have limitations and features.
 */
public class PhysicalDevice implements com.gracefulcode.opengine.v2.PhysicalDevice<LogicalDevice> {
	public PhysicalDevice(VkPhysicalDevice physicalDevice) {

	}

	public LogicalDevice createLogicalDevice(String[] requiredExtensions, boolean hasGraphicsQueue, boolean hasComputeQueue) {
		return null;
	}

	public int deviceType() {
		return 0;
	}

	public int apiVersion() {
		return 0;
	}
}