package com.gracefulcode;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.WindowManager;
import com.gracefulcode.opengine.Window;
import com.gracefulcode.opengine.vulkan.Vulkan;
import com.gracefulcode.opengine.vulkan.MemoryManager;
import com.gracefulcode.opengine.vulkan.Pipeline;
import com.gracefulcode.opengine.vulkan.Shader;

import java.io.FileNotFoundException;
import java.io.IOException;


import org.lwjgl.glfw.GLFWKeyCallback;

public class WindowedApp {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (!glfwInit()) {
			throw new RuntimeException("Failed to initialize GLFW");
		}

		WindowManager.Configuration wmConfiguration = new WindowManager.Configuration();
		wmConfiguration.defaultWindowConfiguration = new Window.Configuration();

		// TODO: This should be made GLFW agnostic.
		wmConfiguration.defaultWindowConfiguration.keyCallback = new GLFWKeyCallback() {
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (action != GLFW_RELEASE) return;
				if (key == GLFW_KEY_ESCAPE) glfwSetWindowShouldClose(window, true);
			}
		};

		Vulkan.Configuration vulkanConfiguration = new Vulkan.Configuration();
		vulkanConfiguration.applicationName = "GOTBK";
		vulkanConfiguration.needGraphics = true;
		vulkanConfiguration.needCompute = false;

		// Vulkan vulkan = new Vulkan(vulkanConfiguration);

		WindowManager windowManager = new WindowManager(wmConfiguration);
		Window window = windowManager.createWindow();

		while (!windowManager.tick()) {

		}

		// vulkan.dispose();
    }
}
