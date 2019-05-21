package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDescription;

/**
 * A render pass is not tied to any <b>specific</b> image, but does need to
 * know what format the images are going to be in.
 */
public class RenderPass {
	public RenderPass(SwapChain swapChain, VulkanLogicalDevice logicalDevice) {
		VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1);
		colorAttachment.format(swapChain.getImageFormat());
		colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
		colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
		colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
		colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
		colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
		colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
		colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

		VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1);
		colorAttachmentRef.attachment(0);
		colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

		VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1);
		subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
		subpass.pColorAttachments(colorAttachmentRef);

		VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc();
		renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
		renderPassInfo.pAttachments(colorAttachment);
		renderPassInfo.pSubpasses(subpass);

		LongBuffer lb = memAllocLong(1);
		int err = vkCreateRenderPass(logicalDevice.getDevice(), renderPassInfo, null, lb);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Error creating render pass: " + Vulkan.translateVulkanResult(err));
		}

		System.out.println("Created render pass: " + lb.get(0));
	}
}