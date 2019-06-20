package com.gracefulcode.opengine.core;

/**
 * There are many cases where we need to handle a ternary state. One example is
 * in plugins asking for features. Features start as "unknown", and can be
 * marked as either "yes" (we want this feature) or "no" (we do NOT want this
 * feature). This allows us to detect when something wants both yes and no
 * (which is a configuration conflict).
 *
 * @author Daniel Grace <dgrace@gracefulcode.com>
 * @version 0.1
 */
public enum Ternary {
	YES,
	UNKNOWN,
	NO
}