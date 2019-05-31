package com.gracefulcode;

import static org.lwjgl.glfw.GLFW.*;

import com.gracefulcode.opengine.v2.vulkan.Vulkan;
import com.gracefulcode.opengine.vulkan.MemoryManager;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ComputeApp {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (!glfwInit()) {
			throw new RuntimeException("Failed to initialize GLFW");
		}

		Vulkan.Configuration vulkanConfiguration = new Vulkan.Configuration();
		vulkanConfiguration.applicationName = "GOTBK";
    }
}
