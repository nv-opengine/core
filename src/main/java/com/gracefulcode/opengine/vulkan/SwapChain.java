package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

public class SwapChain {
	protected VkDevice logicalDevice;

	public SwapChain(VkDevice logicalDevice, long surface) {
		this.logicalDevice = logicalDevice;

		LongBuffer lb = memAllocLong(1);
		IntBuffer ib = memAllocInt(0);

		VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.calloc();
		vkGetPhysicalDeviceSurfaceCapabilitiesKHR(this.logicalDevice.getPhysicalDevice(), surface, capabilities);

		VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc();
		createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
		createInfo.surface(surface);
		createInfo.minImageCount(capabilities.minImageCount());
		createInfo.imageFormat(VK_FORMAT_B8G8R8A8_UNORM);
		createInfo.imageColorSpace(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);
		createInfo.imageExtent(capabilities.currentExtent());
		createInfo.imageArrayLayers(1);
		// TODO: Is this always this? I think so...
		// Might want to | with transfer destination or something...
		createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
		// TODO: If the queues are different, we need to handle that.
		createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
		createInfo.pQueueFamilyIndices(ib);
		createInfo.preTransform(capabilities.currentTransform());
		// TODO: Make sure supported, also be able to do others.
		createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);

		// TODO: Mac doesn't support this. Deal with negotiation. See code in Vulkan.
		createInfo.presentMode(VK_PRESENT_MODE_MAILBOX_KHR);
		createInfo.clipped(true);
		createInfo.oldSwapchain(0);

		int err = vkCreateSwapchainKHR(
			this.logicalDevice,
			createInfo,
			null,
			lb
		);
		memFree(ib);
		memFree(lb);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Error creating swapchain: " + Vulkan.translateVulkanResult(err));
		}
	}
}