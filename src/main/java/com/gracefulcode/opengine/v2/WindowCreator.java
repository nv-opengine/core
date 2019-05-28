package com.gracefulcode.opengine.v2;

/**
 * A WindowCreator is responsible for creating windows.
 * <p>
 * This is hiding behind an interface because we want to potentially support
 * more than just Vulkan.
 */
public interface WindowCreator<W extends Window> {
	/**
	 * Creates a new window with a specific configuration. This window will be
	 * managed by the WindowCreator if WindowCreator is the one calling this
	 * function.
	 *
	 * @param configuration The configuration of the resultant window.
	 * @return The created window.
	 */
	public W createWindow(Window.Configuration configuration);
}