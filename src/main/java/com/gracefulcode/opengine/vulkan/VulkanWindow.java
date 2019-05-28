package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFW.*;
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
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

import com.gracefulcode.opengine.ImageSet;
import com.gracefulcode.opengine.LogicalDevice;
import com.gracefulcode.opengine.PhysicalDevice;
import com.gracefulcode.opengine.Window;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import java.lang.Iterable;
import java.util.HashMap;

/**
 * A VulkanWindow wraps the (more generic) Window class and adds some
 * Vulkan-specific capabilities, such as being able to create an Image.
 *
 * @author Daniel Grace<dgrace@gracefulcode.com>
 * @version 0.1
 * @since 0.1
 */
public class VulkanWindow implements Window {
	/**
	 */
	protected long windowId;

	/**
	 * The instance of Vulkan that we're tied to.
	 */
	protected Vulkan vulkan;

	/**
	 * The surface is how Vulkan sees our window.
	 * TODO: Don't need both of these.
	 */
	protected Surface surface;
	protected PhysicalDeviceSurface physicalDeviceSurface;

	protected HashMap<ImageSet, SwapChain> swapChains = new HashMap<ImageSet, SwapChain>();
	protected SwapChain activeSwapChain;

	/**
	 * We have one swapchain per window.
	 */
	// protected SwapChain swapChain;
	// protected Pipeline pipeline;
	// protected RenderPass renderPass;

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

	VulkanWindow(Vulkan vulkan, long windowId, Iterable<VulkanPhysicalDevice> physicalDevices) {
		this.vulkan = vulkan;
		this.ib = memAllocInt(1);
		this.windowId = windowId;
		this.surface = this.vulkan.getInstance().createSurface(this.windowId);

		this.setupDevice(physicalDevices);
		// this.findPhysicalSurface();
		this.createSurface(this.physicalDevice);
	}

	/**
	 * Gets the ImageSet that represents our current framebuffer image.
	 */
	public ImageSet getFramebuffer() {
		return new VulkanWindowImageSet(
			this,
			this.physicalDeviceSurface.getCapabilities().minImageCount()
		);
	}

	public void setDisplay(ImageSet imageSet) {
		if (this.swapChains.containsKey(imageSet)) {
			this.activeSwapChain = this.swapChains.get(imageSet);
		}

		int presentMode = VK_PRESENT_MODE_MAILBOX_KHR;

		this.activeSwapChain = new SwapChain(
			this.logicalDevice,
			this.physicalDeviceSurface.getCapabilities(),
			this.physicalDeviceSurface,
			presentMode
		);
		this.swapChains.put(imageSet, this.activeSwapChain);

		System.out.println("Active swap chain: " + this.activeSwapChain);
	}

	public void dispose() {
		for (SwapChain sc: this.swapChains.values()) {
			sc.dispose();
		}
		// vkDestroySurfaceKHR(this.vulkan.getInstance(), this.surface, null);
	}

	public void close() {
		glfwHideWindow(this.windowId);
		glfwDestroyWindow(this.windowId);
		this.dispose();
	}

	protected void createSurface(VulkanPhysicalDevice physicalDevice) {
		this.physicalDeviceSurface = new PhysicalDeviceSurface(physicalDevice, this.surface);
	}

	public ImageSet getImageSet() {
		return null;
	}

	public boolean shouldClose() {
		return glfwWindowShouldClose(this.windowId);
	}

	public long getWindowId() {
		return this.windowId;
	}

	public String toString() {
		return "VulkanWindow:[id:" + this.windowId + "]";
	}

	protected void setupDevice(Iterable<VulkanPhysicalDevice> devices) {
		for (VulkanPhysicalDevice physicalDevice: devices) {
			if (physicalDevice.canDisplayToSurface(this.surface)) {
				this.physicalDevice = physicalDevice;
				return;
			}
		}
	}
}