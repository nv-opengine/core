package com.gracefulcode.opengine.v2.vulkan.plugins.interfaces;

public interface NeedsQueues {
	public int numGraphicsQueues();
	public int numPresentationQueues();
	public int numComputeQueues();
	public boolean computeGraphicsMustMatch();
	public boolean graphicsPresentationMustMatch();
}