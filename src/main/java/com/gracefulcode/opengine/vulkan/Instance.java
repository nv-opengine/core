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
	 * The configuration for Instance controls how things behave.
	 */
	public static class Configuration {
		public String applicationName = "GOTBK";
		/**
		 * These are layers that we will enable if we can, but it's not critical.
		 * Unfortunately MoltenVK doesn't support the debug layers, so we cannot
		 * die if we don't have them. It does make debugging a lot harder, though.
		 */
		public String[] desiredLayers = {
			"VK_LAYER_LUNARG_standard_validation",
			"VK_LAYER_LUNARG_monitor",
			// "VK_LAYER_LUNARG_api_dump"
		};

		/**
		 * These are extensions that we will enable if we can, but it's not
		 * critical. Unfortunately MoltenVK doesn't support the debug layers, so
		 * we cannot die if we don't have them. It does make debugging a lot
		 * harder, though.
		 */
		public String[] desiredExtensions = {
			VK_EXT_DEBUG_REPORT_EXTENSION_NAME,
			VK_KHR_SWAPCHAIN_EXTENSION_NAME
			// "VK_EXT_debug_utils"
		};

		/**
		 * ExtensionPicker allows the application to decide what extensions to
		 * enable. By default it's an instance of DefaultExtensionPicker, which
		 * just enables them all.
		 */
		public ExtensionPicker extensionPicker = new DefaultExtensionPicker();

		public PhysicalDeviceSelector physicalDeviceSelector = new DefaultPhysicalDeviceSelector();
	}

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
	 * The configuration that we are currently under. This should not be
	 * modified afterward, as we do not check the configuration constantly for
	 * updates and will likely not notice. Worse, we might half-notice, leading
	 * to undefined behavior. If a setting can be modified during runtime it
	 * will get its own function.
	 */
	protected Configuration configuration;

	/**
	 * All physical devices that this Vulkan knows about.
	 */
	// protected ArrayList<VulkanPhysicalDevice> physicalDevices = new ArrayList<VulkanPhysicalDevice>();
	protected TreeSet<PhysicalDevice> physicalDevices;

	/**
	 * Create an Instance with the default Configuration.
	 */
	public Instance() {
		this(new Configuration());
	}

	/**
	 * Create an Instance with a custom Configuration.
	 */
	public Instance(Configuration configuration) {
		System.out.println("Instance");
		this.configuration = configuration;
		this.physicalDevices = new TreeSet<PhysicalDevice>(this.configuration.physicalDeviceSelector);

		VkApplicationInfo appInfo = VkApplicationInfo.calloc();
		appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
		appInfo.pApplicationName(memUTF8(this.configuration.applicationName));
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