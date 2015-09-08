/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveIndicatorCommandClass;
import org.openhab.binding.zwave.internal.protocol.ZWaveDeviceClass.Specific;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * This class provides a storage class for z-wave scene devices
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
	private static final int COOPER = 0x001A;

	private ZWaveNode node;
	private boolean isCooperController;
	private boolean isPortable;
	private ZWaveController controller;
	private ArrayList<Integer> controlledScenes;
	private int indicator;

	/**
	 * Constructor with just a Z-Wave controller object
	 * @param ctrl Z-Wave Controller object
	 */
	ZWaveSceneController(ZWaveController ctrl) {
		init(ctrl, null);
	}

	/**
	 * Constructor
	 * @param ctrl Z-Wave Controller object
	 * @param newNode ZWaveNode of the scene controller device
	 */
	ZWaveSceneController(ZWaveController ctrl, ZWaveNode newNode) {
		init(ctrl, newNode);
	}
	
	/**
	 * Constructor
	 * @param ctrl Z-Wave Controller object
	 * @param nodeId int node ID of scene controller device
	 */
	ZWaveSceneController(ZWaveController ctrl, int nodeId) {
		init(ctrl, ctrl.getNode(nodeId));
	}
	
	/**
	 * Initialize object
	 * @param ctrl Z-Wave Controller object
	 * @param newNode ZWaveNode object
	 */
	private void init(ZWaveController ctrl, ZWaveNode newNode) {
		controller = ctrl;
		isCooperController = false;
		isPortable = false;
		indicator = 0;
		setNode(newNode);
		controlledScenes = new ArrayList<Integer>();	
	}
	
	/**
	 * Set a scene controller button to ON 
	 * @param buttonId ID of the button that will be tuned on
	 */
	public void setButtonOn(int buttonId) {
		indicator = indicator ^ ( 0x0001 << buttonId);
	}
	
	/**
	 * Set a scene controller button to OFF
	 * @param buttonId ID of the button that will be tuned on
	 */
	public void setButtonOff(int buttonId) {
		indicator = indicator & ~(0x0001 << buttonId);
	}
	
	/**
	 * check if button is on of off
	 * @param buttonId
	 * @return true if button is on, false if it is off
	 */
	public boolean isButtonOn(int buttonId) {
		int b = indicator & (0x0001 << buttonId); 
		if (b != 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * Get the Indicator state for this controller
	 * @return int 
	 */
	public int getIndicator() {
		return indicator;
	}
	
	/** 
	 * Set the indicator status for this controller
	 * @param newIndicator
	 * @return int previous indicator status 
	 */
	public int setIndicator(int newIndicator) {
		int previousIndicator = indicator;
		indicator = newIndicator;
		return previousIndicator;
	}
	
	public void setNodeIndicator() {
		ZWaveIndicatorCommandClass indicatorCmdClass = (ZWaveIndicatorCommandClass) node.getCommandClass(ZWaveCommandClass.CommandClass.INDICATOR);
		SerialMessage serialMessage = indicatorCmdClass.setValueMessage(indicator);
		controller.sendData(serialMessage);
	}
	
	public void getNodeIndicator() {
		ZWaveIndicatorCommandClass indicatorCmdClass = (ZWaveIndicatorCommandClass) node.getCommandClass(ZWaveCommandClass.CommandClass.INDICATOR);
		indicatorCmdClass.getValueMessage(indicator);
		// TODO: add event listner for INDICATOR_REPORT
	}
	
	/**
	 * Set a Z-Wave node as a Scene Controller
	 * @param n ZWaveNode 
	 */
	public void setNode(ZWaveNode n) {
		
		if (n==null) {
			logger.debug("Node cannot be null. Ignoring.", n.getNodeId());
			return;
		}
		
		// Check if this node is a Scene Controller
		ZWaveDeviceClass deviceClass = n.getDeviceClass();
		if (deviceClass.getSpecificDeviceClass() == ZWaveDeviceClass.Specific.PORTABLE_SCENE_CONTROLLER ||
			deviceClass.getSpecificDeviceClass() == ZWaveDeviceClass.Specific.SCENE_CONTROLLER) {
			
			if (deviceClass.getSpecificDeviceClass() == ZWaveDeviceClass.Specific.PORTABLE_SCENE_CONTROLLER) {
				isPortable = true;
			}
			
			if(n.getManufacturer() == COOPER) {
				isCooperController = true;
			}
			
		}
		else {
			logger.debug("Node {} is not a Scene Controller. Ignoring.", n.getNodeId());
			return;
		}
		
		// Check if node supports scene activation
		ZWaveCommandClass c = n.getCommandClass(ZWaveCommandClass.CommandClass.SCENE_CONTROLLER_CONF);
		if (c != null) {
			logger.error("NODE {} : This device does not support scene controller configuration command class and cannot be configured for scenes.", n.getNodeId());
			return;
		}
		
		logger.debug("Scene Controller Set Node {}", n.getNodeId());
		node = n;
	}
	
	/**
	 * Set a Z-Wave node as a scene controller using node ID
	 * @param nodeId int ID of the node
	 */
	public void setNode(int nodeId) {
		setNode(controller.getNode(nodeId));
	}

	/**
	 * Get the scene controller node 
	 * @return node
	 */
	public ZWaveNode getNode() {
		return node;
	}

	/**
	 * Is the device a Copper Scene Controller
	 * @return boolean true if device is a cooper scene controller
	 */
	public boolean isCooperController() {
		return isCooperController;
	}
	
	/**
	 * Is the device a portable scene controller or static
	 * @return boolean true if device is a protable scene controller
	 */
	public boolean isPortableController() {
		return isPortable;
	}
}