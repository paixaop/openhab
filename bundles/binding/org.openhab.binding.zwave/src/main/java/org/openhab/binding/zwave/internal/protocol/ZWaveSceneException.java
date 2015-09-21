/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol;

/**
 * Exceptions thrown from the serial interface.
 * @author Pedro Paixao
 * @since 1.8.0
 */
public class ZWaveSceneException extends Exception {
	private static final long serialVersionUID = 5517888887773071687L;
	private int sceneId;
	
	public ZWaveSceneException(String message, int sceneId) {
		super(message);
		this.sceneId = sceneId; 
	}

	/**
	 * Constructor. Creates a new instance of ZWaveSceneException.
	 * @param cause the cause. (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public ZWaveSceneException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor. Creates a new instance of ZWaveSceneException.
	 * @param message the detail message.
	 * @param cause the cause. (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
	 */
	public ZWaveSceneException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public int getSceneId() {
		return sceneId;
	}

	public void setSceneId(int sceneId) {
		this.sceneId = sceneId;
	}
	
	public String getMessage() {
		return "Scene " + sceneId + " " + super.getMessage();
	}
}
