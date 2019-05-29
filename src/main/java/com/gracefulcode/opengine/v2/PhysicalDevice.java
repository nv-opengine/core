package com.gracefulcode.opengine.v2;

public interface PhysicalDevice {
	public enum DeviceType {
		UNKNOWN,
		INTEGRATED,
		DISCRETE
	};

	public int deviceType();
	public int apiVersion();
}