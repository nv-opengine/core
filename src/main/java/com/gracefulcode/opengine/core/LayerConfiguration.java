package com.gracefulcode.opengine.core;

/**
 * LayerConfiguration holds information about what layers are needed in
 * a way that is generic. That is, it is not vulkan or gles specific.
 * <p>
 * The intention is that things work this way:
 *     First, we get a list of all layers that we CAN support. These are
 *     initially inserted as DONT_CARE.
 *     Then we lock this LayerConfiguration, which means that we cannot add
 *     any new items to the list.
 *     Then we go through all of the layers that we need and set it as
 *     either DESIRED or REQUIRED.
 *     If there is a conflict here (both NOT_DESIRED and REQUIRED), then we
 *     throw an exception.
 *     If everything works well, we can get our layers via
 *     getConfiguredLayers().
 */
public interface LayerConfiguration<T, M> {
	public void lock();
	public T getConfiguredLayers();
	public void setLayer(M layer, Ternary required);
	public Ternary getRequireType(M layer);
	public boolean shouldHave(M layer);
}