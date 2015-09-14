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
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveIndicatorCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;

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
public class ZWaveSceneController implements ZWaveEventListener {
	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneController.class);
	private static final int COOPER = 0x001A;

	private ZWaveNode node;
	private boolean isCooperController;
	private boolean isPortable;
	private ZWaveController controller;
	private ZWaveIndicatorCommandClass indicatorCmdClass;
	private byte indicator;
	private boolean indicatorValid;

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
		
		// Initialize the internal indicator state, and set it as invalid
		// until we get an INDICATOR Command Class Report from the device
		indicator = 0;
		indicatorValid = false;
		
		setNode(newNode);
		
		ZWaveIndicatorCommandClass indicatorCmdClass = (ZWaveIndicatorCommandClass) node.getCommandClass(ZWaveCommandClass.CommandClass.INDICATOR);
		if (indicatorCmdClass != null) {
			controller.addEventListener(this);
			getNodeIndicator();
		}
	}
	
	/**
	 * Set a scene controller button to ON 
	 * @param buttonId ID of the button that will be tuned on
	 */
	public void setButtonOn(int buttonId) {
		// If button is already ON do nothing!
		if (!isButtonOn(buttonId)) {
			indicator = (byte) (indicator ^ ( 0x01 << buttonId));
			setNodeIndicator();
		}
	}
	
	/**
	 * Set a scene controller button to OFF
	 * @param buttonId ID of the button that will be tuned on
	 */
	public void setButtonOff(int buttonId) {
		// If button is already OFF do nothing!
		if (isButtonOn(buttonId)) {
			indicator = (byte) (indicator & ~(0x01 << buttonId));
			setNodeIndicator();
		}
	}
	
	/**
	 * check if button is ON of OFF
	 * @param buttonId
	 * @return true if button is ON, false if it is OFF
	 */
	public boolean isButtonOn(int buttonId) {
		int b = indicator & (0x01 << buttonId); 
		if (b != 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * Get the Indicator state for this controller
	 * @return indicator 
	 */
	public byte getIndicator() {
		return indicator;
	}
	
	/** 
	 * Set the indicator status for this controller
	 * @param newIndicator
	 * @return previous indicator status 
	 */
	public byte setIndicator(byte newIndicator) {
		
		if (indicator == newIndicator) {
			return indicator;
		}
		
		byte previousIndicator = indicator;
		indicator = newIndicator;
		setNodeIndicator();
		return previousIndicator;
	}
	
	/**
	 * Send a Z-Wave INDICATOR Command Class SET message to the node
	 * to set the value of the node's indicator.
	 * This operation is asynchronous, and it will take some time to take effect
	 */
	public void setNodeIndicator() {
		if (indicatorCmdClass != null) {
			
			// Indicator changed so internal value is no longer valid
			indicatorValid = false;
			
			SerialMessage serialMessage = indicatorCmdClass.setValueMessage(indicator);
			controller.sendData(serialMessage);
			
			// Send an INDICATOR GET message to later validate the state. 
			getNodeIndicator();
		}
		else {
			logger.info("NODE {} does not support indicator class, not sending SET INDICATOR message",node.getNodeId());
		}
	}
	
	/**
	 * Send and GET INDICATOR message to the Z-Wave node to get node's indicator status
	 * Getting the Indicator status is asynchronous, a Z-Wave INDICATOR report from the node
	 * needs to be processed in order to get the node's indicator value. 
	 * See {ZWaveIncomingEvent}
	 */
	public void getNodeIndicator() {
		if (indicatorCmdClass != null) {
			SerialMessage serialMessage = indicatorCmdClass.getValueMessage();
			controller.sendData(serialMessage);
		}
		else {
			logger.info("NODE {} does not support indicator class, not sending GET INDICATOR message",node.getNodeId());
		}
	}
	
	/**
	 * Check if the internal indicator state is valid. INDICATOR SET and GET 
	 * operations are asynchronous. So we need to wait until we receive an
	 * INDICATOR REPORT message with the indicator value currently stored
	 * in the real Z-Wave device.
	 * Any operation that changes the indicator value will invalidate the state.
	 * State will remain invalid until a REPORT message is received
	 * 
	 * @return true if the indicator state is valid and can be used
	 */
	public boolean isIndicatorSateValid() {
		return indicatorValid;
	}
	
	/**
	 * Does the node support the INDICATOR Command Class
	 * @return true if INDICATOR is supported
	 */
	public boolean isIndicatorSupported() {
		return (indicatorCmdClass != null);
	}
	
	/**
	 * Set a Z-Wave node as a Scene Controller
	 * @param n ZWaveNode 
	 */
	public void setNode(ZWaveNode n) {
		
		if (n==null) {
			logger.debug("Node cannot be null. Ignoring.");
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
	 * @return boolean true if device is a portable scene controller
	 */
	public boolean isPortableController() {
		return isPortable;
	}
	
	/**
	 * Process Z-Wave events and if they are INDICATOR command class value events
	 * directed to this node extract the indicator value and validate internal state
	 * @param Z-Wave Events from controller
	 */
	@Override
	public void ZWaveIncomingEvent(ZWaveEvent event) {
		
		// Check if we got a Z-Wave Value Event
		if (event instanceof ZWaveCommandClassValueEvent) {
			ZWaveCommandClassValueEvent valueEvent = (ZWaveCommandClassValueEvent) event;
			
			// Is it an INDICATOR Command Class event for this node
			if (valueEvent.getCommandClass() == CommandClass.INDICATOR &&
				valueEvent.getNodeId() == node.getNodeId() ) {
				
				// get the indicator value from the event
				indicator = ((Integer) valueEvent.getValue()).byteValue();
				
				// the indicator state is now valid
				indicatorValid = true;
			}
		}
	}
}