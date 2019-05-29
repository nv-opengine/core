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
	 * Given a windowID, give us a Surface object.
	 */
	protected HashMap<Long, Surface> windowToSurface = new HashMap<Long, Surface>();

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

		PointerBuffer extensions = this.getExtensions();
		PointerBuffer layers = this.getLayers();

		// boolean foundDebugging = false;
		// for (int i = 0; i < extensions.limit(); i++) {
		// 	String tmp = extensions.getStringUTF8(i);
		// 	if (tmp.equals(VK_EXT_DEBUG_REPORT_EXTENSION_NAME)) {
		// 		foundDebugging = true;
		// 		this.setupDebugging(pCreateInfo);
		// 	}
		// }

		// if (!foundDebugging) {
		// 	System.out.println("Failed to find extension:" + VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
		// }

		VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc();
		instanceCreateInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
		instanceCreateInfo.pNext(NULL);
		instanceCreateInfo.pApplicationInfo(appInfo);
		instanceCreateInfo.ppEnabledExtensionNames(extensions);
		instanceCreateInfo.ppEnabledLayerNames(layers);

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

	public Surface createSurface(long windowId) {
		if (!this.windowToSurface.containsKey(windowId)) {
			this.windowToSurface.put(windowId, new Surface(windowId, this.instance));
		}

		return this.windowToSurface.get(windowId);
	}

	protected PointerBuffer getLayers() {
		return null;
	}

	protected PointerBuffer getExtensions() {
		IntBuffer ib = memAllocInt(1);

		PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
		if (requiredExtensions == null) {
			throw new AssertionError("Failed to find list of required Vulkan extensions");
		}

		vkEnumerateInstanceExtensionProperties((ByteBuffer)null, ib, null);
		int result = ib.get(0);

		VkExtensionProperties.Buffer buffer = VkExtensionProperties.calloc(result);
		vkEnumerateInstanceExtensionProperties((ByteBuffer)null, ib, buffer);

		int limit = buffer.limit();

		PointerBuffer ppEnabledExtensionNames = memAllocPointer(result);

		// PointerBuffer ppEnabledExtensionNames = memAllocPointer(requiredExtensions.remaining() + this.configuration.desiredExtensions.length);
		ppEnabledExtensionNames.put(requiredExtensions);
		HashSet<String> enabledExtensions = new HashSet<String>();

		for (int m = 0; m < limit; m++) {
			buffer.position(m);

			this.configuration.extensionPicker.needsExtension(buffer.extensionNameString(), enabledExtensions);

			/*
			boolean didFind = false;
			for (int i = 0; i < requiredExtensions.limit(); i++) {
				for (int p = 0; p < this.configuration.desiredExtensions.length; p++) {
					if (requiredExtensions.getStringASCII(i).equals(buffer.extensionNameString())) {
						didFind = true;
						break;
					}
				}
			}
			if (!didFind) {
				for (int i = 0; i < this.configuration.desiredExtensions.length; i++) {
					if (buffer.extensionNameString().equals(this.configuration.desiredExtensions[i])) {
						didFind = true;
						ppEnabledExtensionNames.put(memUTF8(this.configuration.desiredExtensions[i]));
						break;
					}
				}
			}
			*/
		}

		ppEnabledExtensionNames.flip();
		buffer.free();

		memFree(ib);

		return ppEnabledExtensionNames;
	}

	public void dispose() {
		vkDestroyInstance(this.instance, null);
	}
}