package com.gracefulcode;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.gracefulcode.opengine.WindowManager;

import org.junit.Test;


public class WindowManagerTest {
	@Test
	public void testDefaultConfiguration() {
		WindowManager wm = mock(WindowManager.class);
		verify(wm, times(1)).createWindow(wm.configuration.defaultWindowConfiguration);
	}

	/*
    @Test
    public void testAppHasAGreeting() {
        App classUnderTest = new App();
        assertNotNull("app should have a greeting", classUnderTest.getGreeting());
    }
    */
}
