package com.gracefulcode.opengine.v2.vulkan;

import static org.lwjgl.glfw.GLFW.*;

public class Window implements com.gracefulcode.opengine.v2.Window {
	protected Vulkan vulkan;
	protected long id;
	protected Surface surface;

	public Window(long id) {
		this.vulkan = Vulkan.get();
		this.id = id;
		this.surface = new Surface(this.id, this.vulkan);
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(this.id);
	}

	public void close() {

	}

	public void dispose() {

	}
}