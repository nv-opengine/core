package com.gracefulcode;

import com.gracefulcode.opengine.v2.Window;
import com.gracefulcode.opengine.v2.vulkan.plugins.Debug;
import com.gracefulcode.opengine.v2.vulkan.plugins.WindowManager;
import com.gracefulcode.opengine.v2.vulkan.Vulkan;

public class WindowedApp {
	public static void main(String[] args) {
		Vulkan.Configuration vulkanConfiguration = new Vulkan.Configuration();
		vulkanConfiguration.applicationName = "GOTBK";

		/**
		 * Register the plugins.
		 *
		 * Debug lets us know when we use the API wrong or in a way that might
		 * be slow. Use the default (log to stderr).
		 * WindowManager lets us create windows. Opengine lets use do
		 * compute-only applications, so the window manager is a plugin.
		 */
		Vulkan.addPlugin(new Debug(false));

		WindowManager windowManager = new WindowManager();
		Vulkan.addPlugin(windowManager);

		/**
		 * Create our Vulkan instance. We have to do this after we register the
		 * plugins so that the changes the plugins made take effect.
		 */
		Vulkan vulkan = Vulkan.initialize(vulkanConfiguration);

		/**
		 * Make a window!
		 */
		Window.Configuration windowConfiguration = new Window.Configuration();
		Window vulkanWindow = windowManager.createWindow(windowConfiguration);

		/**
		 * Go until all windows are closed.
		 */
		while (!windowManager.tick()) {
			// Do game logic here!
		}

		/**
		 * Clean up.
		 */
		vulkan.dispose();
    }
}
