package com.gracefulcode.opengine.v2;

import java.lang.Math;
import java.util.Arrays;

public class NDimensional {
	protected float[] floats;

	public NDimensional(int dimensions) {
		this.floats = new float[dimensions];
	}

	public NDimensional cpy() {
		NDimensional tmp = new NDimensional(this.floats.length);
		for (int i = 0; i < this.floats.length; i++) {
			tmp.set(i, this.floats[i]);
		}
		return tmp;
	}

	public void set(int index, float value) {
		this.floats[index] = value;
	}

	public void mul(float m) {
		for (int i = 0; i < this.floats.length; i++) {
			this.floats[i] *= m;
		}
	}

	public void add(NDimensional other) {
		if (other.floats.length != this.floats.length) throw new AssertionError("Cannot add two NDimensionals with different number of dimensions.");		
		for (int i = 0; i < this.floats.length; i++) {
			this.floats[i] += other.floats[i];
		}
	}

	public float get(int index) {
		return this.floats[index];
	}

	public float length() {
		float d2 = 0.0f;
		for (int i = 0; i < this.floats.length; i++) {
			float tmp = this.floats[i];
			d2 += (tmp * tmp);
		}
		return (float)Math.sqrt(d2);
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

	public void normalize() {
		float l = this.length();
		if (l == 0.0f) return;
		
		for (int i = 0; i < this.floats.length; i++) {
			this.floats[i] = this.floats[i] / l;
		}
	}

	public String toString() {
		return Arrays.toString(this.floats);
	}
}