package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.PhysicalDevice;
import com.gracefulcode.opengine.Window;
import com.gracefulcode.opengine.WindowManager;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import java.util.ArrayList;
import java.util.TreeSet;

import org.lwjgl.PointerBuffer;

import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

/**
 * Handles all high-level Vulkan things. Right now this means the VkInstance,
 * layers and extensions. The goal is that you will be able to pass a single
 * Vulkan instance around and won't also have to pass around
 * VkDevice/VkPhysicalDevice, etc. There will probably be some amount of other
 * things you have to pass around thanks to queues and whatnot, but let's keep
 * that to a minimum.
 *
 * TODO: Set up debugging when I'm at a computer that supports it.
 */
public class Vulkan {
	public static class Configuration {
		public String applicationName;
		public boolean validation = true;
		public boolean needGraphics = true;
		public boolean needCompute = false;
		public int debugFlags = VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT;
		public PhysicalDeviceSelector physicalDeviceSelector = new DefaultPhysicalDeviceSelector();

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
	}

	protected Vulkan.Configuration configuration;
	protected VkPhysicalDevice selectedDevice;
	protected MemoryManager memoryManager;
	protected VkInstance instance;
	protected long callbackHandle;
	protected WindowManager<VulkanWindow, VulkanWindowCreator> windowManager;

	// Reuse this int buffer for many method calls.
	protected IntBuffer ib = memAllocInt(1);

	protected ArrayList<VulkanWindow> windows = new ArrayList<VulkanWindow>();
	protected TreeSet<VulkanPhysicalDevice> physicalDevices;

	public Vulkan() {
		this(new Vulkan.Configuration());
	}

	public Vulkan(Vulkan.Configuration configuration) {
		this.configuration = configuration;
		this.physicalDevices = new TreeSet<VulkanPhysicalDevice>(this.configuration.physicalDeviceSelector);

		if (!glfwVulkanSupported()) {
			throw new AssertionError("GLFW failed to find the Vulkan loader");
		}

		PointerBuffer extensions = this.getExtensions();
		PointerBuffer layers = this.getLayers();

		VkApplicationInfo appInfo = VkApplicationInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
			.pApplicationName(memUTF8(this.configuration.applicationName))
			.pEngineName(memUTF8("Opengine v0.1"))
			.apiVersion(VK_MAKE_VERSION(1, 0, 2));

		VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
			.pNext(NULL)
			.pApplicationInfo(appInfo)
			.ppEnabledExtensionNames(extensions)
			.ppEnabledLayerNames(layers);

		PointerBuffer pInstance = memAllocPointer(1);
		int err = vkCreateInstance(pCreateInfo, null, pInstance);
		long instance = pInstance.get(0);
		memFree(pInstance);

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create VkInstance: " + Vulkan.translateVulkanResult(err));
		}

		this.instance = new VkInstance(instance, pCreateInfo);

		boolean foundDebugging = false;
		for (int i = 0; i < extensions.limit(); i++) {
			String tmp = extensions.getStringUTF8(i);
			if (tmp.equals(VK_EXT_DEBUG_REPORT_EXTENSION_NAME)) {
				foundDebugging = true;
				this.setupDebugging(pCreateInfo);
			} else {
				// System.out.println("Extension: " + tmp);
			}
		}

		if (!foundDebugging) {
			System.out.println("Failed to find extension:" + VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
		}

		// Initialize all physical devices so that we can choose from them.
		this.initPhysicalDevices();
	}

	TreeSet<VulkanPhysicalDevice> getPhysicalDevices() {
		return this.physicalDevices;
	}

	public WindowManager<VulkanWindow, VulkanWindowCreator> getWindowManager() {
		return this.getWindowManager(new WindowManager.Configuration());
	}

	public WindowManager<VulkanWindow, VulkanWindowCreator> getWindowManager(WindowManager.Configuration wmConfiguration) {
		if (this.windowManager == null) {
			this.windowManager = new WindowManager<VulkanWindow, VulkanWindowCreator>(wmConfiguration, new VulkanWindowCreator(this));
		}

		return this.windowManager;
	}

	protected void initPhysicalDevices() {
		int err = vkEnumeratePhysicalDevices(this.instance, this.ib, null);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to get number of physical devices: " + translateVulkanResult(err));
		}

		int numPhysicalDevices = this.ib.get(0);

		PointerBuffer pPhysicalDevices = memAllocPointer(numPhysicalDevices);
		err = vkEnumeratePhysicalDevices(this.instance, this.ib, pPhysicalDevices);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to get physical devices: " + translateVulkanResult(err));
		}

		for (int i = 0; i < this.ib.get(0); i++) {
			long physicalDeviceId = pPhysicalDevices.get(i);
			VulkanPhysicalDevice physicalDevice = new VulkanPhysicalDevice(new VkPhysicalDevice(physicalDeviceId, this.instance));
			// System.out.println(physicalDevice);
			this.physicalDevices.add(physicalDevice);
		}
		memFree(pPhysicalDevices);
	}

	/**
	 * A pipeline layout defines what arguments we're going to be passing to
	 * our pipeline. In the case of a compute pipeline, this is basically the
	 * set of inputs.
	 */
	/*
	public Pipeline createComputePipeline(Shader shader, String entryMethod) {
		VkPipelineShaderStageCreateInfo stageCreate = VkPipelineShaderStageCreateInfo.calloc();
		stageCreate.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
		stageCreate.stage(VK_SHADER_STAGE_COMPUTE_BIT);
		stageCreate.module(shader.getId());
		stageCreate.pName(memUTF8(entryMethod));

		LongBuffer lb = memAllocLong(1);
		lb.put(shader.getLayout());
		lb.flip();

		VkPipelineLayoutCreateInfo pipelineCreate = VkPipelineLayoutCreateInfo.calloc();
		pipelineCreate.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
		pipelineCreate.pSetLayouts(lb);

		LongBuffer lb2 = memAllocLong(1);
		int err = vkCreatePipelineLayout(this.logicalDevice, pipelineCreate, null, lb2);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create pipeline layout: " + Vulkan.translateVulkanResult(err));
		}
		this.pipelineLayoutId = lb2.get(0);

		return null;
	}
	*/

	private final VkDebugReportCallbackEXT dbgFunc = VkDebugReportCallbackEXT.create(
        (flags, objectType, object, location, messageCode, pLayerPrefix, pMessage, pUserData) -> {
            String type;
            if ((flags & VK_DEBUG_REPORT_INFORMATION_BIT_EXT) != 0) {
                type = "INFORMATION";
            } else if ((flags & VK_DEBUG_REPORT_WARNING_BIT_EXT) != 0) {
                type = "WARNING";
            } else if ((flags & VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT) != 0) {
                type = "PERFORMANCE WARNING";
            } else if ((flags & VK_DEBUG_REPORT_ERROR_BIT_EXT) != 0) {
                type = "ERROR";
            } else if ((flags & VK_DEBUG_REPORT_DEBUG_BIT_EXT) != 0) {
                type = "DEBUG";
            } else {
                type = "UNKNOWN";
            }

            System.err.format(
                "%s: [%s] Code %d : %s\n",
                type, memASCII(pLayerPrefix), messageCode, VkDebugReportCallbackEXT.getString(pMessage)
            );

            /*
             * false indicates that layer should not bail-out of an
             * API call that had validation failures. This may mean that the
             * app dies inside the driver due to invalid parameter(s).
             * That's what would happen without validation layers, so we'll
             * keep that behavior here.
             */
            return VK_FALSE;
        }
    );

	protected void setupDebugging(VkInstanceCreateInfo createInfo) {
		int flags = this.configuration.debugFlags;

		VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
			.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT) // <- the struct type
			.pNext(NULL) // <- must be NULL
			.pfnCallback(dbgFunc) // <- the actual function pointer (in LWJGL a Callback)
			.pUserData(NULL) // <- any user data provided to the debug report callback function
			.flags(flags); // <- indicates which kind of messages we want to receive

		// createInfo.pNext(dbgCreateInfo.address());
		// createInfo.pNext(NULL);

		LongBuffer pCallback = memAllocLong(1); // <- allocate a LongBuffer (for a non-dispatchable handle)
		// Actually create the debug report callback

		int err = vkCreateDebugReportCallbackEXT(this.instance, dbgCreateInfo, null, pCallback);
		this.callbackHandle = pCallback.get(0);
		System.out.println("Callback handle: " + this.callbackHandle);
		memFree(pCallback); // <- and free the LongBuffer
		dbgCreateInfo.free(); // <- and also the create-info struct
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create VkInstance: " + translateVulkanResult(err));
		}
	}

	// public Shader createComputeShader(String fileName) throws FileNotFoundException, IOException {
	// 	// Do we want to eventually have some sort of shader controller?
	// 	Shader shader = new Shader(this.logicalDevice, "comp.spv", VK_SHADER_STAGE_COMPUTE_BIT);
	// 	return shader;
	// }

	public VkInstance getInstance() {
		return this.instance;
	}

	public Image clearImage(Image inputImage) {
		return null;
	}

	public void doneAllocating() {
		this.memoryManager.doneAllocating();
	}

	public MemoryManager.Buffer createComputeBuffer(String name, int bytes) {
		return this.memoryManager.createExclusiveComputeBuffer(name, bytes);
	}

	public void dispose() {
		memFree(this.ib);
		this.windowManager.dispose();
		System.out.println("Destroying callback: " + this.callbackHandle);
		vkDestroyDebugReportCallbackEXT(
			this.instance,
			this.callbackHandle,
			null
		);
		vkDestroyInstance(this.instance, null);
	}

	protected PointerBuffer getLayers() {
		PointerBuffer layers = memAllocPointer(this.configuration.desiredLayers.length);

		vkEnumerateInstanceLayerProperties(this.ib, null);
		int result = this.ib.get(0);

		VkLayerProperties.Buffer buffer = VkLayerProperties.calloc(result);
		vkEnumerateInstanceLayerProperties(this.ib, buffer);

		// System.out.println("----------");
		// System.out.println("LAYERS");
		// System.out.println("----------");		

		int limit = buffer.limit();
		for (int m = 0; m < limit; m++) {
			boolean found = false;
			for (int i = 0; i < this.configuration.desiredLayers.length; i++) {
				buffer.position(m);
				if (buffer.layerNameString().equals(this.configuration.desiredLayers[i])) {
					// System.out.println("    +(" + m + "): " + buffer.layerNameString());
					layers.put(memUTF8(this.configuration.desiredLayers[i]));
					found = true;
					break;					
				}
			}
			// if (!found) {
			// 	System.out.println("    -(" + m + "): " + buffer.layerNameString());					
			// }
		}
		layers.flip();

		return layers;
	}

	protected PointerBuffer getExtensions() {
		// System.out.println("----------");
		// System.out.println("EXTENSIONS");
		// System.out.println("----------");

		PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
		if (requiredExtensions == null) {
			throw new AssertionError("Failed to find list of required Vulkan extensions");
		}

		vkEnumerateInstanceExtensionProperties((ByteBuffer)null, this.ib, null);
		int result = this.ib.get(0);

		VkExtensionProperties.Buffer buffer = VkExtensionProperties.calloc(result);
		vkEnumerateInstanceExtensionProperties((ByteBuffer)null, this.ib, buffer);

		result = this.ib.get(0);

		int limit = buffer.limit();

		PointerBuffer ppEnabledExtensionNames = memAllocPointer(requiredExtensions.remaining() + this.configuration.desiredExtensions.length);
		ppEnabledExtensionNames.put(requiredExtensions);

		for (int m = 0; m < limit; m++) {
			buffer.position(m);
			boolean didFind = false;
			for (int i = 0; i < requiredExtensions.limit(); i++) {
				for (int p = 0; p < this.configuration.desiredExtensions.length; p++) {
					if (requiredExtensions.getStringASCII(i).equals(buffer.extensionNameString())) {
						// System.out.println("    +(" + m + "): " + buffer.extensionNameString());
						didFind = true;
						break;
					}
				}
			}
			if (!didFind) {
				for (int i = 0; i < this.configuration.desiredExtensions.length; i++) {
					if (buffer.extensionNameString().equals(this.configuration.desiredExtensions[i])) {
						// System.out.println("    +(" + m + "): " + buffer.extensionNameString());
						didFind = true;
						ppEnabledExtensionNames.put(memUTF8(this.configuration.desiredExtensions[i]));
						break;
					}
				}
			}

			// if (!didFind) {
			// 	System.out.println("    -(" + m + "): " + buffer.extensionNameString());
			// }
		}

		ppEnabledExtensionNames.flip();
		buffer.free();

		return ppEnabledExtensionNames;
	}

	public static String translatePresentMode(int presentMode) {
		switch(presentMode) {
			case VK_PRESENT_MODE_IMMEDIATE_KHR:
				return "Immediate";
			case VK_PRESENT_MODE_MAILBOX_KHR:
				return "Mailbox";
			case VK_PRESENT_MODE_FIFO_KHR:
				return "FIFO";
			case VK_PRESENT_MODE_FIFO_RELAXED_KHR:
				return "FIFO Relaxed";
			// case VK_PRESENT_MODE_SHARED_DEMAND_REFRESH_KHR:
			// 	return "Shared Demand Refresh";
			// case VK_PRESENT_MODE_SHARED_CONTINUOUS_REFRESH_KHR:
			// 	return "Shared Continuous Refresh";
			default:
				return "Unknown: " + presentMode;
		}
	}

	public static String translateFormat(int format) {
		switch(format) {
			case VK_FORMAT_UNDEFINED:
				return "Undefined";
			case VK_FORMAT_B8G8R8A8_UNORM:
				return "VK_FORMAT_B8G8R8A8_UNORM";
			case VK_FORMAT_B8G8R8A8_SRGB:
				return "VK_FORMAT_B8G8R8A8_SRGB";
			default:
				return "Unknown: " + format;
		}
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