package com.gracefulcode.opengine.v2.vulkan.plugins;

import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

public interface Plugin {
	public void setupCreateInfo(VkInstanceCreateInfo createInfo);
	public void postCreate(VkInstance instance);
	public void dispose();
}