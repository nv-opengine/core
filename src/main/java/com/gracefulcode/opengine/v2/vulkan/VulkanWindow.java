package com.gracefulcode.opengine.v2.vulkan;

import com.gracefulcode.opengine.v2.Window;

public class VulkanWindow implements Window {
	protected Iterable<PhysicalDevice> physicalDevices;
	protected Vulkan vulkan;
	protected long id;

	public VulkanWindow(Vulkan vulkan, long id, Iterable<PhysicalDevice> physicalDevices) {
		this.vulkan = vulkan;
		this.physicalDevices = physicalDevices;
		this.id = id;
	}

	public boolean shouldClose() {
		return false;
	}

	public void close() {

	}

	public void dispose() {

	}
}