package com.gracefulcode.opengine.core;

/**
 * ExtensionConfiguration holds information about what extensions are needed in
 * a way that is generic. That is, it is not vulkan or gles specific.
 * <p>
 * The intention is that things work this way:
 *     First, we get a list of all extensions that we CAN support. These are
 *     initially inserted as DONT_CARE.
 *     Then we lock this ExtensionConfiguration, which means that we cannot add
 *     any new items to the list.
 *     Then we go through all of the extensions that we need and set it as
 *     either DESIRED or REQUIRED.
 *     If there is a conflict here (both NOT_DESIRED and REQUIRED), then we
 *     throw an exception.
 *     If everything works well, we can get our extensions via
 *     getConfiguredExtensions().
 */
public interface ExtensionConfiguration<T, M> {
	public void lock();
	public T getConfiguredExtensions();
	public void setExtension(M extension, Ternary required);
	public Ternary getRequireType(M extension);
	public boolean shouldHave(M extension);
}