package com.gracefulcode.opengine.v2.vulkan.plugins;

import com.gracefulcode.opengine.v2.vulkan.PhysicalDevice;

import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

/**
 * The interface that all plugins should implement.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1.1
 */
public interface Plugin {
	/**
	 * Called before VkInstance is created. Allows you to alter the createInfo.
	 *
	 * @param createInfo The configuration of the VkInstance that will be created.
	 */
	public void setupCreateInfo(VkInstanceCreateInfo createInfo);

	/**
	 * Called after the VkInstance is created.
	 *
	 * @param instance The VkInstance that was just created.
	 */
	public void postCreate(VkInstance instance);

	/**
	 * Plugins can reject a physical device for any reason.
	 */
	public boolean canUsePhysicalDevice(PhysicalDevice physicalDevice);

	/**
	 * Called during teardown. Clean up your stuff.
	 */
	public void dispose();
}