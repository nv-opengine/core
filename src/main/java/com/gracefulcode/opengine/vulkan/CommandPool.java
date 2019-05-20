package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.annotations.ThreadInfo;

import java.nio.LongBuffer;
import java.util.ArrayList;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;

/**
 * Command pools are opaque objects that command buffer memory is allocated
 * from, and which allow the implementation to amortize the cost of resource
 * creation across multiple command buffers. Command pools are externally
 * synchronized, meaning that a command pool must not be used concurrently in
 * multiple threads. That includes use via recording commands on any command
 * buffers allocated from the pool, as well as operations that allocate, free,
 * and reset command buffers or the pool itself.
 */
@ThreadInfo( perThread = true )
public class CommandPool {
	protected long commandPool;
	protected VkDevice logicalDevice;
	protected ArrayList<Long> commandBuffers = new ArrayList<Long>();

	CommandPool(VkDevice logicalDevice, int queueIndex) {
		this.logicalDevice = logicalDevice;

		LongBuffer pCmdPool = memAllocLong(1);

		VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
			.queueFamilyIndex(queueIndex)
			.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
		int err = vkCreateCommandPool(this.logicalDevice, cmdPoolInfo, null, pCmdPool);
		this.commandPool = pCmdPool.get(0);
		cmdPoolInfo.free();

		memFree(pCmdPool);
	}

	/**
	 * TODO: CommandBuffer should be a class.
	 */
	public long getCommandBuffer() {
		PointerBuffer pCommandBuffer = memAllocPointer(1);

		VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
			.commandPool(commandPool)
			.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
			.commandBufferCount(1);
		int err = vkAllocateCommandBuffers(this.logicalDevice, cmdBufAllocateInfo, pCommandBuffer);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to allocate command buffer: " + Vulkan.translateVulkanResult(err));
		}

		cmdBufAllocateInfo.free();
		long commandBuffer = pCommandBuffer.get(0);
		this.commandBuffers.add(commandBuffer);

		memFree(pCommandBuffer);
		return commandBuffer;
	}
}