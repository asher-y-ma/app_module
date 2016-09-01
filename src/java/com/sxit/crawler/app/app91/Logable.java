/**
 * @(#)Logable.java 2013-5-13
 *
 * Copyright 2013 MINDCENTER Inc. All rights reserved.
 * MINDCENTER PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sxit.crawler.app.app91;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <tt>Logable</tt> provides Logging capability with the target object. the
 * inherited class should extends <tt>Logable</tt> that it would have a logging
 * business.
 * 
 * @see {@link org.slf4j.Logger}
 * @author mclaren
 * 
 */
public abstract class Logable {
	/**
	 * Logging provider
	 * 
	 * @see {@link org.slf4j.Logger}
	 */
	protected Logger log = LoggerFactory.getLogger(getClass());
}
