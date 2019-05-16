package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;

import org.lwjgl.BufferUtils;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

public class Shader {
	protected long id;
	protected VkDevice logicalDevice;
	protected ArrayList<PendingBinding> pendingBindings = new ArrayList<PendingBinding>();
	protected DescriptorPool descriptorPool;

	private static class PendingBinding {
		public int bindLocation;
		public int descriptorType;
		public int stageFlags;
		public MemoryManager.Buffer buffer;
	}

	public Shader(VkDevice logicalDevice, String fileName, int stage) throws FileNotFoundException, IOException {
		this.logicalDevice = logicalDevice;

		ByteBuffer shaderCode = this.fileToByteBuffer(fileName);
		int err;

		VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
			.pNext(NULL)
			.pCode(shaderCode)
			.flags(0);

		LongBuffer pShaderModule = memAllocLong(1);
		err = vkCreateShaderModule(this.logicalDevice, moduleCreateInfo, null, pShaderModule);

		this.id = pShaderModule.get(0);
		memFree(pShaderModule);

		if (err != VK_SUCCESS) {
			// TODO: Translate this error code.
			throw new AssertionError("Failed to create shader module.");
		}
	}

	public void createBinding(int bindLocation, MemoryManager.Buffer buffer) {
		PendingBinding pb = new PendingBinding();
		pb.bindLocation = bindLocation;

		// TODO: These two shouldn't be constant.
		pb.descriptorType = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
		pb.stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
		pb.buffer = buffer;

		this.pendingBindings.add(pb);
	}

	public void doneBinding() {
		VkDescriptorSetLayoutBinding.Buffer binding = VkDescriptorSetLayoutBinding.calloc(this.pendingBindings.size());
		for (int i = 0; i < this.pendingBindings.size(); i++) {
			PendingBinding pb = this.pendingBindings.get(i);
			binding.position(0);
			binding.binding(pb.bindLocation);
			binding.descriptorType(pb.descriptorType);
			binding.stageFlags(pb.stageFlags);
		}

		VkDescriptorSetLayoutCreateInfo layoutCreate = VkDescriptorSetLayoutCreateInfo.calloc();
		layoutCreate.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
		layoutCreate.pBindings(binding);

		LongBuffer lb = memAllocLong(1);
		int err = vkCreateDescriptorSetLayout(this.logicalDevice, layoutCreate, null, lb);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed");
		}
		long ret = lb.get(0);

		this.descriptorPool = new DescriptorPool(this.logicalDevice, ret);
		// TODO: This is definitely wrong. We want to essentially pass an array into
		// this, but it only accepts one. We build the array inside of DescriptorPool,
		// when it should be built out here?
		this.descriptorPool.doAThing(this.pendingBindings.get(0).buffer);
	}

	private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
		ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
		buffer.flip();
		newBuffer.put(buffer);
		return newBuffer;
    }

	protected ByteBuffer fileToByteBuffer(String fileName) throws FileNotFoundException, IOException {
		int bufferSize = 8192;
		ByteBuffer buffer;
		URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);

		File file = new File(url.getFile());
		if (file.isFile()) {
			FileInputStream fis = new FileInputStream(file);
			FileChannel fc = fis.getChannel();
			buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			fc.close();
			fis.close();
		} else {
			buffer = BufferUtils.createByteBuffer(bufferSize);
			InputStream source = url.openStream();
			if (source == null)
				throw new FileNotFoundException(fileName);
			try {
				byte[] buf = new byte[8192];
				while (true) {
					int bytes = source.read(buf, 0, buf.length);
					if (bytes == -1)
						break;
					if (buffer.remaining() < bytes)
						buffer = resizeBuffer(buffer, buffer.capacity() * 2);
					buffer.put(buf, 0, bytes);
				}
				buffer.flip();
			} finally {
				source.close();
			}
		}
		return buffer;
	}
}