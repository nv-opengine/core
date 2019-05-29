package com.gracefulcode.opengine.v2;

public class NDimensional {
	protected float[] floats;

	public NDimensional(int dimensions) {
		this.floats = new float[dimensions];
	}

	public void set(int index, float value) {
		this.floats[index] = value;
	}

	public float dst2(NDimensional other) {
		if (other.floats.length != this.floats.length) throw new AssertionError("Cannot get the distance of two NDimensionals with different number of dimensions.");

		float d2 = 0.0f;
		for (int i = 0; i < this.floats.length; i++) {
			float tmp = this.floats[i] - other.floats[i];
			d2 += (tmp * tmp);
		}

		return d2;
	}
}