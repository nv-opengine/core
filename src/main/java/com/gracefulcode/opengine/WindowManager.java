package com.gracefulcode.opengine;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;

public class WindowManager {
	public static class Configuration {
		public boolean exitAfterLastWindow = true;
		public Window.Configuration defaultWindowConfiguration = new Window.Configuration();
	}

	protected WindowManager.Configuration configuration;
	protected ArrayList<Window> openWindows = new ArrayList<Window>();;

	public WindowManager() {
		this(new WindowManager.Configuration());
	}

	public WindowManager(WindowManager.Configuration configuration) {
		this.configuration = configuration;
	}

	public Window createWindow() {
		return this.createWindow(this.configuration.defaultWindowConfiguration);
	}

	public Window createWindow(Window.Configuration configuration) {
		Window window = new Window(configuration);
		this.openWindows.add(window);
		return window;
	}

	public boolean tick() {
		glfwPollEvents();

		// TODO: If our window was focused and is now being closed, we should focus another one of our windows.
		// TODO: We should maybe let the user control this since we're in engine code here.
		for (int i = this.openWindows.size() - 1; i >= 0; i--) {
			Window w = this.openWindows.get(i);
			if (w.shouldClose()) {
				this.openWindows.remove(i);
				w.close();
			}
		}

		if (this.openWindows.isEmpty() && this.configuration.exitAfterLastWindow) return true;
		return false;
	}
}