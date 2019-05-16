package com.gracefulcode;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.VK10.*;

// import com.gracefulcode.opengine.WindowManager;
// import com.gracefulcode.opengine.Window;
import com.gracefulcode.opengine.vulkan.Vulkan;
import com.gracefulcode.opengine.vulkan.MemoryManager;
import com.gracefulcode.opengine.vulkan.Pipeline;
import com.gracefulcode.opengine.vulkan.Shader;

import java.io.FileNotFoundException;
import java.io.IOException;


// import org.lwjgl.glfw.GLFWKeyCallback;

public class App {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (!glfwInit()) {
			throw new RuntimeException("Failed to initialize GLFW");
		}

		// WindowManager.Configuration wmConfiguration = new WindowManager.Configuration();
		// wmConfiguration.defaultWindowConfiguration = new Window.Configuration();

		// TODO: This should be made GLFW agnostic.
		// wmConfiguration.defaultWindowConfiguration.keyCallback = new GLFWKeyCallback() {
		// 	public void invoke(long window, int key, int scancode, int action, int mods) {
		// 		if (action != GLFW_RELEASE) return;
		// 		if (key == GLFW_KEY_ESCAPE) glfwSetWindowShouldClose(window, true);
		// 	}
		// };

		Vulkan.Configuration vulkanConfiguration = new Vulkan.Configuration();
		vulkanConfiguration.applicationName = "GOTBK";
		vulkanConfiguration.needGraphics = false;
		vulkanConfiguration.needCompute = true;

		Vulkan vulkan = new Vulkan(vulkanConfiguration);
		MemoryManager.Buffer bufferA = vulkan.createComputeBuffer("Compute: A", 1023);
		MemoryManager.Buffer bufferB = vulkan.createComputeBuffer("Compute: B", 1023);
		vulkan.doneAllocating();
		
		Shader shader = vulkan.createComputeShader("comp.spv");

		// TODO: I am going to want to swap these back and forth, using last
		// "frame"'s data as the scratchpad for this "frame." The current
		// createBinding setup makes that hard/impossible. Rethink?
		shader.createBinding(0, bufferA);
		shader.createBinding(1, bufferB);
		shader.doneBinding();

		Pipeline computePipeline = vulkan.createComputePipeline(shader, "main");

		// WindowManager windowManager = new WindowManager(wmConfiguration);
		// Window window = windowManager.createWindow();
		// Window window2 = windowManager.createWindow();

		// while (true) {
		// 	boolean shouldClose = windowManager.tick();
		// 	if (shouldClose) break;
		// }

		vulkan.dispose();
    }
}
