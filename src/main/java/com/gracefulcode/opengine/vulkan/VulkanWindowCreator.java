package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFW.*;

import com.gracefulcode.opengine.Window;
import com.gracefulcode.opengine.WindowCreator;

import org.lwjgl.glfw.GLFWKeyCallback;

public class VulkanWindowCreator implements WindowCreator<VulkanWindow> {
	protected Vulkan vulkan;
	protected Iterable<VulkanPhysicalDevice> physicalDevices;

	public VulkanWindowCreator(Vulkan vulkan, Iterable<VulkanPhysicalDevice> physicalDevices) {
		this.vulkan = vulkan;
		this.physicalDevices = physicalDevices;
	}

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

		if (configuration.keyCallback != null) {
			glfwSetKeyCallback(id, configuration.keyCallback);
		}

		glfwShowWindow(id);

		VulkanWindow vw = new VulkanWindow(this.vulkan, id, this.physicalDevices);
		return vw;
	}
}