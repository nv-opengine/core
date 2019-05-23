package com.gracefulcode.opengine.vulkan;

import com.gracefulcode.opengine.ImageSet;

/**
 * There is a command buffer per rendering setup.
 * In the underlying implementation, there is one per rendering setup <b>per
 * frame</b>, but we try not to expose the per-frame elements to the user.
 */
public class CommandBuffer {
	public CommandBuffer() {

	}

	/**
	 * Starts creating a (set of) command buffer(s) for the given window.
	 */
	public void start(VulkanWindow window) {
		ImageSet imageSet = window.getImageSet();
	}
}