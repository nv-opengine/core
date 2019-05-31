package com.gracefulcode.opengine.vulkan;

import static org.lwjgl.system.MemoryUtil.*;

import java.util.HashMap;

import org.lwjgl.PointerBuffer;

/**
 * Keeps track of layer requirements and conflicts.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1.1
 */
public class LayerConfiguration {
	public static enum RequireType {
		DONT_CARE,
		NOT_DESIRED,
		DESIRED,
		REQUIRED
	}

	protected HashMap<String, RequireType> layers = new HashMap<String, RequireType>();
	protected boolean isLocked = false;

	public LayerConfiguration() {

	}

	public String toString() {
		return this.layers.toString();
	}

	public void lock() {
		this.isLocked = true;
	}

	public PointerBuffer getConfiguredLayers() {
		int i = 0;
		for (String key: this.layers.keySet()) {
			if (this.shouldHave(key)) i++;
		}

		PointerBuffer ret = memAllocPointer(i);
		for (String key: this.layers.keySet()) {
			if (this.shouldHave(key)) ret.put(memUTF8(key));
		}
		ret.flip();
		return ret;
	}

	public void setLayer(String layerName, RequireType requireType) {
		if (this.isLocked) throw new AssertionError("Trying to set extension after the configuration is locked.");

		if (!this.layers.containsKey(layerName)) {
			this.layers.put(layerName, requireType);
			return;
		}

		RequireType previousValue = this.layers.get(layerName);
		switch (previousValue) {
			case DONT_CARE:
			case DESIRED:
				this.layers.put(layerName, requireType);
				break;
			case NOT_DESIRED:
				if (requireType == RequireType.REQUIRED) {
					throw new AssertionError(layerName + " is both required and not desired.");
				}
				break;
			case REQUIRED:
				if (requireType == RequireType.NOT_DESIRED) {
					throw new AssertionError(layerName + " is both required and not desired.");
				}
				break;
		}
	}

	public RequireType getRequireType(String layerName) {
		if (this.layers.containsKey(layerName))
			return this.layers.get(layerName);
		return RequireType.DONT_CARE;
	}

	public boolean shouldHave(String layerName) {
		if (this.layers.containsKey(layerName)) {
			switch (this.layers.get(layerName)) {
				case REQUIRED:
				case DESIRED:
					return true;
			}
		}
		return false;
	}
}