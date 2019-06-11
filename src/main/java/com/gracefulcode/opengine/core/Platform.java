package com.gracefulcode.opengine.core;

public interface Platform<T extends Renderer> {
	public String name();
	public void configureRendererExtensions(T renderer);
	public void configureRendererLayers(T renderer);
}