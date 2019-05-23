package com.gracefulcode.opengine;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;

public class WindowManager<W extends Window, WC extends WindowCreator<W>> {
	public static class Configuration {
		public boolean exitAfterLastWindow = true;
		public Window.Configuration defaultWindowConfiguration = new Window.Configuration();
	}

	protected WindowManager.Configuration configuration;
	protected ArrayList<W> openWindows = new ArrayList<W>();
	protected WC windowCreator;

	public static void init() {
		if (!glfwInit()) {
			throw new RuntimeException("Failed to initialize GLFW");
		}
	}

	public WindowManager(WC windowCreator) {
		this(new WindowManager.Configuration(), windowCreator);
	}

	public WindowManager(WindowManager.Configuration configuration, WC windowCreator) {
		this.configuration = configuration;
		this.windowCreator = windowCreator;
	}

	public W createWindow() {
		return this.createWindow(this.configuration.defaultWindowConfiguration);
	}

	public W createWindow(Window.Configuration configuration) {
		W window = this.windowCreator.createWindow(configuration);
		this.openWindows.add(window);
		return window;
	}

	public boolean tick() {
		glfwPollEvents();

		// TODO: If our window was focused and is now being closed, we should focus another one of our windows.
		// TODO: We should maybe let the user control this since we're in engine code here.
		for (int i = this.openWindows.size() - 1; i >= 0; i--) {
			W w = this.openWindows.get(i);
			if (w.shouldClose()) {
				this.openWindows.remove(i);
				w.close();
			}
		}

		if (this.openWindows.isEmpty() && this.configuration.exitAfterLastWindow) return true;
		return false;
	}
}