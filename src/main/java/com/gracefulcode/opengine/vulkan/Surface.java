package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkInstance;

/**
 * A surface is an OS-specific representation of the backbuffer of a window. We
 * don't have to allocate the memory for this, but we likewise don't have much
 * control.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1.1
 */
public class Surface {
	protected long id;

	public Surface(long windowId, Vulkan vulkan) {
		LongBuffer lb = memAllocLong(1);
		int err;

		if ((err = glfwCreateWindowSurface(vulkan.getVkInstance(), windowId, null, lb)) != VK_SUCCESS) {
			throw new AssertionError("Could not create surface: " + Vulkan.translateVulkanResult(err));
		}
		this.id = lb.get(0);
		memFree(lb);
	}

	public long getId() {
		return this.id;
	}
}