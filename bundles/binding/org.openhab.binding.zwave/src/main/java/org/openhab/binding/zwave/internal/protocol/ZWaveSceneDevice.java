/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * This class provides a storage class for zwave scene devices
 * This is then serialized to XML.
 *
 * @author Pedro Paixao
 * @since 1.8.0
 *
 */


@XStreamAlias("zwaveSceneDevice")
public class ZWaveSceneDevice {
	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneDevice.class);

	private ZWaveNode node;
	private byte value;
	private boolean sceneSupport;

	ZWaveSceneDevice() {
		node = null;
		value = 0;
		sceneSupport = false;
	}

	public void setNode(ZWaveNode n) {
		if (n == null) {
			return;
		}
		
		logger.debug("Scene Device Set Node to {}", n.getNodeId());
		node = n;

		// Check if node supports scene activation
		ZWaveCommandClass c = node.getCommandClass(ZWaveCommandClass.CommandClass.SCENE_ACTIVATION);
		sceneSupport = false;
		if (c != null) {
			sceneSupport = true;
		}
	}

	public ZWaveNode getNode() {
		return node;
	}

	/** 
	 * Get the node of the device
	 * @return node ID
	 */
	public int getNodeId() {
		if(node != null) {
			return node.getNodeId();
		}
		return 0;
	}
	
	/**
	 * Check if devices supports scenes 
	 * @return true if device supports scenes, false otherwise.
	 */
	public boolean isSceneSupported() {
		return sceneSupport;
	}

	/**
	 * Set the value/level of the device in this scene
	 * @param v the desired value/level
	 */
	public void setValue(int v) {
		if (node == null) {
			logger.error("Scene Device trying to set a value of a NULL node. Set node first");
			return;
		}

		logger.debug("Scene Device set Node {} value to {}", node.getNodeId(), v);

		value = (byte) v;
	}

	public byte getValue() {
		return value;
	}
}