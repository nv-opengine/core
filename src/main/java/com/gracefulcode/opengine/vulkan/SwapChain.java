package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.ImageView;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import java.util.ArrayList;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

public class SwapChain {
	protected VulkanLogicalDevice logicalDevice;
	protected long id;
	protected ArrayList<VulkanImageView> imageViews = new ArrayList<VulkanImageView>();
	protected VkSurfaceCapabilitiesKHR capabilities;

	public SwapChain(VulkanLogicalDevice logicalDevice, long surface) {
		this.logicalDevice = logicalDevice;

		LongBuffer lb = memAllocLong(1);
		IntBuffer ib = memAllocInt(0);

		this.capabilities = VkSurfaceCapabilitiesKHR.calloc();
		vkGetPhysicalDeviceSurfaceCapabilitiesKHR(this.logicalDevice.getPhysicalDevice().getDevice(), surface, this.capabilities);

		VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc();
		createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
		createInfo.surface(surface);
		createInfo.minImageCount(this.capabilities.minImageCount());
		createInfo.imageFormat(this.getImageFormat());
		createInfo.imageColorSpace(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);
		createInfo.imageExtent(this.capabilities.currentExtent());
		createInfo.imageArrayLayers(1);
		// TODO: Is this always this? I think so...
		// Might want to | with transfer destination or something...
		createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
		// TODO: If the queues are different, we need to handle that.
		createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
		createInfo.pQueueFamilyIndices(ib);
		createInfo.preTransform(this.capabilities.currentTransform());
		// TODO: Make sure supported, also be able to do others.
		createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);

		// TODO: Mac doesn't support this. Deal with negotiation. See code in Vulkan.
		createInfo.presentMode(VK_PRESENT_MODE_MAILBOX_KHR);
		createInfo.clipped(true);
		createInfo.oldSwapchain(0);

		int err = vkCreateSwapchainKHR(
			this.logicalDevice.getDevice(),
			createInfo,
			null,
			lb
		);
		createInfo.free();

		this.id = lb.get(0);

		memFree(ib);
		memFree(lb);

		ib = memAllocInt(1);
		vkGetSwapchainImagesKHR(this.logicalDevice.getDevice(), this.id, ib, null);
		lb = memAllocLong(ib.get(0));
		vkGetSwapchainImagesKHR(this.logicalDevice.getDevice(), this.id, ib, lb);
		
		for (int i = 0; i < ib.get(0); i++) {
			long l = lb.get(i);
			VulkanImageView imageView = new VulkanImageView(this.logicalDevice, l, VK_FORMAT_B8G8R8A8_UNORM);
			this.imageViews.add(imageView);
		}

		memFree(ib);
		memFree(lb);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Error creating swapchain: " + Vulkan.translateVulkanResult(err));
		}

		System.out.println("Created swapchain: " + this.id);
	}

	public int getImageFormat() {
		return VK_FORMAT_B8G8R8A8_UNORM;
	}

	public VkExtent2D getExtent() {
		return this.capabilities.currentExtent();
	}

	public void dispose() {
		this.capabilities.free();
	}
}