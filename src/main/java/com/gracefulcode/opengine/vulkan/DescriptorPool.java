package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.system.MemoryUtil.*;

import com.gracefulcode.opengine.annotations.ThreadInfo;

import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

/**
 * A descriptor pool maintains a pool of descriptors, from which descriptor
 * sets are allocated. Descriptor pools are externally synchronized, meaning
 * that the application must not allocate and/or free descriptor sets from the
 * same pool in multiple threads simultaneously.
 */
@ThreadInfo( perThread = true )
public class DescriptorPool {
	protected long pool;
	protected VkDevice logicalDevice;
	protected long layout;

	DescriptorPool(VkDevice logicalDevice, long layout) {
		this.logicalDevice = logicalDevice;
		this.layout = layout;

		VkDescriptorPoolSize.Buffer descriptorPoolSize = VkDescriptorPoolSize.calloc(1);
		descriptorPoolSize.position(0);
		// TODO: This isn't always storage buffer!
		descriptorPoolSize.type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
		descriptorPoolSize.descriptorCount(1);

		VkDescriptorPoolCreateInfo descriptorPoolCreate = VkDescriptorPoolCreateInfo.calloc();
		descriptorPoolCreate.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
		descriptorPoolCreate.maxSets(1);
		descriptorPoolCreate.pPoolSizes(descriptorPoolSize);

		LongBuffer lb = memAllocLong(1);
		int err = vkCreateDescriptorPool(this.logicalDevice, descriptorPoolCreate, null, lb);
		this.pool = lb.get(0);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create descriptor pool.");
		}
	}

	// TODO: Rename this silly thing.
	public void doAThing(MemoryManager.Buffer buffer) {
		LongBuffer lb = memAllocLong(1);
		lb.put(this.layout);
		lb.flip();

		// Create a descriptor set (should be a different thing)
		VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc();
		descriptorSetAllocateInfo.descriptorPool(this.pool);
		descriptorSetAllocateInfo.pSetLayouts(lb);

		LongBuffer lb2 = memAllocLong(1);
		int err = vkAllocateDescriptorSets(this.logicalDevice, descriptorSetAllocateInfo, lb2);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create descriptor set.");
		}
		System.out.println("Set is " + lb2.get(0));

		/**
		 * VkDescriptorBufferInfo (?) merely says what the buffer *looks like*.
		 * It's not actually binding to a specific buffer. This can be done far
		 * in advance if the user gives us enough info.
		 */

		// TODO: This allows multiple, do I ever want multiple?
		// Answer: Yes. Really we need to pass in an array of them
		// or have some other way of easily building the array. Even
		// my simple compute shader is going to have multiple, probably.
		VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1);
		bufferInfo.clear();
		bufferInfo.put(buffer.getBufferInfo());
		bufferInfo.flip();

		/**
		 * Write descriptor set is what binds it to a specific buffer. We need to split these parts out.
		 */

		// Set up a descriptor set
		VkWriteDescriptorSet writeDescriptorSet = VkWriteDescriptorSet.calloc();
		writeDescriptorSet.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
		writeDescriptorSet.dstSet(lb2.get(0));
		// This is variable
		writeDescriptorSet.dstBinding(0);

		// More efficient if I can do it all at once?
		// writeDescriptorSet.descriptorCount(1);

		// This isn't always the same!
		writeDescriptorSet.descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
		writeDescriptorSet.pBufferInfo(bufferInfo);
	}
}