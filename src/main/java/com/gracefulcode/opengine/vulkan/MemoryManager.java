package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.lang.Math;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryHeap;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkMemoryType;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

/**
 * TODO: Actually allocate the memory once we're done asking for types of memory.
 * 
 */
public class MemoryManager {
	public static class Buffer {
		protected long id;
		protected VkDevice logicalDevice;
		protected VkMemoryRequirements requirements;
		protected int optimalMemoryFlags;
		protected int requiredMemoryFlags;
		protected int memoryTypeId;
		protected int descriptorType;
		protected String name;

		// Used when binding. Not valid until we actually allocate memory
		// and segment things up.
		// TODO: When that happens, fill this out.
		protected VkDescriptorBufferInfo bufferInfo;

		public Buffer(
			String name,
			VkDevice logicalDevice,
			int bytes,
			int usage,
			int sharingMode,
			int optimalMemoryFlags,
			int requiredMemoryFlags,
			int descriptorType
		) {
			this.name = name;
			this.logicalDevice = logicalDevice;
			this.optimalMemoryFlags = optimalMemoryFlags;
			this.requiredMemoryFlags = requiredMemoryFlags;
			this.descriptorType = descriptorType;

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

		public int getDescriptorType() {
			return this.descriptorType;
		}

		public long getId() {
			return this.id;
		}

		public void setMemoryTypeId(int id) {
			this.memoryTypeId = id;
		}

		public int getMemoryTypeId() {
			return this.memoryTypeId;
		}

		public VkDescriptorBufferInfo getBufferInfo() {
			if (this.bufferInfo == null) {
				this.bufferInfo = VkDescriptorBufferInfo.calloc();
				this.bufferInfo.buffer(this.id);
				this.bufferInfo.offset(0);
				this.bufferInfo.range(this.requirements.size());
			}
			return this.bufferInfo;
		}

		public long getEffectiveSize() {
			return (long)(Math.ceil(this.requirements.size() / (float)this.requirements.alignment())) * this.requirements.alignment();
		}

		public boolean isOptimal(int id, int flags) {
			if ((this.requirements.memoryTypeBits() & (1 << id)) == 0) return false;
			if ((flags & this.optimalMemoryFlags) == flags) return true;
			return false;
		}

		public boolean isAcceptable(int id, int flags) {
			if ((this.requirements.memoryTypeBits() & (1 << id)) == 0) return false;
			if ((flags & this.requiredMemoryFlags) == flags) return true;
			return false;
		}
	}

	public static class StorageBuffer extends MemoryManager.Buffer {
		public StorageBuffer(String name, VkDevice logicalDevice, int bytes, int sharingMode, int optimalMemoryFlags, int requiredMemoryFlags) {
			super(name, logicalDevice, bytes, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, sharingMode, optimalMemoryFlags | VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, requiredMemoryFlags, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
		}
	}

	public static class ExclusiveStorageBuffer extends StorageBuffer {
		public ExclusiveStorageBuffer(String name, VkDevice logicalDevice, int bytes) {
			this(name, logicalDevice, bytes, 0, 0);
		}

		public ExclusiveStorageBuffer(String name, VkDevice logicalDevice, int bytes, int optimalMemoryFlags, int requiredMemoryFlags) {
			super(name, logicalDevice, bytes, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, optimalMemoryFlags, requiredMemoryFlags);
		}
	}

	protected ArrayList<Buffer> buffers = new ArrayList<Buffer>();
	protected VkDevice logicalDevice;

	public MemoryManager(VkDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
	}

	public Buffer createExclusiveComputeBuffer(String name, int bytes) {
		ExclusiveStorageBuffer buffer = new ExclusiveStorageBuffer(name, this.logicalDevice, bytes);
		this.buffers.add(buffer);
		return buffer;
	}

	// TODO: Clean this up! Lots of leaked memory!
	public void doneAllocating() {
		VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
		vkGetPhysicalDeviceMemoryProperties(this.logicalDevice.getPhysicalDevice(), memoryProperties);

		VkMemoryType.Buffer memoryTypes = memoryProperties.memoryTypes();
		HashMap<Integer, ArrayList<Buffer>> memoryTypeToBuffer = new HashMap<Integer, ArrayList<Buffer>>();

		for (Buffer buffer: this.buffers) {
			// Find the first memory type that satisifes their requirements.
			// Vendors are encouraged to put their highest performing memory
			// types at the top of the list, so bail after you find one that works.

			boolean found = false;
			for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
				memoryTypes.position(i);
				if (buffer.isOptimal(i, memoryTypes.propertyFlags())) {
					// Pick this one!
					System.out.println("Picking memory id " + i + " (optimal)");

					ArrayList<Buffer> ab = memoryTypeToBuffer.get(i);
					if (ab == null) {
						ab = new ArrayList<Buffer>();
					}
					ab.add(buffer);
					memoryTypeToBuffer.put(i, ab);

					buffer.setMemoryTypeId(i);
					found = true;
					break;
				}
			}

			if (found) continue;

			for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
				memoryTypes.position(i);
				if (buffer.isAcceptable(i, memoryTypes.propertyFlags())) {
					// Pick this one!
					System.out.println("Picking memory id " + i + " (acceptable)");

					ArrayList<Buffer> ab = memoryTypeToBuffer.get(i);
					if (ab == null) {
						ab = new ArrayList<Buffer>();
					}
					ab.add(buffer);
					memoryTypeToBuffer.put(i, ab);

					buffer.setMemoryTypeId(i);
					found = true;
					break;
				}
			}

			if (found) continue;

			throw new AssertionError("Could not find a suitable set of memory for a buffer.");
		}

		for (Integer i: memoryTypeToBuffer.keySet()) {
			ArrayList<Buffer> buffers = memoryTypeToBuffer.get(i);
			int totalBytes = 0;
			for (Buffer b: buffers) {
				totalBytes += b.getEffectiveSize();
			}
			System.out.println("Determining memory for buffer type " + i + ": " + totalBytes);

			VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc();
			allocateInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
			allocateInfo.allocationSize(totalBytes);
			allocateInfo.memoryTypeIndex(i);

			LongBuffer bufferMemory = memAllocLong(1);
			int err = vkAllocateMemory(this.logicalDevice, allocateInfo, null, bufferMemory);
			if (err != VK_SUCCESS) {
				throw new AssertionError("Failed to allocate memory: " + Vulkan.translateVulkanResult(err));
			}

			long bufferMemoryId = bufferMemory.get(0);

			int offset = 0;
			for (Buffer b: buffers) {
				err = vkBindBufferMemory(
					this.logicalDevice,
					b.getId(),
					bufferMemoryId,
					offset
				);
				if (err != VK_SUCCESS) {
					throw new AssertionError("Failed to bind memory: " + Vulkan.translateVulkanResult(err));
				}
				offset += b.getEffectiveSize();
			}
		}
	}
}