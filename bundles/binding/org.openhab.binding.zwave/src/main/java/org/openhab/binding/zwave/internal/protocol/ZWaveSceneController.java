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


@XStreamAlias("zwaveSceneController")
public class ZWaveSceneController {
	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneController.class);
	
	private ZWaveNode node;
	private int numberOfGroups;
	private boolean isCooperController;
	
	ZWaveSceneController() {
		node = null;
		numberOfGroups = 0;
		isCooperController = false;
	}
	
	ZWaveSceneController(ZWaveNode newNode) {
		setNode(newNode);
	}
	
	public void setNode(ZWaveNode n) {
		logger.debug("Set Node {}", n.getNodeId());
		node = n;
		
		// Check if node supports scene activation
		ZWaveCommandClass c = node.getCommandClass(ZWaveCommandClass.CommandClass.SCENE_CONTROLLER_CONF);
		if (c != null) {
			logger.error("NODE {} : This device does not support scene controller configuration command class and cannot be configured for scenes.", node.getNodeId());
			return;
		}
	}
	
	public ZWaveNode getNode() {
		return node;
	}
	
	public boolean isCooper() {
		return isCooperController;
	}
	
}