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
 * Handles allocation of Vulkan resources. This is needed because Vulkan wants
 * you to allocate a small number of large blocks then segment those into
 * individual objects in user code.
 * <p>
 * Instead of exposing our users to that directly, we let them "allocate" many
 * small blocks from the <code>MemoryManager</code>. <code>MemoryManager</code>
 * will, in turn, collate those into a few big blocks and handle the
 * translation.
 * <p>
 * While it is not enforced, there should only be one
 * <code>MemoryManager</code> in existance. It should be created and managed by
 * your single instance of the <code>Vulkan</code> class, not directly. This
 * class is considered largely internal.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1
 * @since 0.1
 */
public class MemoryManager {
	/**
	 * Representation of a logical buffer (that is, what the user thinks of as
	 * a buffer). These are collected and collated into a smaller number of
	 * larger memory areas.
	 *
	 * @author Daniel Grace <dgrace@gracefulcode.com>
	 * @version 0.1
	 * @since 0.1
	 */
	public static class Buffer {
		/**
		 * The Vulkan id for the VkBuffer that this buffer represents.
		 */
		protected long id;

		/**
		 * The VkDevice that this buffer was allocated from. Needed to free
		 * this resource, also can be used for debugging/error prevention.
		 */
		protected VkDevice logicalDevice;

		/**
		 * The properties that Vulkan says we need for this memory block. The
		 * logic here is technically specific to the vulkan driver, but we have
		 * to comply with what it says it needs.
		 */
		protected VkMemoryRequirements requirements;

		/**
		 * We determine, from our own logic, the memory properties that we
		 * ideally want. This is usually going to be the fastest type of memory
		 * possible. These aren't required, so we might not get these. This
		 * isn't directly exposed to the user, it's more indirect. We might
		 * possibly expose this to the user eventually if there's a call for
		 * more explicit performance tuning.
		 */
		protected int optimalMemoryFlags;

		/**
		 * This is the flags for the memory that are REQUIRED. As the engine
		 * first gets started, we might have some things (like HOST_VISIBLE)
		 * there more for convenience for anything, and then relax the
		 * requiredness once we write logic to deal with memory transfers.
		 * <p>
		 * If we cannot find a memory type that matches these requirements,
		 * it's an error.
		 */
		protected int requiredMemoryFlags;

		/**
		 * The memory type that we have chosen based on optimalMemoryFlags /
		 * requiredMemoryFlags.
		 */
		protected int memoryTypeId;

		/**
		 * The logical type of buffer that we've decided this buffer is.
		 */
		protected BufferType bufferType;

		/**
		 * A name for this buffer. Used primarily for debugging, is never
		 * actually passed to Vulkan in any form.
		 */
		protected String name;

		/**
		 * The type of buffer, as determined by what type of memory it needs to
		 * be. These types are specified in terms of how they are used (are
		 * they written to often? Big in size? Will they be used as displayed
		 * images?). This determines whether we need fast memory, or memry that
		 * is slower but cpu-visible, etc. This all feeds into the logic for
		 * collating memory regions.
		 * <p>
		 * If possible, this should be kept internal. Expose something more
		 * user-relevant to the user.
		 * 
		 * @author Daniel Grace <dgrace@gracefulcode.com>
		 * @version 0.1
		 * @since 0.1
		 */
		enum BufferType {
			/**
			 * EXCLUSIVESTORAGE is for storage buffers (ie, compute memory)
			 * that is exclusive to a single queue. This should be the default
			 * choice for compute buffers and virtually unused for graphics
			 * work unless you're mixing compute and graphics.
			 */
			EXCLUSIVESTORAGE(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, VK_SHARING_MODE_EXCLUSIVE, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);

			// NOTE: This actually should be a VkAttachment, not a VkBuffer. How do I abstract this?!
			// FRAMEBUFFER()

			/**
			 * VkBufferUsageFlagBits
			 */
			public int usage;

			/**
			 * VkSharingMode
			 */
			public int sharingMode;


			public int optimalMemoryFlags;
			public int requiredMemoryFlags;
			public int descriptorType;

			private BufferType(int usage, int sharingMode, int optimalMemoryFlags, int requiredMemoryFlags, int descriptorType) {
				this.usage = usage;
				this.sharingMode = sharingMode;
				this.optimalMemoryFlags = optimalMemoryFlags;
				this.requiredMemoryFlags = requiredMemoryFlags;
				this.descriptorType = descriptorType;
			}
		}

		// Used when binding. Not valid until we actually allocate memory
		// and segment things up.
		// TODO: When that happens, fill this out.
		protected VkDescriptorBufferInfo bufferInfo;

		public Buffer(
			String name,
			VkDevice logicalDevice,
			int bytes,
			BufferType bufferType
		) {
			this.name = name;
			this.logicalDevice = logicalDevice;
			this.optimalMemoryFlags = optimalMemoryFlags;
			this.requiredMemoryFlags = requiredMemoryFlags;
			this.bufferType = bufferType;

			VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc();
			bufferCreateInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
			bufferCreateInfo.size(bytes);
			bufferCreateInfo.usage(bufferType.usage);
			bufferCreateInfo.sharingMode(bufferType.sharingMode);

			LongBuffer longBuffer = memAllocLong(1);
			int err = vkCreateBuffer(this.logicalDevice, bufferCreateInfo, null, longBuffer);
			bufferCreateInfo.free();

			this.id = longBuffer.get(0);
			memFree(longBuffer);

			this.requirements = VkMemoryRequirements.calloc();
			vkGetBufferMemoryRequirements(this.logicalDevice, this.id, this.requirements);

			/*
			System.out.println("Alignment: " + this.requirements.alignment());
			System.out.println("Memory Type: " + Integer.toBinaryString(this.requirements.memoryTypeBits()));
			System.out.println("Asked Size: " + bytes);
			System.out.println("Size: " + this.requirements.size());
			System.out.println("Effective Size: " + this.getEffectiveSize());
			*/
		}

		public int getDescriptorType() {
			return this.bufferType.descriptorType;
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

		public String getName() {
			return this.name;
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
			if ((flags & this.optimalMemoryFlags) == this.optimalMemoryFlags) return true;
			return false;
		}

		public boolean isAcceptable(int id, int flags) {
			if ((this.requirements.memoryTypeBits() & (1 << id)) == 0) return false;
			if ((flags & this.requiredMemoryFlags) == this.requiredMemoryFlags) return true;
			return false;
		}
	}

	protected ArrayList<Buffer> buffers = new ArrayList<Buffer>();
	protected ArrayList<Image> images = new ArrayList<Image>();
	protected VkDevice logicalDevice;

	MemoryManager(VkDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
	}

	public Image createFramebufferImage(String name, int bytes) {
		Image image = new FramebufferImage();
		this.images.add(image);
		return image;
	}

	public Buffer createExclusiveComputeBuffer(String name, int bytes) {
		Buffer buffer = new Buffer(name, this.logicalDevice, bytes, MemoryManager.Buffer.BufferType.EXCLUSIVESTORAGE);
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
					// System.out.println("Picking memory id " + i + " (optimal)");

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
					// System.out.println("Picking memory id " + i + " (acceptable)");

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

			String errMessage = "";
			errMessage += "Could not find a suitable set of memory for buffer: " + buffer.getName();
			errMessage += "\n";
			for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
				memoryTypes.position(i);
				errMessage += "Memory: " + Integer.toBinaryString(memoryTypes.propertyFlags()) + " vs Required: " + Integer.toBinaryString(buffer.requirements.memoryTypeBits());
				errMessage += "\n";
			}

			throw new AssertionError(errMessage);
		}

		for (Integer i: memoryTypeToBuffer.keySet()) {
			ArrayList<Buffer> buffers = memoryTypeToBuffer.get(i);
			int totalBytes = 0;
			for (Buffer b: buffers) {
				totalBytes += b.getEffectiveSize();
			}
			// System.out.println("Determining memory for buffer type " + i + ": " + totalBytes);

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