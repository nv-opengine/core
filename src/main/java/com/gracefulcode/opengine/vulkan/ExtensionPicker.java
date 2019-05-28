package com.gracefulcode.opengine.vulkan;

import java.util.Set;

public interface ExtensionPicker {
	public void needsExtension(String extensionName, Set<String> extensionsUsed);
}