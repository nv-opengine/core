package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.PointerBuffer;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
// import org.lwjgl.vulkan.VkRenderPass;

import com.gracefulcode.opengine.LogicalDevice;
import com.gracefulcode.opengine.PhysicalDevice;
import com.gracefulcode.opengine.Window;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
	protected Pipeline pipeline;
	protected RenderPass renderPass;

	protected String[] requiredExtensions = {
		VK_KHR_SWAPCHAIN_EXTENSION_NAME
	};

	/**
	 * The physical device backing this window.
	 */
	protected VulkanPhysicalDevice physicalDevice;
	protected VulkanLogicalDevice logicalDevice;

	protected IntBuffer ib;

	/**
	 * This is the main thing that we're abstracting here. Each window has a
	 * single RenderPass that we're setting up and then executing.
	 */
	// protected VkRenderPass renderPass;

	VulkanWindow(Window window, Vulkan vulkan) {
		this.window = window;
		this.vulkan = vulkan;
		this.ib = memAllocInt(1);

		this.createSurface();
		this.findPhysicalSurface();
	}

	protected void createSurface() {
		LongBuffer lb = memAllocLong(1);
		int err;

		if ((err = glfwCreateWindowSurface(this.vulkan.getInstance(), this.window.getId(), null, lb)) != VK_SUCCESS) {
			throw new AssertionError("Could not create surface: " + Vulkan.translateVulkanResult(err));
		}
		this.surface = lb.get(0);
		memFree(lb);
	}

	protected VulkanPhysicalDevice findPhysicalDeviceForSurface() {
		System.out.println("Finding physical device for surface (window)");

		for (VulkanPhysicalDevice device: this.vulkan.getPhysicalDevices()) {
			if (this.deviceIsSuitableForSurface(device)) {
				return device;
			}
		}

		return null;
	}

	/**
	 * TODO: Find some way to let the user choose which device based on these qualities. Right now
	 * they cannot filter based on these.
	 */
	protected boolean deviceIsSuitableForSurface(VulkanPhysicalDevice physicalDevice) {
		if (physicalDevice.canDisplayToSurface(this.surface)) return true;
		return false;
	}

	protected void findPhysicalSurface() {
		this.physicalDevice = this.findPhysicalDeviceForSurface();
		this.logicalDevice = this.physicalDevice.createLogicalDevice(this.requiredExtensions, true, false);

		// this.memoryManager = new MemoryManager(this.logicalDevice);

		this.swapChain = new SwapChain(this.logicalDevice, this.physicalDevice.getSurface(this.surface));
		this.pipeline = new Pipeline(this.swapChain, this.logicalDevice);
		this.renderPass = new RenderPass(this.swapChain, this.logicalDevice);
	}
}