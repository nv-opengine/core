package com.gracefulcode.opengine;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWKeyCallback;

public class Window {
	public static class Configuration {
		public String title = "";
		public int width = 800;
		public int height = 600;
		public GLFWKeyCallback keyCallback = null;
		// Clear color. Most efficient if all of {r, g, b} 0 or all 255.
		public int clearR = 0;
		public int clearG = 0;
		public int clearB = 0;
		public int clearA = 255;
	}

	protected Window.Configuration configuration;
	protected long id;

	Window(Window.Configuration configuration) {
		this.configuration = configuration;

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

		this.id = glfwCreateWindow(
			this.configuration.width,
			this.configuration.height,
			this.configuration.title,
			0,
			0
		);

		if (this.configuration.keyCallback != null) {
			glfwSetKeyCallback(this.id, this.configuration.keyCallback);
		}

		glfwShowWindow(this.id);
	}

	public ImageSet getImageSet() {
		return null;
	}

	public long getId() {
		return this.id;
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(this.id);
	}

	public void close() {
		glfwHideWindow(this.id);
		glfwDestroyWindow(this.id);
	}
}