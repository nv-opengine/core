package com.gracefulcode.opengine.v2.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

/**
 * A physical device is a graphics card. These have limitations and features and the end user can choose between them.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1
 */
public class PhysicalDevice implements com.gracefulcode.opengine.v2.PhysicalDevice {
	/**
	 * A reference to lwjgl's version of a physical device.
	 */
	protected VkPhysicalDevice vkPhysicalDevice;

	/**
	 * The physical device properties that aren't specific to any window or surface.
	 */
	protected VkPhysicalDeviceProperties properties;

	protected VkPhysicalDeviceLimits limits;

	public PhysicalDevice(long deviceId, VkInstance instance) {
		this.vkPhysicalDevice = new VkPhysicalDevice(deviceId, instance);

		this.properties = VkPhysicalDeviceProperties.calloc();
		vkGetPhysicalDeviceProperties(this.vkPhysicalDevice, this.properties);

		System.out.println("API Version: " + this.properties.apiVersion());
		System.out.println("Device ID: " + this.properties.deviceID());
		System.out.println("Device Name: " + this.properties.deviceNameString());
		System.out.println("Device Type: " + this.properties.deviceType());
		System.out.println("Driver Version: " + this.properties.driverVersion());

		this.limits = this.properties.limits();
	}

	public int deviceType() {
		return this.properties.deviceType();
	}

	public int apiVersion() {
		return this.properties.apiVersion();
	}
}