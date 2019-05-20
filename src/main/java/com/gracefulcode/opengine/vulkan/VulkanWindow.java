package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.VkPhysicalDevice;
// import org.lwjgl.vulkan.VkRenderPass;

import com.gracefulcode.opengine.Window;

import java.nio.LongBuffer;

/**
 * A VulkanWindow wraps the (more generic) Window class and adds some
 * Vulkan-specific capabilities, such as being able to create an Image.
 *
 * @author Daniel Grace<dgrace@gracefulcode.com>
 * @version 0.1
 * @since 0.1
 */
public class VulkanWindow {
	/**
	 * The non-vulkan-specific Window.
	 */
	protected Window window;

	/**
	 * The instance of Vulkan that we're tied to.
	 */
	protected Vulkan vulkan;

	/**
	 * The surface is how Vulkan sees our window.
	 */
	protected long surface;

	/**
	 * We have one swapchain per window.
	 */
	protected SwapChain swapChain;

	/**
	 * The physical device backing this window.
	 */
	protected VkPhysicalDevice physicalDevice;

	// protected VkSupportCapabilitiesKHR capabilities;

	/**
	 * This is the main thing that we're abstracting here. Each window has a
	 * single RenderPass that we're setting up and then executing.
	 */
	// protected VkRenderPass renderPass;

	VulkanWindow(Window window, Vulkan vulkan) {
		this.window = window;
		this.vulkan = vulkan;

		LongBuffer lb = memAllocLong(1);
		int err;

		if ((err = glfwCreateWindowSurface(this.vulkan.getInstance(), this.window.getId(), null, lb)) != VK_SUCCESS) {
			throw new AssertionError("Could not create surface: " + Vulkan.translateVulkanResult(err));
		}
		this.surface = lb.get(0);

		this.physicalDevice = this.vulkan.findPhysicalDeviceForSurface(this.surface);
		System.out.println("Window chose: " + this.physicalDevice);

		this.swapChain = new SwapChain();
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