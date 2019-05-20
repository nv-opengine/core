package com.gracefulcode.opengine.vulkan;

import com.gracefulcode.opengine.Window;

/**
 * A VulkanWindow wraps the (more generic) Window class and adds some
 * Vulkan-specific capabilities, such as being able to create an Image.
 */
public class VulkanWindow {
	protected Window window;
	protected Vulkan vulkan;

	public VulkanWindow(Window window, Vulkan vulkan) {
		this.window = window;
		this.vulkan = vulkan;
	}

	/**
	 * Create a generic Image that is configured for the framebuffer of this
	 * window. That means that it will be suitable for presentation (display),
	 * and will be the size of the window.
	 *
	 * When the window is resized, this image will automatically handle that
	 * gracefully.
	 */
	public Image createFramebufferImage() {
		return null;
	}

	// TODO: Do this.
	public void display(Image image) {

	}
}