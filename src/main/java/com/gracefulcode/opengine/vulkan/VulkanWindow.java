package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.PointerBuffer;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
// import org.lwjgl.vulkan.VkRenderPass;

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

	protected String[] requiredExtensions = {
		VK_KHR_SWAPCHAIN_EXTENSION_NAME
	};

	/**
	 * The physical device backing this window.
	 */
	protected VkPhysicalDevice physicalDevice;
	protected VkDevice logicalDevice;

	protected IntBuffer ib;

	// TODO: Do we wrap these?
	protected int graphicsQueueIndex = -1;
	protected int computeQueueIndex = -1;

	// protected VkSupportCapabilitiesKHR capabilities;

	/**
	 * This is the main thing that we're abstracting here. Each window has a
	 * single RenderPass that we're setting up and then executing.
	 */
	// protected VkRenderPass renderPass;

	VulkanWindow(Window window, Vulkan vulkan) {
		this.window = window;
		this.vulkan = vulkan;
		this.ib = memAllocInt(1);

		LongBuffer lb = memAllocLong(1);
		int err;

		if ((err = glfwCreateWindowSurface(this.vulkan.getInstance(), this.window.getId(), null, lb)) != VK_SUCCESS) {
			throw new AssertionError("Could not create surface: " + Vulkan.translateVulkanResult(err));
		}
		this.surface = lb.get(0);

		this.physicalDevice = this.vulkan.findPhysicalDeviceForSurface(this.surface);
		System.out.println("Window chose: " + this.physicalDevice);

		vkGetPhysicalDeviceQueueFamilyProperties(this.physicalDevice, this.ib, null);
		int queueCount = this.ib.get(0);
		System.out.println("For window, queue count is: " + queueCount);

		VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
		vkGetPhysicalDeviceQueueFamilyProperties(this.physicalDevice, this.ib, queueProps);

		int queueFamilyIndex;
		for (queueFamilyIndex = 0; queueFamilyIndex < queueCount; queueFamilyIndex++) {
			VkQueueFamilyProperties properties = queueProps.get(queueFamilyIndex);
			int flags = properties.queueFlags();

			if ((flags & VK_QUEUE_GRAPHICS_BIT) != 0)
				this.graphicsQueueIndex = queueFamilyIndex;
			if ((flags & VK_QUEUE_COMPUTE_BIT) != 0)
				this.computeQueueIndex = queueFamilyIndex;
		}

		// TODO: Do we ever want to make multiple in order to use different
		// priorities? Do we want to expose that to the user?
		System.out.println("Graphics: " + this.graphicsQueueIndex);
		VkQueueFamilyProperties properties = queueProps.get(this.graphicsQueueIndex);
		System.out.println("\tMax Queues: " + properties.queueCount());

		System.out.println("Compute: " + this.computeQueueIndex);
		properties = queueProps.get(this.computeQueueIndex);
		System.out.println("\tMax Queues: " + properties.queueCount());

		if (this.graphicsQueueIndex == -1) {
			throw new AssertionError("Cannot create a graphics queue, but using a VulkanWindow.");
		}

		// TODO: When/if we have multiple queues, the priorities are actually important.
		// For now they mean nothing.
		FloatBuffer pQueuePriorities = memAllocFloat(1).put(0.0f);
		pQueuePriorities.flip();

		VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1);

		queueCreateInfo.position(0);
		queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
		queueCreateInfo.queueFamilyIndex(this.graphicsQueueIndex);
		queueCreateInfo.pQueuePriorities(pQueuePriorities);

		PointerBuffer ppEnabledExtensionNames = memAllocPointer(this.requiredExtensions.length);
		for (int i = 0; i < this.requiredExtensions.length; i++) {
			ppEnabledExtensionNames.put(memUTF8(requiredExtensions[i]));
		}
		ppEnabledExtensionNames.flip();

		VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
			.pNext(NULL)
			.pQueueCreateInfos(queueCreateInfo)
			.ppEnabledExtensionNames(ppEnabledExtensionNames)
			.ppEnabledLayerNames(null);

		PointerBuffer pDevice = memAllocPointer(1);
		err = vkCreateDevice(this.physicalDevice, deviceCreateInfo, null, pDevice);
		long device = pDevice.get(0);
		this.logicalDevice = new VkDevice(device, this.physicalDevice, deviceCreateInfo);
		// this.memoryManager = new MemoryManager(this.logicalDevice);

		this.swapChain = new SwapChain(this.logicalDevice, this.surface);
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