package com.gracefulcode.opengine.vulkan;

/**
 * Image is used in the building of a pipeline. It doesn't represent a concrete
 * image, it is used to collect information about how the image is used as the
 * user is defining their pipeline.
 *
 * Once building is done, this abstract Image concept will turn into one or
 * more concrete images. The exact number, format, etc. will be determined by
 * the backend, based on how the image is used.
 */
public interface Image {
	
}