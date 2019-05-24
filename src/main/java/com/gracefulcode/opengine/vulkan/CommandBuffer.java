package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.ImageSet;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;


/**
 * There is a command buffer per rendering setup.
 * In the underlying implementation, there is one per rendering setup <b>per
 * frame</b>, but we try not to expose the per-frame elements to the user.
 */
public class CommandBuffer {
	protected long id;
	protected VulkanLogicalDevice logicalDevice;

	public CommandBuffer(VulkanLogicalDevice logicalDevice, long commandPool) {
		this.logicalDevice = logicalDevice;

		PointerBuffer pCommandBuffer = memAllocPointer(1);

		VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
			.commandPool(commandPool)
			.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
			.commandBufferCount(1);
		int err = vkAllocateCommandBuffers(this.logicalDevice.getDevice(), cmdBufAllocateInfo, pCommandBuffer);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to allocate command buffer: " + Vulkan.translateVulkanResult(err));
		}

		cmdBufAllocateInfo.free();
		this.id = pCommandBuffer.get(0);

		memFree(pCommandBuffer);
	}
}