package com.gracefulcode;

import com.gracefulcode.opengine.v2.Window;
import com.gracefulcode.opengine.v2.vulkan.ExtensionConfiguration;
import com.gracefulcode.opengine.v2.vulkan.plugins.Debug;
import com.gracefulcode.opengine.v2.vulkan.plugins.WindowManager;
import com.gracefulcode.opengine.v2.vulkan.Vulkan;

public class WindowedApp {
	public static void main(String[] args) {
		Vulkan.Configuration vulkanConfiguration = new Vulkan.Configuration();
		vulkanConfiguration.applicationName = "GOTBK";

		// Allows the debug plugin to register itself. Should be done before initialize.
		// By default the debug plugin will not cause errors to be thrown if it cannot work.
		// This is largely because MoltenVK doesn't support debugging.
		// You should turn debugging off in production. It can make things much slower.
		Debug debugPlugin = new Debug(vulkanConfiguration);
		WindowManager windowManagerPlugin = new WindowManager(vulkanConfiguration);

		// Create our vulkan instance.
		Vulkan vulkan = Vulkan.initialize(vulkanConfiguration);

		Window.Configuration windowConfiguration = new Window.Configuration();
		Window vulkanWindow = windowManagerPlugin.createWindow(windowConfiguration);

		// WindowManager windowManager = vulkan.getWindowManager();

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

		while (!windowManagerPlugin.tick()) {
			// Do game logic here!
		}

		vulkan.dispose();
    }
}
