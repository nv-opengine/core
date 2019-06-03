package com.gracefulcode.opengine;

/**
 * Represents a single user-visible window.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1
 */
public interface Window {
	/**
	 * Represents a buffer strategy.
	 */
	public static enum BufferType {
		/**
		 * Single buffering is where we draw immediately. Tearing is possible.
		 */
		SINGLE,
		/**
		 * Double buffering is the simplest method that really buffers. Two
		 * buffers that simply swap back and forth. No tearing.
		 */
		DOUBLE,
		/**
		 * Mailbox is a form of triple buffering that attempts to reduce
		 * latency when the framerate suffers on the GPU. In happy cases is not
		 * much different than double buffering.
		 */
		MAILBOX
	};

	/**
	 * The initial configuration for this window. Many of these values can be
	 * changed programmatically later, or changed by the user themselves.
	 */
	public static class Configuration {
		/**
		 * The title in the title bar.
		 */
		public String title = "";

		/**
		 * Initial window width.
		 */
		public int width = 800;

		/**
		 * Initial window height.
		 */
		public int height = 600;

		/**
		 * What color the framebuffer is automatically cleared to. Yes, it has
		 * alpha.
		 */
		public float[] clearColor;

		/**
		 * The buffer type that this window uses. Cannot change after the fact.
		 */
		public Window.BufferType bufferType = Window.BufferType.MAILBOX;

		public KeyboardCallback keyboardCallback = null;
	}

	/**
	 * Whether this window should be closed. This is essentially an interface
	 * to the close button provided by the OS.
	 */
	public boolean shouldClose();

	/**
	 * A way to programmatically close the window.
	 */
	public void close();

	/**
	 * Clear memory.
	 */
	public void dispose();
}