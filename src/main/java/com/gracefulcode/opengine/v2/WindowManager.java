package com.gracefulcode.opengine.v2;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;

/**
 * A window manager handles all of the open windows, creating new windows, and
 * telling when all windows are closed.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1
 */
public class WindowManager<W extends Window, WC extends WindowCreator<W>> {
	/**
	 * The configuration for WindowManagers.
	 *
	 * @author Daniel Grace <dgrace@gracefulcode.com>
	 * @version 0.1.1
	 * @since 0.1
	 */
	public static class Configuration {
		/**
		 * If we ever go down to zero open windows, should the application
		 * close?
		 */
		public boolean exitAfterLastWindow = true;
	}

	/**
	 * The configuration that this WindowManager is using.
	 */
	protected Configuration configuration;

	/**
	 * What we use to create new windows.
	 */
	protected WC windowCreator;

	/**
	 * All open windows.
	 */
	protected ArrayList<W> openWindows = new ArrayList<W>();

	public WindowManager(WC windowCreator) {
		this(windowCreator, new Configuration());
	}

	public WindowManager(WC windowCreator, Configuration configuration) {
		this.configuration = configuration;
		this.windowCreator = windowCreator;
	}

	/**
	 * Create a window with the configured default window configuration.
	 */
	public W createWindow() {
		return this.createWindow(this.configuration.defaultWindowConfiguration);
	}

	/**
	 * Create a window with a custom window configuration.
	 */
	public W createWindow(Window.Configuration configuration) {
		W window = this.windowCreator.createWindow(configuration);
		this.openWindows.add(window);
		return window;
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
			W w = this.openWindows.get(i);
			if (w.shouldClose()) {
				this.openWindows.remove(i);
				w.close();
				w.dispose();
			}
		}

		if (this.openWindows.isEmpty() && this.configuration.exitAfterLastWindow) return true;
		return false;
	}

	/**
	 * Free memory associated with this instance.
	 */
	public void dispose() {
		for (Window w: this.openWindows) {
			w.close();
			w.dispose();
		}
	}
}