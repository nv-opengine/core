package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.PointerBuffer;

import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

/**
 * A Physical Device represents a GPU in the system. Many things ultimately
 * come through here, though we should endeavor to pass as much as possible to
 * other classes to make things manageable.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1
 * @since 0.1
 */
public class VulkanPhysicalDevice {
	protected VkPhysicalDevice device;

	public VulkanPhysicalDevice(VkPhysicalDevice physicalDevice) {
		this.device = physicalDevice;
	}

	/*
	public boolean canDisplayToSurface(Surface surface) {
		for (QueueFamilyProperties qpf: this.queueFamilyProperties) {
			vkGetPhysicalDeviceSurfaceSupportKHR(
				this.device,
				qpf.index,
				surface.getId(),
				this.ib
			);
			if (this.ib.get(0) == VK_TRUE) {
				if (!this.surfaceProperties.containsKey(surface)) {
					PhysicalDeviceSurface pds = new PhysicalDeviceSurface(this, surface);
					this.surfaceProperties.put(
						surface.getId(),
						pds
					);
				}
				return true;
			}
		}
		return false;
	}
	*/

	public VulkanLogicalDevice createLogicalDevice(
		String[] requiredExtensions2,
		boolean hasGraphicsQueue,
		boolean hasComputeQueue
	) {
		String[] requiredExtensions = {
			VK_KHR_SWAPCHAIN_EXTENSION_NAME
			// "VK_EXT_debug_utils"
		};

		PointerBuffer ppEnabledExtensionNames = memAllocPointer(requiredExtensions.length);
		for (int i = 0; i < requiredExtensions.length; i++) {
			ppEnabledExtensionNames.put(memUTF8(requiredExtensions[i]));
		}
		ppEnabledExtensionNames.flip();

		FloatBuffer pQueuePriorities = memAllocFloat(1).put(0.0f);
		pQueuePriorities.flip();

		VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1);
		queueCreateInfo.position(0);
		queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
		// queueCreateInfo.queueFamilyIndex(this.graphicsQueueIndex);
		queueCreateInfo.pQueuePriorities(pQueuePriorities);

		VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
			.pNext(NULL)
			.pQueueCreateInfos(queueCreateInfo)
			.ppEnabledExtensionNames(ppEnabledExtensionNames)
			.ppEnabledLayerNames(null);

		PointerBuffer pb = memAllocPointer(1);
		int err = vkCreateDevice(this.device, deviceCreateInfo, null, pb);
		long device = pb.get(0);

		return new VulkanLogicalDevice(
			this,
			new VkDevice(device, this.device, deviceCreateInfo)
		);
	}

	public void dispose() {
	}
}