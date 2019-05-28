package com.gracefulcode.opengine.vulkan;

import com.gracefulcode.opengine.v2.LogicalDevice;
import com.gracefulcode.opengine.v2.PhysicalDevice;

import org.lwjgl.vulkan.VkDevice;

public class VulkanLogicalDevice implements LogicalDevice {
	protected VulkanPhysicalDevice physicalDevice;
	protected VkDevice logicalDevice;

	public VulkanLogicalDevice(VulkanPhysicalDevice physicalDevice, VkDevice logicalDevice) {
		this.physicalDevice = physicalDevice;
		this.logicalDevice = logicalDevice;
	}

	public VkDevice getDevice() {
		return this.logicalDevice;
	}

	public VulkanPhysicalDevice getPhysicalDevice() {
		return this.physicalDevice;
	}

	public String toString() {
		return "VulkanLogicalDevice<PhysicalDevice:" + this.physicalDevice.toString() + ">";
	}
}