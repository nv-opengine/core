package com.gracefulcode.opengine.vulkan;

import com.gracefulcode.opengine.PhysicalDevice;

import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Comparator;

public interface PhysicalDeviceSelector extends Comparator<PhysicalDevice> {	
}