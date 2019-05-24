package com.gracefulcode.opengine;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;

/**
 * The WindowManager is responsible for keeping track of what windows exist,
 * displaying them, and keeping track of when they close. It is also how you
 * create new windows, knowing how to create them.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1
 * @since 0.1
 */
public class WindowManager<W extends Window, WC extends WindowCreator<W>> {
	/**
	 * The WindowManager.Configuration controls how our WindowManager behaves.
	 * It also holds the default Window.Configuration that will be used if you
	 * create a Window without an explicit configuration.
	 *
	 * @author Daniel Grace <dgrace@gracefulcode.com>
	 * @version 0.1
	 * @since 0.1
	 */
	public static class Configuration {
		/**
		 * When the last window is closed, we don't necessarily have to end.
		 */
		public boolean exitAfterLastWindow = true;

		/**
		 * Default configuration if we don't provide one when creating a Window.
		 */
		public Window.Configuration defaultWindowConfiguration = new Window.Configuration();
	}

	/**
	 * Our current configuration. This should not ever be changed after
	 * creation, as the assumption is that we never read it after. We might,
	 * but I don't want to make any guarantees about that. In general, things
	 * that are logically changeable after the fact should expose a setter.
	 */
	protected WindowManager.Configuration configuration;

	/**
	 * The windows that are currently open. When this list is empty, IF our
	 * configuration says so, we tell the app to close.
	 */
	protected ArrayList<W> openWindows = new ArrayList<W>();

	/**
	 * Though we have no current plans, we are supporting a bit of flexibility
	 * here. We get a WindowCreator instead of creating our own windows.
	 */
	protected WC windowCreator;

	/**
	 * Because of glfw, we need to do this once. This is curently where we're
	 * doing it. I don't actually like this, but haven't figured out/made it
	 * better. The problem is that it's currently being punted to the end user
	 * to do this and it needs to be done before we can do anything Vulkan.
	 */
	public static void init() {
		if (!glfwInit()) {
			throw new RuntimeException("Failed to initialize GLFW");
		}
	}

	/**
	 * Create a window manager with the given creator and a default
	 * configuration.
	 */
	public WindowManager(WC windowCreator) {
		this(new WindowManager.Configuration(), windowCreator);
	}

	/**
	 * Create a window manager with the given creator and a custom
	 * configuration.
	 */
	public WindowManager(WindowManager.Configuration configuration, WC windowCreator) {
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