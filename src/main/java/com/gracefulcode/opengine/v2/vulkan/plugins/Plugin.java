package com.gracefulcode.opengine.v2.vulkan.plugins;

import com.gracefulcode.opengine.v2.vulkan.ExtensionConfiguration;
import com.gracefulcode.opengine.v2.vulkan.LayerConfiguration;
import com.gracefulcode.opengine.v2.vulkan.PhysicalDevice;
import com.gracefulcode.opengine.v2.vulkan.Vulkan;

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
	 * Set up the extensions that you desire/require. At this point we know
	 * what extensions are supported and can throw errors if things aren't
	 * possible.
	 *
	 * @param configuration The list of extensions that are available. You can change the priorities.
	 */
	public void setupExtensions(ExtensionConfiguration configuration);

	/**
	 * Set up the layers that you desire/require. At this point we know
	 * what layers are supported and can throw errors if things aren't
	 * possible.
	 *
	 * @param configuration The list of layers that are available. You can change the priorities.
	 */
	public void setupLayers(LayerConfiguration configuration);

	/**
	 * Called after the VkInstance is created.
	 *
	 * @param instance The VkInstance that was just created.
	 * @param extensionConfiguration The list of extensions that were created.
	 * @param layerConfiguration The list of layers that were created.
	 */
	public void postCreate(Vulkan vulkan, VkInstance instance, ExtensionConfiguration extensionConfiguration, LayerConfiguration LayerConfiguration);

	/**
	 * Plugins can reject a physical device for any reason.
	 *
	 * @param physicalDevice The physical device under test.
	 * @return True if this plugin can use this physical device, otherwise false.
	 */
	public boolean canUsePhysicalDevice(PhysicalDevice physicalDevice);

	/**
	 * Called during teardown. Clean up your stuff.
	 */
	public void dispose();
}