package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkInstance;

public class Surface {
	protected long id;

	public Surface(long windowId, VkInstance vkInstance) {
		LongBuffer lb = memAllocLong(1);
		int err;

		if ((err = glfwCreateWindowSurface(vkInstance, windowId, null, lb)) != VK_SUCCESS) {
			throw new AssertionError("Could not create surface: " + Vulkan.translateVulkanResult(err));
		}
		this.id = lb.get(0);
		memFree(lb);
	}

	/**
	 * TODO: Get rid of this.
	 */
	public long getId() {
		return this.id;
	}
}