package com.gracefulcode.opengine.v2;

import java.nio.FloatBuffer;

/**
 * The base class for many more specific classes that are basically four float
 * values.
 * <p>
 * Having a single base class makes things like translations and copies easier,
 * but having specific subclasses helps us rely on type safety. Using a color
 * as a world position is something you shouldn't be able to accidentally do.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1.1
 */
public class Vector4 {
	/**
	 * The backing float buffer. Using a floatBuffer instead of somethign like
	 * a Float[] or float[] allows us to interact with native code (ie, Vulkan)
	 * more efficiently.
	 */
	protected FloatBuffer floatBuffer;

	public Vector4() {
		this.floatBuffer = FloatBuffer.allocate(4);
	}
}