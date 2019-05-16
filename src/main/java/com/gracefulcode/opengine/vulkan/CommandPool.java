package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;

public class CommandPool {
	protected long commandPool;
	protected long commandBuffer;
	protected VkDevice logicalDevice;

	CommandPool(VkDevice logicalDevice, int queueIndex) {
		this.logicalDevice = logicalDevice;

		LongBuffer pCmdPool = memAllocLong(1);
		PointerBuffer pCommandBuffer = memAllocPointer(1);

		VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
			.queueFamilyIndex(queueIndex)
			.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
		int err = vkCreateCommandPool(this.logicalDevice, cmdPoolInfo, null, pCmdPool);
		this.commandPool = pCmdPool.get(0);
		cmdPoolInfo.free();

		VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
			.commandPool(commandPool)
			.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
			.commandBufferCount(1);
		err = vkAllocateCommandBuffers(this.logicalDevice, cmdBufAllocateInfo, pCommandBuffer);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to allocate command buffer: " + Vulkan.translateVulkanResult(err));
		}

		cmdBufAllocateInfo.free();
		this.commandBuffer = pCommandBuffer.get(0);

		memFree(pCmdPool);
		memFree(pCommandBuffer);
	}
}