package com.gracefulcode.opengine;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkImageViewCreateInfo;

public class ImageView {
	protected LogicalDevice logicalDevice;
	protected long id;

	public ImageView(LogicalDevice logicalDevice, long id, int format) {
		this.logicalDevice = logicalDevice;

		VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc();
		createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
		createInfo.image(id);
		createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
		createInfo.format(format);
		createInfo.components()
			.r(VK_COMPONENT_SWIZZLE_IDENTITY)
			.g(VK_COMPONENT_SWIZZLE_IDENTITY)
			.b(VK_COMPONENT_SWIZZLE_IDENTITY)
			.a(VK_COMPONENT_SWIZZLE_IDENTITY);
		createInfo.subresourceRange()
			.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
			.baseMipLevel(0)
			.levelCount(1)
			.baseArrayLayer(0)
			.layerCount(1);

		LongBuffer lb = memAllocLong(1);
		int err = vkCreateImageView(this.logicalDevice.getDevice(), createInfo, null, lb);
		this.id = lb.get(0);

		System.out.println("ImageView: " + this.id);

		memFree(lb);
		createInfo.free();
	}
}