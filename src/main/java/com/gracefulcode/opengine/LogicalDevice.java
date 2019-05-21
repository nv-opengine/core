package com.gracefulcode.opengine;

import org.lwjgl.vulkan.VkDevice;

public class LogicalDevice {
	protected PhysicalDevice physicalDevice;
	protected VkDevice logicalDevice;

	LogicalDevice(PhysicalDevice physicalDevice, VkDevice logicalDevice) {
		this.physicalDevice = physicalDevice;
		this.logicalDevice = logicalDevice;
	}

	public VkDevice getDevice() {
		return this.logicalDevice;
	}

	public PhysicalDevice getPhysicalDevice() {
		return this.physicalDevice;
	}

	public String toString() {
		return "LogicalDevice<PhysicalDevice:" + this.physicalDevice.toString() + ">";
	}
}