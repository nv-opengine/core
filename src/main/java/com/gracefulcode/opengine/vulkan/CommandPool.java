package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.annotations.ThreadInfo;

import java.nio.LongBuffer;
import java.util.ArrayList;

import org.lwjgl.PointerBuffer;
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
	protected VulkanLogicalDevice logicalDevice;
	// protected ArrayList<CommandBuffer> commandBuffers = new ArrayList<CommandBuffer>();

	CommandPool(VulkanLogicalDevice logicalDevice, int queueIndex) {
		this.logicalDevice = logicalDevice;

		LongBuffer pCmdPool = memAllocLong(1);

		VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
			.queueFamilyIndex(queueIndex)
			.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
		int err = vkCreateCommandPool(this.logicalDevice.getDevice(), cmdPoolInfo, null, pCmdPool);
		this.commandPool = pCmdPool.get(0);
		cmdPoolInfo.free();

		memFree(pCmdPool);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Could not create command pool: " + Vulkan.translateVulkanResult(err));
		}
	}

	// public CommandBuffer getCommandBuffer() {
	// 	CommandBuffer cb = new CommandBuffer(this.logicalDevice, this.commandPool);
	// 	this.commandBuffers.add(cb);
	// 	return cb;
	// }

	public long getId() {
		return this.commandPool;
	}
}