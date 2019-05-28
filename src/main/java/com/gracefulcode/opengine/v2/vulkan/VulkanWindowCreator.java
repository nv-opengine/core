package com.gracefulcode.opengine.v2.vulkan;

import static org.lwjgl.glfw.GLFW.*;

import com.gracefulcode.opengine.v2.Window;
import com.gracefulcode.opengine.v2.WindowCreator;

import org.lwjgl.glfw.GLFWKeyCallback;

/**
 * Responsible for creating Vulkan-enabled windows.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1
 */
public class VulkanWindowCreator implements WindowCreator<VulkanWindow> {
	/**
	 * The Vulkan instance that we are using.
	 */
	protected Vulkan vulkan;

	/**
	 * The physical devices that are allowed to be used. In practice we are
	 * likely to use the same one every time, but we technically check them all
	 * for every window.
	 */
	protected Iterable<PhysicalDevice> physicalDevices;

	/**
	 * Create a VulkanWindowCreator.
	 *
	 * @param vulkan The Vulkan instance that we are using.
	 * @param physicalDevices The list of devices that we are allowed to use when creating windows.
	 */
	public VulkanWindowCreator(Vulkan vulkan, Iterable<PhysicalDevice> physicalDevices) {
		this.vulkan = vulkan;
		this.physicalDevices = physicalDevices;
	}

	/**
	 * Create a window that is properly enabled for Vulkan. Must be provided a
	 * configuration.
	 *
	 * @param configuration The configuration for this window.
	 * @return The initialized window.
	 */
	public VulkanWindow createWindow(Window.Configuration configuration) {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);
		// glfwWindowHint(GLFW_RED_BITS, 8);
		// glfwWindowHint(GLFW_GREEN_BITS, 8);
		// glfwWindowHint(GLFW_BLUE_BITS, 8);
		// glfwWindowHint(GLFW_ALPHA_BITS, 8);
		glfwWindowHint(GLFW_SRGB_CAPABLE, GLFW_TRUE);

		long id = glfwCreateWindow(
			configuration.width,
			configuration.height,
			configuration.title,
			0,
			0
		);

		// if (configuration.keyCallback != null) {
		// 	glfwSetKeyCallback(id, configuration.keyCallback);
		// }

		glfwShowWindow(id);

		VulkanWindow vw = new VulkanWindow(this.vulkan, id, this.physicalDevices);
		return vw;
	}
}