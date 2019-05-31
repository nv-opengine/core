package com.gracefulcode.opengine.vulkan.plugins.interfaces;

import com.gracefulcode.opengine.vulkan.PhysicalDevice;

public interface FiltersPhysicalDevices {
	/**
	 * Plugins can reject a physical device for any reason.
	 *
	 * @param physicalDevice The physical device under test.
	 * @return True if this plugin can use this physical device, otherwise false.
	 */
	public boolean canUsePhysicalDevice(PhysicalDevice physicalDevice);
}