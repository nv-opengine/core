package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;

import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

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
		public PhysicalDeviceSelector physicalDeviceSelector = new DefaultPhysicalDeviceSelector();

		/**
		 * These are layers that we will enable if we can, but it's not critical.
		 * Unfortunately MoltenVK doesn't support the debug layers, so we cannot
		 * die if we don't have them. It does make debugging a lot harder, though.
		 */
		public String[] desiredLayers = {
			"VK_LAYER_LUNARG_standard_validation"
		};

		/**
		 * These are extensions that we will enable if we can, but it's not
		 * critical. Unfortunately MoltenVK doesn't support the debug layers, so
		 * we cannot die if we don't have them. It does make debugging a lot
		 * harder, though.
		 */
		public String[] desiredExtensions = {
			VK_EXT_DEBUG_REPORT_EXTENSION_NAME
		};
	}

	protected Vulkan.Configuration configuration;
	protected VkPhysicalDevice selectedDevice;
	protected VkDevice logicalDevice;
	protected MemoryManager memoryManager;
	protected VkInstance instance;
	protected CommandPool graphicsPool;
	protected CommandPool computePool;

	// TODO: This probably doesn't belong in Vulkan, but in some other object
	// you can have more than one of.
	protected long pipelineLayoutId;

	// TODO: Do we wrap these?
	protected int graphicsQueueIndex = -1;
	protected int computeQueueIndex = -1;

	// Reuse this int buffer for many method calls.
	protected IntBuffer ib = memAllocInt(1);

	public Vulkan() {
		this(new Vulkan.Configuration());
	}

	public Vulkan(Vulkan.Configuration configuration) {
		this.configuration = configuration;

		if (!glfwVulkanSupported()) {
			throw new AssertionError("GLFW failed to find the Vulkan loader");
		}

		VkApplicationInfo appInfo = VkApplicationInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
			.pApplicationName(memUTF8(this.configuration.applicationName))
			.pEngineName(memUTF8("GOTBK"))
			.apiVersion(VK_MAKE_VERSION(1, 0, 2));

		PointerBuffer extensions = this.getExtensions();
		PointerBuffer layers = this.getLayers();

		for (int i = 0; i < layers.limit(); i++) {
			String tmp = layers.getStringUTF8(i);
			if (tmp.equals(VK_EXT_DEBUG_REPORT_EXTENSION_NAME)) {
				this.setupDebugging();
			}
		}
		layers.flip();

		VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO) // <- identifies what kind of struct this is (this is useful for extending the struct type later)
			.pNext(NULL) // <- must always be NULL until any next Vulkan version tells otherwise
			.pApplicationInfo(appInfo) // <- the application info we created above
			.ppEnabledExtensionNames(extensions)
			.ppEnabledLayerNames(layers);

		PointerBuffer pInstance = memAllocPointer(1); // <- create a PointerBuffer which will hold the handle to the created VkInstance
		int err = vkCreateInstance(pCreateInfo, null, pInstance); // <- actually create the VkInstance now!
		long instance = pInstance.get(0); // <- get the VkInstance handle
		memFree(pInstance); // <- free the PointerBuffer

		// One word about freeing memory:
		// Every host-allocated memory directly or indirectly referenced via a parameter to any Vulkan function can always
		// be freed right after the invocation of the Vulkan function returned.

		// Check whether we succeeded in creating the VkInstance
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create VkInstance: " + Vulkan.translateVulkanResult(err));
		}

		// Create an object-oriented wrapper around the simple VkInstance long handle
		// This is needed by LWJGL to later "dispatch" (i.e. direct calls to) the right Vukan functions.
		this.instance = new VkInstance(instance, pCreateInfo);

		err = vkEnumeratePhysicalDevices(this.instance, this.ib, null);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to get number of physical devices: " + translateVulkanResult(err));
		}

		int numPhysicalDevices = this.ib.get(0);

		PointerBuffer pPhysicalDevices = memAllocPointer(numPhysicalDevices);
		err = vkEnumeratePhysicalDevices(this.instance, this.ib, pPhysicalDevices);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to get physical devices: " + translateVulkanResult(err));
		}

		VkPhysicalDevice currentBest = null;
		VkPhysicalDeviceProperties currentBestProperties = null;

		for (int i = 0; i < this.ib.get(0); i++) {
			long physicalDeviceId = pPhysicalDevices.get(i);
			VkPhysicalDevice physicalDevice = new VkPhysicalDevice(physicalDeviceId, this.instance);
			VkPhysicalDeviceProperties pProperties = VkPhysicalDeviceProperties.calloc();
			vkGetPhysicalDeviceProperties(physicalDevice, pProperties);

			if (currentBest == null) {
				currentBest = physicalDevice;
				currentBestProperties = pProperties;
				continue;
			}

			if (this.configuration.physicalDeviceSelector.compare(pProperties, currentBestProperties) > 0) {
				currentBestProperties.free();

				currentBest = physicalDevice;
				currentBestProperties = pProperties;
			} else {
				pProperties.free();
			}
		}
		memFree(pPhysicalDevices);
		currentBestProperties.free();

		this.selectedDevice = currentBest;

		vkGetPhysicalDeviceQueueFamilyProperties(this.selectedDevice, this.ib, null);
		int queueCount = this.ib.get(0);
		VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
		vkGetPhysicalDeviceQueueFamilyProperties(this.selectedDevice, this.ib, queueProps);

		// TODO: Things get simpler and possibly more performant if we use the
		// same queue. As-is we simply take the last one, which is not
		// necessarily the most performant. Do we want to do what we did with
		// PhysicalDevice and make there be a Comparator?
		int queueFamilyIndex;
		for (queueFamilyIndex = 0; queueFamilyIndex < queueCount; queueFamilyIndex++) {
			VkQueueFamilyProperties properties = queueProps.get(queueFamilyIndex);
			int flags = properties.queueFlags();

			if ((flags & VK_QUEUE_GRAPHICS_BIT) != 0)
				this.graphicsQueueIndex = queueFamilyIndex;
			if ((flags & VK_QUEUE_COMPUTE_BIT) != 0)
				this.computeQueueIndex = queueFamilyIndex;
		}

		// TODO: Do we ever want to make multiple in order to use different
		// priorities? Do we want to expose that to the user?
		System.out.println("Graphics: " + this.graphicsQueueIndex);
		VkQueueFamilyProperties properties = queueProps.get(this.graphicsQueueIndex);
		System.out.println("\tMax Queues: " + properties.queueCount());

		System.out.println("Compute: " + this.computeQueueIndex);
		properties = queueProps.get(this.computeQueueIndex);
		System.out.println("\tMax Queues: " + properties.queueCount());

		// TODO: If we don't have queues on this device, perhaps we can go back
		// and try a different physical device?
		if (this.configuration.needGraphics && this.graphicsQueueIndex == -1) {
			throw new AssertionError("Need graphics, but Vulkan does not support a graphics queue.");
		}

		if (this.configuration.needCompute && this.computeQueueIndex == -1) {
			throw new AssertionError("Need compute, but Vulkan does not support a compute queue.");
		}

		int numQueues = 0;
		if (this.configuration.needGraphics) numQueues++;
		if (this.configuration.needCompute) numQueues++;

		if (numQueues == 0) {
			throw new AssertionError("Asked for zero queues. Vulkan will not be useful without any queues.");
		}

		// TODO: When/if we have multiple queues, the priorities are actually important.
		// For now they mean nothing.
		FloatBuffer pQueuePriorities = memAllocFloat(1).put(0.0f);
		pQueuePriorities.flip();

		VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1);

		int idx = 0;
		if (this.configuration.needGraphics) {
			queueCreateInfo.position(idx++);
			queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
			queueCreateInfo.queueFamilyIndex(this.graphicsQueueIndex);
			queueCreateInfo.pQueuePriorities(pQueuePriorities);
		}
		if (this.configuration.needCompute) {
			queueCreateInfo.position(idx++);
			queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
			queueCreateInfo.queueFamilyIndex(this.computeQueueIndex);
			queueCreateInfo.pQueuePriorities(pQueuePriorities);
		}

		VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
			.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
			.pNext(NULL)
			.pQueueCreateInfos(queueCreateInfo)
			.ppEnabledExtensionNames(null)
			.ppEnabledLayerNames(null);

		PointerBuffer pDevice = memAllocPointer(1);
		err = vkCreateDevice(this.selectedDevice, deviceCreateInfo, null, pDevice);
		long device = pDevice.get(0);
		this.logicalDevice = new VkDevice(device, this.selectedDevice, deviceCreateInfo);
		this.memoryManager = new MemoryManager(this.logicalDevice);

		memFree(pDevice);
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create device: " + translateVulkanResult(err));
		}

		// TODO: If these are the same queue, won't we create two?
		if (this.configuration.needGraphics) {
			this.graphicsPool = new CommandPool(this.logicalDevice, this.graphicsQueueIndex);
		}
		if (this.configuration.needCompute) {
			this.computePool = new CommandPool(this.logicalDevice, this.computeQueueIndex);
		}

		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create command pool: " + translateVulkanResult(err));
		}
	}

	/**
	 * A pipeline layout defines what arguments we're going to be passing to
	 * our pipeline. In the case of a compute pipeline, this is basically the
	 * set of inputs.
	 */
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

	protected void setupDebugging() {
		// TODO: Allow this to be changed.
		int flags = VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT;

		// TODO: Allow this to be changed.
		final VkDebugReportCallbackEXT callback = new VkDebugReportCallbackEXT() {
			public int invoke(
				int flags,
				int objectType,
				long object,
				long location,
				int messageCode,
				long pLayerPrefix,
				long pMessage,
				long pUserData
			) {
				System.err.println("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage));
				return 0;
			}
		};

		VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
			.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT) // <- the struct type
			.pNext(NULL) // <- must be NULL
			.pfnCallback(callback) // <- the actual function pointer (in LWJGL a Callback)
			.pUserData(NULL) // <- any user data provided to the debug report callback function
			.flags(flags); // <- indicates which kind of messages we want to receive

		LongBuffer pCallback = memAllocLong(1); // <- allocate a LongBuffer (for a non-dispatchable handle)
		// Actually create the debug report callback
		int err = vkCreateDebugReportCallbackEXT(this.instance, dbgCreateInfo, null, pCallback);
		long callbackHandle = pCallback.get(0);
		memFree(pCallback); // <- and free the LongBuffer
		dbgCreateInfo.free(); // <- and also the create-info struct
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create VkInstance: " + translateVulkanResult(err));
		}
        // return callbackHandle;
	}

	public Shader createComputeShader(String fileName) throws FileNotFoundException, IOException {
		// Do we want to eventually have some sort of shader controller?
		Shader shader = new Shader(this.logicalDevice, "comp.spv", VK_SHADER_STAGE_COMPUTE_BIT);
		return shader;
	}

	public VkDevice getLogicalDevice() {
		return this.logicalDevice;
	}

	public void doneAllocating() {
		this.memoryManager.doneAllocating();
	}

	public MemoryManager.Buffer createComputeBuffer(String name, int bytes) {
		return this.memoryManager.createExclusiveComputeBuffer(name, bytes);
	}

	public void dispose() {
		memFree(this.ib);
	}

	protected PointerBuffer getLayers() {
		PointerBuffer layers = memAllocPointer(this.configuration.desiredLayers.length);

		vkEnumerateInstanceLayerProperties(this.ib, null);
		int result = this.ib.get(0);

		VkLayerProperties.Buffer buffer = VkLayerProperties.calloc(result);
		vkEnumerateInstanceLayerProperties(this.ib, buffer);

		int limit = buffer.limit();
		for (int i = 0; i < this.configuration.desiredLayers.length; i++) {
			for (int m = 0; m < limit; m++) {
				buffer.position(m);
				if (buffer.layerNameString().equals(this.configuration.desiredLayers[i])) {
					System.out.println("Enabling Layer(" + m + "): " + buffer.layerNameString());
					layers.put(memUTF8(this.configuration.desiredLayers[i]));
					break;
				} else {
					System.out.println("NOT Enabling Layer(" + m + "): " + buffer.layerNameString());					
				}
			}
		}
		layers.flip();

		return layers;
	}

	protected PointerBuffer getExtensions() {
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

		for (int i = 0; i < this.configuration.desiredExtensions.length; i++) {
			for (int m = 0; m < limit; m++) {
				buffer.position(m);
				if (buffer.extensionNameString().equals(this.configuration.desiredExtensions[i])) {
					// Good!
					System.out.println("Enabling Extension(" + m + "): " + buffer.extensionNameString());
					ppEnabledExtensionNames.put(memUTF8(this.configuration.desiredExtensions[i]));
					break;
				} else {
					System.out.println("NOT Enabling Extension(" + m + "): " + buffer.extensionNameString());
				}
			}
		}
		ppEnabledExtensionNames.flip();
		buffer.free();

		return ppEnabledExtensionNames;
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