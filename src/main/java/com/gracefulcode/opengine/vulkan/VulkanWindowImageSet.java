package com.gracefulcode.opengine.vulkan;

import com.gracefulcode.opengine.ImageSet;

public class VulkanWindowImageSet implements ImageSet {
	protected int count;
	protected VulkanWindow vulkanWindow;

	public VulkanWindowImageSet(VulkanWindow vulkanWindow, int count) {
		this.vulkanWindow = vulkanWindow;
		this.count = count;
	}

	/**
	 * Our initial images have their memory already created for us.
	 */
	public boolean memoryBacked() {
		return true;
	}

	public int getHeight() {
		return 0;
	}

	public int getWidth() {
		return 0;
	}

	public boolean knownResolution() {
		return true;
	}

	public String toString() {
		return "VulkanWindowImageSet:[count:" + this.count + ",width:" + this.getWidth() + ",height:" + this.getHeight() + "]";
	}
}