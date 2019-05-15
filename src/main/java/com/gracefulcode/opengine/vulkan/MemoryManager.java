package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.lang.Math;
import java.nio.LongBuffer;
import java.util.ArrayList;

import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryRequirements;

/**
 * TODO: Actually allocate the memory once we're done asking for types of memory.
 * 
 */
public class MemoryManager {
	public static class Buffer {
		protected long id;
		protected VkDevice logicalDevice;
		protected VkMemoryRequirements requirements;

		public Buffer(VkDevice logicalDevice, int bytes, int usage, int sharingMode) {
			this.logicalDevice = logicalDevice;

			VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc();
			bufferCreateInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
			bufferCreateInfo.size(bytes);
			bufferCreateInfo.usage(usage);
			bufferCreateInfo.sharingMode(sharingMode);

			LongBuffer longBuffer = memAllocLong(1);
			int err = vkCreateBuffer(this.logicalDevice, bufferCreateInfo, null, longBuffer);
			bufferCreateInfo.free();

			this.id = longBuffer.get(0);
			memFree(longBuffer);

			this.requirements = VkMemoryRequirements.calloc();
			vkGetBufferMemoryRequirements(this.logicalDevice, this.id, this.requirements);

			System.out.println("Alignment: " + this.requirements.alignment());
			System.out.println("Memory Type: " + Integer.toBinaryString(this.requirements.memoryTypeBits()));
			System.out.println("Asked Size: " + bytes);
			System.out.println("Size: " + this.requirements.size());
			System.out.println("Effective Size: " + this.getEffectiveSize());
		}

		public long getEffectiveSize() {
			return (long)(Math.ceil(this.requirements.size() / (float)this.requirements.alignment())) * this.requirements.alignment();
		}
	}

	public static class StorageBuffer extends MemoryManager.Buffer {
		public StorageBuffer(VkDevice logicalDevice, int bytes, int sharingMode) {
			super(logicalDevice, bytes, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, sharingMode);
		}
	}

	public static class ExclusiveStorageBuffer extends StorageBuffer {
		public ExclusiveStorageBuffer(VkDevice logicalDevice, int bytes) {
			super(logicalDevice, bytes, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
		}
	}

	protected ArrayList<Buffer> buffers = new ArrayList<Buffer>();
	protected VkDevice logicalDevice;

	public MemoryManager(VkDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
	}

	public Buffer createExclusiveComputeBuffer(int bytes) {
		ExclusiveStorageBuffer buffer = new ExclusiveStorageBuffer(this.logicalDevice, bytes);
		return buffer;
	}
}