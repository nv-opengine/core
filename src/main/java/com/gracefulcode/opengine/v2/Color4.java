package com.gracefulcode.opengine.v2;

/**
 * A specialization of Vector4 being used for colors.
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1.1
 * @since 0.1.1
 */
public class Color4 extends Vector4 {
	/**
	 * Create a pure black color.
	 */
	public Color4() {
		this(0.0f, 0.0f, 0.0f, 1.0f);
	}

	/**
	 * Creates a fully opaque color.
	 */
	public Color4(float r, float g, float b) {
		this(r, g, b, 1.0f);
	}

	/**
	 * Creates a color.
	 */
	public Color4(float r, float g, float b, float a) {
		super();
		this.floatBuffer.put(0, r);
		this.floatBuffer.put(1, g);
		this.floatBuffer.put(2, b);
		this.floatBuffer.put(3, a);
	}

	/**
	 * Gets the red value from 0.0f to 1.0f.
	 */
	public float r() {
		return this.floatBuffer.get(0);
	}

	/**
	 * Gets the green value from 0.0f to 1.0f.
	 */
	public float g() {
		return this.floatBuffer.get(1);
	}

	/**
	 * Gets the blue value from 0.0f to 1.0f.
	 */
	public float b() {
		return this.floatBuffer.get(2);
	}

	/**
	 * Gets the alpha value from 0.0f to 1.0f.
	 */
	public float a() {
		return this.floatBuffer.get(3);
	}
}