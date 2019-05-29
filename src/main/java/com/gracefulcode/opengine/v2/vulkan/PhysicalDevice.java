package com.gracefulcode.opengine.v2.vulkan;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;

import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

/**
 * A physical device is a graphics card. These have limitations and features and the end user can choose between them.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1
 */
public class PhysicalDevice {
	/**
	 * A reference to lwjgl's version of a physical device.
	 */
	protected VkPhysicalDevice vkPhysicalDevice;

	/**
	 * The physical device properties that aren't specific to any window or surface.
	 */
	protected VkPhysicalDeviceProperties properties;
	protected VkPhysicalDeviceFeatures features;

	protected VkPhysicalDeviceLimits limits;

	public PhysicalDevice(long deviceId, VkInstance instance) {
		this.vkPhysicalDevice = new VkPhysicalDevice(deviceId, instance);

		this.properties = VkPhysicalDeviceProperties.calloc();
		vkGetPhysicalDeviceProperties(this.vkPhysicalDevice, this.properties);

		this.features = VkPhysicalDeviceFeatures.calloc();
		vkGetPhysicalDeviceFeatures(this.vkPhysicalDevice, this.features);

		System.out.println("----------------");
		System.out.println("Device Name:               " + this.properties.deviceNameString());
		System.out.println("API Version:               " + this.properties.apiVersion());
		System.out.println("Device ID:                 " + this.properties.deviceID());
		System.out.println("Device Type:               " + this.properties.deviceType());
		System.out.println("Driver Version:            " + this.properties.driverVersion());
		System.out.println("Geometry Shader:           " + this.features.geometryShader());
		System.out.println("Tessellation Shader:       " + this.features.tessellationShader());
		System.out.println("Fill Mode Non-Solid:       " + this.features.fillModeNonSolid());
		System.out.println("Depth Bounds:              " + this.features.depthBounds());
		System.out.println("Pipeline Statistics Query: " + this.features.pipelineStatisticsQuery());
		System.out.println("Shader Float64:            " + this.features.shaderFloat64());
		System.out.println("Shader Int64:              " + this.features.shaderInt64());
		System.out.println("Shader Int16:              " + this.features.shaderInt16());

		this.limits = this.properties.limits();

		IntBuffer ib = memAllocInt(1);
		vkGetPhysicalDeviceQueueFamilyProperties(this.vkPhysicalDevice, ib, null);
		int queueCount = ib.get(0);

		VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
		vkGetPhysicalDeviceQueueFamilyProperties(this.vkPhysicalDevice, ib, queueProps);

		System.out.println("Queue Count:    " + queueCount);
		int queueFamilyIndex;
		for (queueFamilyIndex = 0; queueFamilyIndex < queueCount; queueFamilyIndex++) {
			VkQueueFamilyProperties properties = queueProps.get(queueFamilyIndex);
			System.out.println("    " + queueFamilyIndex + ":");
			System.out.println("        Count: " + properties.queueCount());
			System.out.println("        Flags: " + properties.queueFlags());
			System.out.println("        Timestamp Valid Bits: " + properties.timestampValidBits());
		}

	}

	public int deviceType() {
		return this.properties.deviceType();
	}

	public int apiVersion() {
		return this.properties.apiVersion();
	}
}