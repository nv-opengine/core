package com.gracefulcode.opengine.core;

public interface Renderer<T, M, N extends ExtensionConfiguration<T, M>, Z extends LayerConfiguration<T, M>> {
	public N getExtensionConfiguration();
	public Z getLayerConfiguration();
}