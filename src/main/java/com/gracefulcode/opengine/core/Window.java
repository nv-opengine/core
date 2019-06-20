package com.gracefulcode.opengine.core;

public interface Window {
	public static class Configuration {
		public String title;
		public int width;
		public int height;

		public Configuration(String title, int width, int height) {
			this.title = title;
			this.width = width;
			this.height = height;
		}
	}

	public boolean shouldClose();
	public void render();
	public void resized(int width, int height);
	public void mousePosition(double xpos, double ypos);
}