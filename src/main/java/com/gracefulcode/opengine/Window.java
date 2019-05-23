package com.gracefulcode.opengine;

import org.lwjgl.glfw.GLFWKeyCallback;

public interface Window {
	public static class Configuration {
		public String title = "";
		public int width = 800;
		public int height = 600;
		public GLFWKeyCallback keyCallback = null;
		// Clear color. Most efficient if all of {r, g, b} 0 or all 255.
		public int clearR = 0;
		public int clearG = 0;
		public int clearB = 0;
		public int clearA = 255;
	}

	public long getWindowId();
	public boolean shouldClose();
	public void close();
}