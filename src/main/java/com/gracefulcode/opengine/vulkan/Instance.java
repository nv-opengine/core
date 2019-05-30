package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.v2.vulkan.DefaultPhysicalDeviceSelector;
import com.gracefulcode.opengine.v2.vulkan.PhysicalDevice;
import com.gracefulcode.opengine.v2.vulkan.PhysicalDeviceSelector;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeSet;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;

/**
 * Instance is a wrapper around VkInstance. Most things are created through
 * here directly or indirectly.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1
 * @since 0.1
 */
public class Instance {
	/**
	 * The long that represents the VkInstance to Vulkan.
	 */
	protected long id;

	/**
	 * LWJGL wraps a VkInstance in its own class for some reason. We need to
	 * keep that around so that we can interface with lwjgl functions.
	 */
	protected VkInstance instance;

	/**
	 * All physical devices that this Vulkan knows about.
	 */
	// protected ArrayList<VulkanPhysicalDevice> physicalDevices = new ArrayList<VulkanPhysicalDevice>();
	protected TreeSet<PhysicalDevice> physicalDevices;

	/**
	 * Create an Instance with a custom Configuration.
	 */
	public Instance() {
		VkApplicationInfo appInfo = VkApplicationInfo.calloc();
		appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
		appInfo.pEngineName(memUTF8("Opengine v0.1"));
		appInfo.apiVersion(VK_MAKE_VERSION(1, 0, 2));

		VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc();
		instanceCreateInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
		instanceCreateInfo.pNext(NULL);
		instanceCreateInfo.pApplicationInfo(appInfo);

		PointerBuffer pInstance = memAllocPointer(1);
		int err = vkCreateInstance(instanceCreateInfo, null, pInstance);
		this.id = pInstance.get(0);
		memFree(pInstance);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create VkInstance: " + Vulkan.translateVulkanResult(err));
		}

		this.instance = new VkInstance(this.id, instanceCreateInfo);
		this.initPhysicalDevices();
	}

	/**
	 * Can we make this unnecessary?
	 */
	TreeSet<PhysicalDevice> getPhysicalDevices() {
		return this.physicalDevices;
	}

	protected void initPhysicalDevices() {
		IntBuffer ib = memAllocInt(1);

		int err = vkEnumeratePhysicalDevices(this.instance, ib, null);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to get number of physical devices: " + Vulkan.translateVulkanResult(err));
		}

		int numPhysicalDevices = ib.get(0);

		PointerBuffer pPhysicalDevices = memAllocPointer(numPhysicalDevices);
		err = vkEnumeratePhysicalDevices(this.instance, ib, pPhysicalDevices);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to get physical devices: " + Vulkan.translateVulkanResult(err));
		}

		for (int i = 0; i < numPhysicalDevices; i++) {
			long physicalDeviceId = pPhysicalDevices.get(i);
			PhysicalDevice physicalDevice = new PhysicalDevice(physicalDeviceId, this.instance);
			this.physicalDevices.add(physicalDevice);
		}
		memFree(pPhysicalDevices);
		memFree(ib);
	}
}