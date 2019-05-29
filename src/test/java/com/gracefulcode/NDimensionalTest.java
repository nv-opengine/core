package com.gracefulcode;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.gracefulcode.opengine.v2.NDimensional;

import org.junit.Test;

public class NDimensionalTest {
	@Test
	public void testSame() {
		NDimensional a = new NDimensional(2);
		NDimensional b = new NDimensional(2);

		a.set(0, 0.0f);
		a.set(1, 0.0f);

		b.set(0, 0.0f);
		b.set(1, 0.0f);

		assertEquals(0.0f, a.dst2(b), 0.001f);
		assertEquals(0.0f, b.dst2(a), 0.001f);
	}

	@Test
	public void testVeryClose() {
		NDimensional a = new NDimensional(2);
		NDimensional b = new NDimensional(2);

		a.set(0, 0.0f);
		a.set(1, 0.1f);

		b.set(0, 0.0f);
		b.set(1, 0.11f);

		assertEquals(0.0f, a.dst2(b), 0.001f);
		assertEquals(0.0f, b.dst2(a), 0.001f);
	}

	@Test
	public void testOne() {
		NDimensional a = new NDimensional(2);
		NDimensional b = new NDimensional(2);

		a.set(0, 0.0f);
		a.set(1, 1.0f);

		b.set(0, 0.0f);
		b.set(1, 0.0f);

		assertEquals(1.0f, a.dst2(b), 0.001f);
		assertEquals(1.0f, b.dst2(a), 0.001f);
	}

	@Test
	public void testNormalizeAlreadyNormalized() {
		NDimensional a = new NDimensional(3);
		a.set(0, 0.0f);
		a.set(1, 0.0f);
		a.set(2, 1.0f);
		a.normalize();

		assertEquals(0.0f, a.get(0), 0.001f);
		assertEquals(0.0f, a.get(1), 0.001f);
		assertEquals(1.0f, a.get(2), 0.001f);
	}

	@Test
	public void testNormalizeSingleDimension() {
		NDimensional a = new NDimensional(3);
		a.set(0, 0.0f);
		a.set(1, 0.0f);
		a.set(2, 10.0f);
		a.normalize();

		assertEquals(0.0f, a.get(0), 0.001f);
		assertEquals(0.0f, a.get(1), 0.001f);
		assertEquals(1.0f, a.get(2), 0.001f);
	}

	@Test
	public void testTwoDimension() {
		NDimensional a = new NDimensional(3);
		a.set(0, 0.0f);
		a.set(1, 10.0f);
		a.set(2, 10.0f);
		a.normalize();

		assertEquals(0.0f, a.get(0), 0.001f);
		assertEquals(0.70710677f, a.get(1), 0.001f);
		assertEquals(0.70710677f, a.get(2), 0.001f);
	}
}
