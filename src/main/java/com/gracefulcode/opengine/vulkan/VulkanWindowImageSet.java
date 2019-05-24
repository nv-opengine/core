package com.gracefulcode.opengine.vulkan;

import com.gracefulcode.opengine.ImageSet;

public class VulkanWindowImageSet implements ImageSet {
	protected VulkanWindow vulkanWindow;
	protected int count;

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

	// TODO: Fix this!
	public int getHeight() {
		return this.vulkanWindow.getHeight();
	}

	// TODO: Fix this!
	public int getWidth() {
		return this.vulkanWindow.getWidth();
	}

	public boolean knownResolution() {
		return true;
	}

	public String toString() {
		return "VulkanWindowImageSet:[count:" + this.count + ",width:" + this.getWidth() + ",height:" + this.getHeight() + "]";
	}
}