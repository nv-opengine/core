package com.gracefulcode.opengine.v2.vulkan;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.v2.vulkan.plugins.interfaces.FiltersPhysicalDevices;
import com.gracefulcode.opengine.v2.vulkan.plugins.interfaces.NeedsQueues;
import com.gracefulcode.opengine.v2.vulkan.plugins.Plugin;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;

/**
 * The main interface between end-user code and the Vulkan backend.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1
 */
public class Vulkan {
	/**
	 * Configuration for Vulkan.
	 *
	 * @author Daniel Grace <dgrace@gracefulcode.com>
	 * @version 0.1.1
	 * @since 0.1
	 */
	public static class Configuration {
		/**
		 * What's the status of this layer/extenion?
		 *
		 * DONT_CARE:
		 *     Nothing has modified this. This won't be asked for if it ends in
		 *     this state, and most things start in this state.
		 * NOT_DESIRED:
		 *     We do NOT want this to be enabled. If one piece has NOT_DESIRED
		 *     and another has DESIRED, we have a fatal conflict.
		 * DESIRED:
		 *     We want this, but it's not a fatal error if we don't get it.
		 * REQUIRED:
		 *     It's a fatal error if we cannot have this extension/layer. If
		 *     one thing says REQUIRED and another says NOT_DESIRED, we have a
		 *     fatal conflict.
		 */
		public static enum RequireType {
			DONT_CARE,
			NOT_DESIRED,
			DESIRED,
			REQUIRED
		};

		/**
		 * Your application name is passed to Vulkan in case your game gets
		 * super popular and they want to tweak their driver for your game.
		 * This should be something unique, but it is not that important at
		 * the end of the day.
		 */
		public String applicationName = "";

		public ExtensionConfiguration extensionConfiguration = new ExtensionConfiguration();
		public LayerConfiguration layerConfiguration = new LayerConfiguration();
		public PhysicalDeviceSelector<PhysicalDevice> physicalDeviceSelector = new DefaultPhysicalDeviceSelector();
	}

	/**
	 * The configuration that we are using.
	 */
	protected Configuration configuration;
	protected VkInstance vkInstance;
	protected TreeSet<PhysicalDevice> physicalDevices;

	/**
	 * Initializing Vulkan multiple times is not allowed. This is the one and
	 * only instance.
	 */
	protected static Vulkan instance;

	protected static ArrayList<Plugin> plugins = new ArrayList<Plugin>();
	protected static ArrayList<FiltersPhysicalDevices> filtersPhysicalDevices = new ArrayList<FiltersPhysicalDevices>();
	protected static ArrayList<NeedsQueues> needsQueues = new ArrayList<NeedsQueues>();

	public static void addPlugin(Plugin plugin) {
		Vulkan.plugins.add(plugin);
		if (plugin instanceof FiltersPhysicalDevices) {
			Vulkan.filtersPhysicalDevices.add((FiltersPhysicalDevices)plugin);
		}
		if (plugin instanceof NeedsQueues) {
			Vulkan.needsQueues.add((NeedsQueues)plugin);
		}
	}

	/**
	 * Initialize the vulkan singleton.
	 *
	 * @param configuration The configuration of the resultant Vulkan instance.
	 * @return An initialized Vulkan instance.
	 * @throws AssertionError If Vulkan has previously been initialized.
	 * @throws AssertionError If GLFW is unable to be initialized.
	 */
	public static Vulkan initialize(Configuration configuration) {
		if (Vulkan.instance != null) {
			throw new AssertionError("Cannot initialize multiple instances of Vulkan.");
		}

		if (!glfwInit()) {
			throw new AssertionError("Failed to initialize GLFW");
		}

		Vulkan.instance = new Vulkan(configuration);
		return Vulkan.instance;
	}

	/**
	 * Initialize the vulkan singleton.
	 *
	 * @return An initialized Vulkan instance.
	 * @throws AssertionError If Vulkan has previously been initialized.
	 */
	public static Vulkan initialize() {
		return Vulkan.initialize(new Configuration());
	}

	/**
	 * Gets the initialized Vulkan instance. Returns null if not already
	 * initialized, won't initialize for you.
	 *
	 * @return The initialized Vulkan instance or null if one has not been initialized yet.
	 */
	public static Vulkan get() {
		return Vulkan.instance;
	}

	/**
	 * Initialize Vulkan with specific configuration.
	 */
	private Vulkan(Configuration configuration) {
		this.configuration = configuration;
		this.physicalDevices = new TreeSet<PhysicalDevice>(this.configuration.physicalDeviceSelector);

		this.createInstance();
		this.setupPhysicalDevices();

		if (this.physicalDevices.size() == 0) {
			throw new AssertionError("No suitable physical devices found.");
		}

		this.createLogicalDevice();
	}

	protected void createLogicalDevice() {
		int compute = 0;
		int graphics = 0;
		int present = 0;

		System.out.println("Creating logical device...");
		for (NeedsQueues nq: Vulkan.needsQueues) {
			if (nq.numGraphicsQueues() > graphics)
				graphics = nq.numGraphicsQueues();
			if (nq.numComputeQueues() > compute)
				compute = nq.numComputeQueues();
			if (nq.numPresentationQueues() > present)
				present = nq.numPresentationQueues();
		}

		System.out.println("compute: " + compute + ", graphics: " + graphics + ", present: " + present);

		/**
		 * TODO: Plugins and/or the application should be able to set these.
		 * Right now we're just using nothing.
		 */
		VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc();

		VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc();
		createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
		createInfo.pQueueCreateInfos();
		createInfo.pEnabledFeatures(features);

		int err = vkCreateDevice(physicalDevice, createInfo, null, device);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Could not create logical device: " + Vulkan.translateVulkanResult(err));
		}
	}

	protected void createInstance() {
		/**
		 * appInfo is basic information about the application itself. There
		 * isn't anything super important here, though we do let Vulkan know
		 * about both the engine and the particular game so that it can change
		 * its behavior if there's a particular popular engine/game.
		 */
		VkApplicationInfo appInfo = VkApplicationInfo.calloc();
		appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
		appInfo.pApplicationName(memUTF8(this.configuration.applicationName));
		appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
		appInfo.pEngineName(memUTF8("Opengine"));
		appInfo.engineVersion(1);
		appInfo.apiVersion(VK_MAKE_VERSION(1, 0, 2));

		/**
		 * Create info is pretty basic right now.
		 */
		VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc();
		createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
		createInfo.pApplicationInfo(appInfo);
		createInfo.pNext(NULL);

		/**
		 * Allow plugins to modify this. I think there's some advanced way
		 * that I can do debugging that uses this. Right now nothing uses it.
		 */
		for (Plugin plugin: Vulkan.plugins) {
			plugin.setupCreateInfo(createInfo);
		}

		IntBuffer ib = memAllocInt(1);
		vkEnumerateInstanceExtensionProperties((CharSequence)null, ib, null);
		VkExtensionProperties.Buffer extensionProperties = VkExtensionProperties.calloc(ib.get(0));
		vkEnumerateInstanceExtensionProperties((CharSequence)null, ib, extensionProperties);
		for (int i = 0; i < extensionProperties.limit(); i++) {
			extensionProperties.position(i);
			this.configuration.extensionConfiguration.setExtension(extensionProperties.extensionNameString(), ExtensionConfiguration.RequireType.DONT_CARE);
		}
		this.configuration.extensionConfiguration.lock();

		for (Plugin plugin: Vulkan.plugins) {
			plugin.setupExtensions(this.configuration.extensionConfiguration);
		}

		PointerBuffer configuredExtensions = this.configuration.extensionConfiguration.getConfiguredExtensions();
		createInfo.ppEnabledExtensionNames(configuredExtensions);

		PointerBuffer configuredLayers = this.configuration.layerConfiguration.getConfiguredLayers();
		createInfo.ppEnabledLayerNames(configuredLayers);

		/**
		 * Actually create the VkInstance.
		 */
		PointerBuffer pInstance = memAllocPointer(1);
		int err = vkCreateInstance(createInfo, null, pInstance);
		long vulkanInstanceId = pInstance.get(0);
		memFree(pInstance);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create VkInstance: " + Vulkan.translateVulkanResult(err));
		}

		this.vkInstance = new VkInstance(vulkanInstanceId, createInfo);

		/**
		 * Call the plugins after creation.
		 */
		for (Plugin plugin: Vulkan.plugins) {
			plugin.postCreate(this, this.vkInstance, this.configuration.extensionConfiguration, this.configuration.layerConfiguration);
		}

		memFree(ib);
	}

	protected void setupPhysicalDevices() {
		/**
		 * Get the physical devices that we have access to.
		 */
		IntBuffer ib = memAllocInt(1);
		int err = vkEnumeratePhysicalDevices(this.vkInstance, ib, null);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to get number of physical devices: " + Vulkan.translateVulkanResult(err));
		}

		int numPhysicalDevices = ib.get(0);

		PointerBuffer pPhysicalDevices = memAllocPointer(numPhysicalDevices);
		err = vkEnumeratePhysicalDevices(this.vkInstance, ib, pPhysicalDevices);
		memFree(ib);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to get physical devices: " + Vulkan.translateVulkanResult(err));
		}

		for (int i = 0; i < numPhysicalDevices; i++) {
			long physicalDeviceId = pPhysicalDevices.get(i);
			PhysicalDevice physicalDevice = new PhysicalDevice(physicalDeviceId, this.vkInstance);

			boolean passedCheck = true;
			for (FiltersPhysicalDevices filter: Vulkan.filtersPhysicalDevices) {
				if (!filter.canUsePhysicalDevice(physicalDevice)) {
					passedCheck = false;
					break;
				}
			}
			if (passedCheck) {
				this.physicalDevices.add(physicalDevice);
			}
		}

		memFree(pPhysicalDevices);
	}

	public Collection<PhysicalDevice> getPhysicalDevices() {
		return this.physicalDevices;
	}

	public VkInstance getVkInstance() {
		return this.vkInstance;
	}

	public void dispose() {
		for (Plugin plugin: Vulkan.plugins) {
			plugin.dispose();
		}
		vkDestroyInstance(this.vkInstance, null);
	}

	public static String translateVulkanResult(int result) {
		switch (result) {
			// Success codes
			case VK_SUCCESS:
				return "Command successfully completed.";
			case VK_NOT_READY:
				return "A fence or query has not yet completed.";
			case VK_TIMEOUT:
				return "A wait operation has not completed in the specified time.";
			case VK_EVENT_SET:
				return "An event is signaled.";
			case VK_EVENT_RESET:
				return "An event is unsignaled.";
			case VK_INCOMPLETE:
				return "A return array was too small for the result.";
			case VK_SUBOPTIMAL_KHR:
				return "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

			// Error codes
			case VK_ERROR_OUT_OF_HOST_MEMORY:
				return "A host memory allocation has failed.";
			case VK_ERROR_OUT_OF_DEVICE_MEMORY:
				return "A device memory allocation has failed.";
			case VK_ERROR_INITIALIZATION_FAILED:
				return "Initialization of an object could not be completed for implementation-specific reasons.";
			case VK_ERROR_DEVICE_LOST:
				return "The logical or physical device has been lost.";
			case VK_ERROR_MEMORY_MAP_FAILED:
				return "Mapping of a memory object has failed.";
			case VK_ERROR_LAYER_NOT_PRESENT:
				return "A requested layer is not present or could not be loaded.";
			case VK_ERROR_EXTENSION_NOT_PRESENT:
				return "A requested extension is not supported.";
			case VK_ERROR_FEATURE_NOT_PRESENT:
				return "A requested feature is not supported.";
			case VK_ERROR_INCOMPATIBLE_DRIVER:
				return "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
			case VK_ERROR_TOO_MANY_OBJECTS:
				return "Too many objects of the type have already been created.";
			case VK_ERROR_FORMAT_NOT_SUPPORTED:
				return "A requested format is not supported on this device.";
			case VK_ERROR_SURFACE_LOST_KHR:
				return "A surface is no longer available.";
			case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
				return "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
			case VK_ERROR_OUT_OF_DATE_KHR:
				return "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
					+ "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue" + "presenting to the surface.";
			case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR:
				return "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an" + " image.";
			case VK_ERROR_VALIDATION_FAILED_EXT:
				return "A validation layer found an error.";
			default:
				return String.format("%s [%d]", "Unknown", Integer.valueOf(result));
		}
	}
}