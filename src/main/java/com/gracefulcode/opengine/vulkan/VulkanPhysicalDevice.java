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
import org.lwjgl.vulkan.VkExtent3D;
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
	/**
	 * Queues that are available for this Physical Device.
	 *
	 * @author Daniel Grace <dgrace@gracefulcode.com>
	 * @version 0.1
	 * @since 0.1
	 */
	public static class QueueFamilyProperties {
		public int index;
		public VkExtent3D minImageTransferGranularity;
		public int queueCount;
		public int queueFlags;
		public int timestampValidBits;
	}

	protected HashMap<Long, PhysicalDeviceSurface> surfaceProperties = new HashMap<Long, PhysicalDeviceSurface>();
	protected ArrayList<QueueFamilyProperties> queueFamilyProperties = new ArrayList<QueueFamilyProperties>();
	protected IntBuffer ib;
	protected PointerBuffer pb;
	protected VkPhysicalDevice device;
	protected int graphicsQueueIndex = -1;

	public VulkanPhysicalDevice(VkPhysicalDevice physicalDevice) {
		this.ib = memAllocInt(1);
		this.pb = memAllocPointer(1);

		this.device = physicalDevice;

		vkGetPhysicalDeviceQueueFamilyProperties(this.device, this.ib, null);
		int queueCount = this.ib.get(0);

		VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
		vkGetPhysicalDeviceQueueFamilyProperties(this.device, this.ib, queueProps);

		int queueFamilyIndex;
		for (queueFamilyIndex = 0; queueFamilyIndex < queueCount; queueFamilyIndex++) {
			VkQueueFamilyProperties properties = queueProps.get(queueFamilyIndex);

			QueueFamilyProperties qfp = new QueueFamilyProperties();
			qfp.index = queueFamilyIndex;
			qfp.minImageTransferGranularity = properties.minImageTransferGranularity();
			qfp.queueCount = properties.queueCount();
			qfp.queueFlags = properties.queueFlags();
			qfp.timestampValidBits = properties.timestampValidBits();

			if ((this.graphicsQueueIndex == -1) && ((qfp.queueFlags & VK_QUEUE_GRAPHICS_BIT) > 0)) {
				this.graphicsQueueIndex = queueFamilyIndex;
			}
			this.queueFamilyProperties.add(qfp);
		}

		queueProps.free();
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

	public PhysicalDeviceSurface getSurface(long surface) {
		return this.surfaceProperties.get(surface);
	}

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
		queueCreateInfo.queueFamilyIndex(this.graphicsQueueIndex);
		queueCreateInfo.pQueuePriorities(pQueuePriorities);

		VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
			.pNext(NULL)
			.pQueueCreateInfos(queueCreateInfo)
			.ppEnabledExtensionNames(ppEnabledExtensionNames)
			.ppEnabledLayerNames(null);

		int err = vkCreateDevice(this.device, deviceCreateInfo, null, this.pb);
		long device = this.pb.get(0);
		// ppEnabledExtensionNames.free();

		return new VulkanLogicalDevice(
			this,
			new VkDevice(device, this.device, deviceCreateInfo)
		);
	}

	public int apiVersion() { return 0; }
	public int deviceType() { return 0; }

	public String toString() {
		String ret = "PhysicalDevice<";
		ret += ">";
		return ret;
	}

	public void dispose() {
		memFree(this.ib);
		for (PhysicalDeviceSurface surface: this.surfaceProperties.values()) {
			surface.dispose();
		}
	}
}