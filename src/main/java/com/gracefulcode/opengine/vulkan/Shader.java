package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;

import org.lwjgl.BufferUtils;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

public class Shader {
	protected long id;

	public Shader(VkDevice device, String fileName, int stage) throws FileNotFoundException, IOException {
		ByteBuffer shaderCode = this.fileToByteBuffer(fileName);
		int err;

		VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
			.pNext(NULL)
			.pCode(shaderCode)
			.flags(0);

		LongBuffer pShaderModule = memAllocLong(1);
		err = vkCreateShaderModule(device, moduleCreateInfo, null, pShaderModule);

		this.id = pShaderModule.get(0);
		memFree(pShaderModule);

		if (err != VK_SUCCESS) {
			// TODO: Translate this error code.
			throw new AssertionError("Failed to create shader module.");
		}
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