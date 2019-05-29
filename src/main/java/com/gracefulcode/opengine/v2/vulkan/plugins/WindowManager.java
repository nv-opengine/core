package com.gracefulcode.opengine.v2.vulkan.plugins;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;

import com.gracefulcode.opengine.v2.vulkan.ExtensionConfiguration;
import com.gracefulcode.opengine.v2.vulkan.PhysicalDevice;
import com.gracefulcode.opengine.v2.vulkan.Vulkan;
import com.gracefulcode.opengine.v2.vulkan.Window;

import java.util.ArrayList;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

/**
 * Adds window manager capabilities to Opengine. Without this plugin, you
 * cannot open windows at all!
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1.1
 */
public class WindowManager implements Plugin {
	protected ArrayList<Window> openWindows = new ArrayList<Window>();
	protected VkInstance vkInstance;

	public WindowManager(Vulkan.Configuration configuration) {
		if (!glfwInit()) {
			throw new AssertionError("GLFW Failed to initialize.");
		}
		
		PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
		if (requiredExtensions == null) {
			throw new AssertionError("Failed to find list of required Vulkan extensions");
		}

		for (int i = 0; i < requiredExtensions.limit(); i++) {
			configuration.extensionConfiguration.setExtension(requiredExtensions.getStringUTF8(i), ExtensionConfiguration.RequireType.REQUIRED);
		}

		configuration.plugins.add(this);
	}

	public boolean canUsePhysicalDevice(PhysicalDevice physicalDevice) {
		return true;
	}

	public Window createWindow(com.gracefulcode.opengine.v2.Window.Configuration configuration) {
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

		Window vw = new Window(id);
		this.openWindows.add(vw);
		return vw;		
	}

	/**
	 * Ticks every open window. Should be called every frame.
	 *
	 * @return True if we should close the app, otherwise false.
	 */
	public boolean tick() {
		glfwPollEvents();

		// TODO: If our window was focused and is now being closed, we should focus another one of our windows.
		// TODO: We should maybe let the user control this since we're in engine code here.
		for (int i = this.openWindows.size() - 1; i >= 0; i--) {
			Window w = this.openWindows.get(i);
			if (w.shouldClose()) {
				this.openWindows.remove(i);
				w.close();
				w.dispose();
			}
		}

		if (this.openWindows.isEmpty()) return true;
		return false;
	}


	public void setupCreateInfo(VkInstanceCreateInfo createInfo) {

	}

	public void postCreate(VkInstance instance) {
		this.vkInstance = instance;
	}

	public void dispose() {

	}
}