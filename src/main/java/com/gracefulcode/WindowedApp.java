package com.gracefulcode;

import static org.lwjgl.glfw.GLFW.*;

import com.gracefulcode.opengine.ImageSet;
import com.gracefulcode.opengine.v2.WindowManager;
import com.gracefulcode.opengine.v2.Window;
import com.gracefulcode.opengine.v2.vulkan.Vulkan;

import org.lwjgl.glfw.GLFWKeyCallback;

public class WindowedApp {
	public static void main(String[] args) {
		WindowManager.Configuration wmConfiguration = new WindowManager.Configuration();

		// TODO: This should be made GLFW agnostic.
		// wmConfiguration.defaultWindowConfiguration.keyCallback = new GLFWKeyCallback() {
		// 	public void invoke(long window, int key, int scancode, int action, int mods) {
		// 		if (action != GLFW_RELEASE) return;
		// 		if (key == GLFW_KEY_ESCAPE) glfwSetWindowShouldClose(window, true);
		// 	}
		// };
		// wmConfiguration.defaultWindowConfiguration.title = "GOTBK";

		Vulkan.Configuration vulkanConfiguration = new Vulkan.Configuration();
		vulkanConfiguration.applicationName = "GOTBK";
		// vulkanConfiguration.needGraphics = true;
		// vulkanConfiguration.needCompute = false;

		Vulkan vulkan = Vulkan.initialize(vulkanConfiguration);
		// WindowManager windowManager = vulkan.getWindowManager(wmConfiguration);

		// Window vulkanWindow = windowManager.createWindow();

		// ImageSet startingImageSet = vulkanWindow.getFramebuffer();
		// vulkanWindow.setDisplay(startingImageSet);

		// Set up pipeline!
		// Image frameImage = vulkanWindow.createFramebufferImage();
		// Image clearedImage = vulkan.clearImage(frameImage);
		// vulkanWindow.display(clearedImage);

		// Same thing, more compact.
		// vulkanWindow.display(
		// 	vulkan.clearImage(
		// 		vulkanWindow.createFramebufferImage()
		// 	)
		// );

		// while (!windowManager.tick()) {
		// 	// Do game logic here!
		// }

		vulkan.dispose();
    }
}
