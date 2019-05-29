package com.gracefulcode.opengine.v2.vulkan;

import static org.lwjgl.system.MemoryUtil.*;

import java.util.HashMap;

import org.lwjgl.PointerBuffer;

/**
 * Keeps track of extension requirements and conflicts.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1.1
 */
public class ExtensionConfiguration {
	public static enum RequireType {
		DONT_CARE,
		NOT_DESIRED,
		DESIRED,
		REQUIRED
	}

	protected HashMap<String, RequireType> extensions = new HashMap<String, RequireType>();
	protected boolean isLocked = false;

	public ExtensionConfiguration() {

	}

	public String toString() {
		return this.extensions.toString();
	}

	public void lock() {
		this.isLocked = true;
	}

	public PointerBuffer getConfiguredExtensions() {
		int i = 0;
		for (String key: this.extensions.keySet()) {
			if (this.shouldHave(key)) i++;
		}

		PointerBuffer ret = memAllocPointer(i);
		for (String key: this.extensions.keySet()) {
			if (this.shouldHave(key)) ret.put(memUTF8(key));
		}
		ret.flip();
		return ret;
	}

	public void setExtension(String extensionName, RequireType requireType) {
		if (this.isLocked) throw new AssertionError("Trying to set extension after the configuration is locked.");

		if (!this.extensions.containsKey(extensionName)) {
			this.extensions.put(extensionName, requireType);
			return;
		}

		RequireType previousValue = this.extensions.get(extensionName);
		switch (previousValue) {
			case DONT_CARE:
			case DESIRED:
				this.extensions.put(extensionName, requireType);
				break;
			case NOT_DESIRED:
				if (requireType == RequireType.REQUIRED) {
					throw new AssertionError(extensionName + " is both required and not desired.");
				}
				break;
			case REQUIRED:
				if (requireType == RequireType.NOT_DESIRED) {
					throw new AssertionError(extensionName + " is both required and not desired.");
				}
				break;
		}
	}

	public RequireType getRequireType(String extensionName) {
		if (this.extensions.containsKey(extensionName))
			return this.extensions.get(extensionName);
		return RequireType.DONT_CARE;
	}

	public boolean shouldHave(String extensionName) {
		if (this.extensions.containsKey(extensionName)) {
			switch (this.extensions.get(extensionName)) {
				case REQUIRED:
				case DESIRED:
					return true;
			}
		}
		return false;
	}
}