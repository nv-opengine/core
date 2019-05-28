package com.gracefulcode.opengine.vulkan;

import java.util.Set;

public class DefaultExtensionPicker implements ExtensionPicker {
	public void needsExtension(String extensionName, Set<String> extensionsUsed) {
		extensionsUsed.add(extensionName);
	}	
}