package com.gracefulcode.opengine;

public interface WindowCreator<W extends Window> {
	public W createWindow(Window.Configuration configuration);
}