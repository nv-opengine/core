package com.gracefulcode.opengine;

public interface PhysicalDevice<T extends LogicalDevice> {
	public int deviceType();
	public int apiVersion();
	public T createLogicalDevice(String[] requiredExtensions, boolean hasGraphicsQueue, boolean hasComputeQueue);
}