package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFW.*;

import com.gracefulcode.opengine.Window;
import com.gracefulcode.opengine.WindowCreator;

import org.lwjgl.glfw.GLFWKeyCallback;

public class VulkanWindowCreator implements WindowCreator<VulkanWindow> {
	protected Vulkan vulkan;

	public VulkanWindowCreator(Vulkan vulkan) {
		this.vulkan = vulkan;
	}

	public VulkanWindow createWindow(Window.Configuration configuration) {
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

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

		VulkanWindow vw = new VulkanWindow(this.vulkan, id);
		return vw;
	}
}