package com.gracefulcode.opengine.v2.vulkan.plugins;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.VK10.*;

import com.gracefulcode.opengine.v2.vulkan.ExtensionConfiguration;
import com.gracefulcode.opengine.v2.vulkan.LayerConfiguration;
import com.gracefulcode.opengine.v2.vulkan.PhysicalDevice;
import com.gracefulcode.opengine.v2.vulkan.Vulkan;

import java.nio.LongBuffer;

import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

/**
 * The Debug plugin enables all of the debugging utilities in Vulkan, if
 * possible. It then (by default) logs a lot of things to stderr. You can
 * override info, warning, etc. to customize that. Debugging layers can be a
 * big performance hit, so you should turn them off in production.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1.1
 */
public class Debug implements Plugin {
	protected long callbackHandle;
	protected VkInstance vkInstance;
	protected boolean isRequired;

	public Debug(boolean isRequired) {
		this.isRequired = isRequired;
	}

	public void setupExtensions(ExtensionConfiguration configuration) {
		configuration.setExtension("VK_EXT_debug_report", this.isRequired ? ExtensionConfiguration.RequireType.REQUIRED : ExtensionConfiguration.RequireType.DESIRED);
	}

	public void setupLayers(LayerConfiguration configuration) {
		configuration.setLayer("VK_LAYER_LUNARG_core_validation", LayerConfiguration.RequireType.REQUIRED);
	}

	protected void info(int objectType, long object, long location, int messageCode, String pLayerString, String message, long pUserData) {
		System.err.format(
			"INFO: [%s] Code %d: %s\n",
			pLayerString,
			messageCode,
			message
		);
	}

	protected void warning(int objectType, long object, long location, int messageCode, String pLayerString, String message, long pUserData) {
		System.err.format(
			"WARNING: [%s] Code %d: %s\n",
			pLayerString,
			messageCode,
			message
		);
	}

	protected void performance(int objectType, long object, long location, int messageCode, String pLayerString, String message, long pUserData) {
		System.err.format(
			"PERFORMANCE: [%s] Code %d: %s\n",
			pLayerString,
			messageCode,
			message
		);
	}

	protected void error(int objectType, long object, long location, int messageCode, String pLayerString, String message, long pUserData) {
		System.err.format(
			"ERROR: [%s] Code %d: %s\n",
			pLayerString,
			messageCode,
			message
		);
	}

	protected void debug(int objectType, long object, long location, int messageCode, String pLayerString, String message, long pUserData) {
		System.err.format(
			"DEBUG: [%s] Code %d: %s\n",
			pLayerString,
			messageCode,
			message
		);
	}

	protected void unknown(int objectType, long object, long location, int messageCode, String pLayerString, String message, long pUserData) {
		System.err.format(
			"UNKNOWN: [%s] Code %d: %s\n",
			pLayerString,
			messageCode,
			message
		);
	}

	public void setupCreateInfo(VkInstanceCreateInfo instanceCreateInfo) {
	}

	public void postCreate(Vulkan vulkan, VkInstance instance, ExtensionConfiguration extensionConfiguration, LayerConfiguration LayerConfiguration) {
		if (!extensionConfiguration.shouldHave("VK_EXT_debug_report")) return;

		this.vkInstance = instance;
		int flags = VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT | VK_DEBUG_REPORT_INFORMATION_BIT_EXT | VK_DEBUG_REPORT_DEBUG_BIT_EXT | VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT;

		VkDebugReportCallbackCreateInfoEXT createInfo = VkDebugReportCallbackCreateInfoEXT.calloc();
		createInfo.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT);
		createInfo.pNext(NULL);
		createInfo.pfnCallback(dbgFunc);
		createInfo.pUserData(NULL);
		createInfo.flags(flags);

		LongBuffer pCallback = memAllocLong(1);
		int err = vkCreateDebugReportCallbackEXT(instance, createInfo, null, pCallback);
		this.callbackHandle = pCallback.get(0);
		memFree(pCallback);
		createInfo.free();
		if (err != VK_SUCCESS) {
			throw new AssertionError("Failed to create debug callback: " + Vulkan.translateVulkanResult(err));
		}
	}

	public void dispose() {
		if (this.vkInstance == null) return;
		
		vkDestroyDebugReportCallbackEXT(
			this.vkInstance,
			this.callbackHandle,
			null
		);
	}

	private final VkDebugReportCallbackEXT dbgFunc = VkDebugReportCallbackEXT.create(
		(flags, objectType, object, location, messageCode, pLayerPrefix, pMessage, pUserData) -> {
			String layer = memASCII(pLayerPrefix);

			if ((flags & VK_DEBUG_REPORT_INFORMATION_BIT_EXT) != 0) {
				this.info(objectType, object, location, messageCode, layer, VkDebugReportCallbackEXT.getString(pMessage), pUserData);
			} else if ((flags & VK_DEBUG_REPORT_WARNING_BIT_EXT) != 0) {
				this.warning(objectType, object, location, messageCode, layer, VkDebugReportCallbackEXT.getString(pMessage), pUserData);
			} else if ((flags & VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT) != 0) {
				this.performance(objectType, object, location, messageCode, layer, VkDebugReportCallbackEXT.getString(pMessage), pUserData);
			} else if ((flags & VK_DEBUG_REPORT_ERROR_BIT_EXT) != 0) {
				this.error(objectType, object, location, messageCode, layer, VkDebugReportCallbackEXT.getString(pMessage), pUserData);
			} else if ((flags & VK_DEBUG_REPORT_DEBUG_BIT_EXT) != 0) {
				this.debug(objectType, object, location, messageCode, layer, VkDebugReportCallbackEXT.getString(pMessage), pUserData);
			} else {
				this.unknown(objectType, object, location, messageCode, layer, VkDebugReportCallbackEXT.getString(pMessage), pUserData);
			}

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
}