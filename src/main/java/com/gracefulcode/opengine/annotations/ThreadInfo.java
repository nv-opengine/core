package com.gracefulcode.opengine.annotations;

import java.lang.annotation.Documented;

@Documented
public @interface ThreadInfo {
	boolean perThread() default false;
}