package com.gracefulcode.opengine.v2.vulkan;

import static org.lwjgl.glfw.GLFW.*;

public class Window implements com.gracefulcode.opengine.v2.Window {
	protected Vulkan vulkan;
	protected long id;

	public Window(long id) {
		this.vulkan = Vulkan.get();
		this.id = id;
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(this.id);
	}

	public void close() {

	}

	public void dispose() {

	}
}