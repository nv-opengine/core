package com.gracefulcode.opengine;

/**
 * Represents a set of logically-identical images.
 * These images might not be fully formed -- we might not know their format, etc.
 * Pass these around, collect info on how they are used in order to figure out their info.
 */
public interface ImageSet {
	/**
	 * In some cases we are figuring out the resolution of an image. This field
	 * lets you know if the resolution of the image is a known thing.
	 */
	public boolean knownResolution();

	/**
	 * Get the size of the image. Only valid if knownResolution returns true.
	 */
	public int getWidth();
	public int getHeight();	

	/**
	 * Returns true if this memory has already been accounted for. If you're
	 * starting with a framebuffer image as given by Window, this will be true
	 * since Vulkan made that image for us. Otherwise, it'll be false until we
	 * finalize things and create memory.
	 */
	public boolean memoryBacked();
}