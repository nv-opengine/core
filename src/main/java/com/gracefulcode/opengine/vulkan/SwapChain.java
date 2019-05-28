package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.ImageSet;
import com.gracefulcode.opengine.ImageView;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

public class SwapChain {
	/**
	 * One of these per swapchain image, basically.
	 */
	public static class PresentationRequest {
		VulkanImageView imageView;
		long commandBuffer;

		public PresentationRequest(VulkanImageView imageView, long commandBuffer) {
			this.imageView = imageView;
			this.commandBuffer = commandBuffer;
		}

		public long getCommandBuffer() {
			return this.commandBuffer;
		}
	}

	protected long id;
	// protected VkSurfaceFormatKHR surfaceFormat;

	protected VulkanLogicalDevice logicalDevice;
	protected VkSurfaceCapabilitiesKHR capabilities;
	protected HashMap<Long, PresentationRequest> presentationRequests = new HashMap<Long, PresentationRequest>();
	protected CommandPool graphicsPool;
	protected CommandPool computePool;
	protected Pipeline pipeline;
	protected RenderPass renderPass;
	protected ImageSet imageSet;
	// protected VulkanWindowImageSet imageSet;

	public SwapChain(VulkanLogicalDevice logicalDevice, VkSurfaceCapabilitiesKHR capabilities, PhysicalDeviceSurface physicalDeviceSurface, int presentMode) {
		this.logicalDevice = logicalDevice;
		this.capabilities = capabilities;

		// TODO: 0 isn't guaranteed to have graphics!
		this.graphicsPool = new CommandPool(this.logicalDevice, 0);

		LongBuffer lb = memAllocLong(1);
		IntBuffer ib = memAllocInt(0);

		VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc();
		createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
		createInfo.surface(physicalDeviceSurface.getSurface().getId());
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

		System.out.println("Logical device: " + physicalDeviceSurface.getSurface().getId());
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

		if (err != VK_SUCCESS) {
			throw new AssertionError("Error creating swapchain: " + Vulkan.translateVulkanResult(err));
		}

		ib = memAllocInt(1);
		vkGetSwapchainImagesKHR(this.logicalDevice.getDevice(), this.id, ib, null);
		lb = memAllocLong(ib.get(0));
		vkGetSwapchainImagesKHR(this.logicalDevice.getDevice(), this.id, ib, lb);

		VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc();
		allocateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
		allocateInfo.commandPool(this.graphicsPool.getId());
		allocateInfo.commandBufferCount(ib.get(0));
		allocateInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);

		PointerBuffer pb = memAllocPointer(ib.get(0));
		err = vkAllocateCommandBuffers(this.logicalDevice.getDevice(), allocateInfo, pb);
		
		if (err != VK_SUCCESS) {
			throw new AssertionError("Error creating command buffers: " + Vulkan.translateVulkanResult(err));
		}

		for (int i = 0; i < ib.get(0); i++) {
			long l = lb.get(i);
			VulkanImageView imageView = new VulkanImageView(this.logicalDevice, l, VK_FORMAT_B8G8R8A8_UNORM);

			PresentationRequest pr = new PresentationRequest(imageView, pb.get(i));
			this.presentationRequests.put(imageView.getId(), pr);
		}

		memFree(ib);
		memFree(lb);

		this.pipeline = new Pipeline(this, this.logicalDevice);
		this.renderPass = new RenderPass(this, this.logicalDevice);
	}

	/**
	 * TODO: Fix.
	 */
	public int getWidth() {
		return 0;
	}

	/**
	 * TODO: Fix.
	 */
	public int getHeight() {
		return 0;
	}

	public void populateSwapChain(ImageSet imageSet) {
		if (this.imageSet != null) throw new AssertionError("Cannot set imageSet for a SwapChain that already has one.");

		this.imageSet = imageSet;

		VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc();
		cmdBufInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
		cmdBufInfo.pNext(NULL);

		VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc();
		renderPassBeginInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
		renderPassBeginInfo.pNext(NULL);
		renderPassBeginInfo.renderPass(this.renderPass.getId());
		renderPassBeginInfo.pClearValues();

		VkRect2D renderArea = renderPassBeginInfo.renderArea();
		renderArea.offset().x(0).y(0);
		renderArea.extent(this.capabilities.currentExtent());

		for (PresentationRequest pr: this.presentationRequests.values()) {
			// renderPassBeginInfo.framebuffer(pr.framebuffer);

			// int err = vkBeginCommandBuffer(pr.commandBuffer, cmdBufInfo);
			// if (err != VK_SUCCESS) {
			// 	throw new AssertionError("Could not create command buffer: " + Vulkan.translateVulkanResult(err));
			// }
		}
	}

	public int getSize() {
		return this.presentationRequests.size();
	}

	public int getImageFormat() {
		return VK_FORMAT_B8G8R8A8_UNORM;
	}

	public VkExtent2D getExtent() {
		return this.capabilities.currentExtent();
	}

	public void dispose() {
		this.capabilities.free();
		vkDestroySwapchainKHR(this.logicalDevice.getDevice(), this.id, null);
	}
}