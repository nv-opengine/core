package com.gracefulcode.opengine.vulkan;

import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.util.Comparator;

public interface PhysicalDeviceSelector extends Comparator<VkPhysicalDeviceProperties> {	
}