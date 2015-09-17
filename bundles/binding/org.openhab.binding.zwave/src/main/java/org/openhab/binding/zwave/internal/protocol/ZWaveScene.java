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
import java.util.HashMap;

import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveAssociationCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveConfigurationCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveSceneActivationCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveSceneActuatorConfCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveSceneControllerConfCommandClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * This class provides a storage class for Z-Wave scenes
 * within the node class. This is then serialized to XML.
 *
 * @author Pedro Paixao
 * @since 1.8.0
 *
 */

@XStreamAlias("zwaveScene")
public class ZWaveScene {
	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveScene.class);
	private static final byte DEFAULT_DIMMING_DURATION = 0x03;
	private static final byte DEFAULT_OVERRIDE = 0x01;
	
	private String sceneName;
	private int sceneId;
	private ZWaveController controller;

	// Map nodes to scene controlled devices Hash<nodeId, sceneDevice>
	private HashMap<Integer, ZWaveSceneDevice> devices;
	
	// map nodes to scene controller devices Hash<nodeId, sceneController>
	private HashMap<Integer, ZWaveSceneController> sceneControllers;
	
	// Map nodes to node buttons Hash<nodeId, buttonID>
	private HashMap<Integer, Integer> sceneControllerButtons;

	private byte dimmingDuration;
	
	// 0 - take the value on the device as the node level, 1 set the device to the controller provided level
	private byte override;
	
	ZWaveScene(ZWaveController zController) {
		init(zController, 0, "", DEFAULT_DIMMING_DURATION);
	}
	
	ZWaveScene(ZWaveController zController, int sId) {
		init(zController, sId, "", DEFAULT_DIMMING_DURATION);
	}
	
	ZWaveScene(ZWaveController zController, int sId, String name) {
		init(zController, sId, name, DEFAULT_DIMMING_DURATION);
	}
	
	ZWaveScene(ZWaveController zController, int sId, String name, byte duration) {
		init(zController, sId, name, DEFAULT_DIMMING_DURATION);
	}
	
	private void init(ZWaveController zController, int sId, String name, byte duration) {
		controller = zController;
		sceneName = name;
		sceneId = sId;
		dimmingDuration = duration;
		override = DEFAULT_OVERRIDE; 
		
		devices = new HashMap<Integer, ZWaveSceneDevice>();
		sceneControllers = new HashMap<Integer, ZWaveSceneController>();
		sceneControllerButtons = new HashMap<Integer, Integer>();
	}
	
	/**
	 * Checks is the specified button of a scene controller can control this scene
	 * @param nodeId scene controller ID
	 * @param groupId button ID
	 * @return true if button on scene controller can control this scene
	 */
	public boolean isSceneContollerBoundToScene(int nodeId, int groupId) {
		if (sceneControllers.containsKey(nodeId) &&
			sceneControllerButtons.get(nodeId) == groupId) {
			return true;
		}
		return false;
	}

	/**
	 * Get scene ID
	 * @return scene ID
	 */
	public int getId() {
		return sceneId;
	}

	/** 
	 * Get scene name
	 * @return name
	 */
	public String getName() {
		return sceneName;
	}
	
	/**
	 * Set scene dimming duration
	 * @param duration
	 * 	  0x00 - Instant
	 *    0x01-0x7F - duration in seconds
	 *    0x80-0xFE - duration in minutes
	 *    0xFF - Factory default
	 */
	public void setDimmingDuration(byte duration) {
		dimmingDuration = duration;
	}
	
	/**
	 * Get the dimming duration of the scene
	 * @return dimming duration
	 *    0x00 - Instant
	 *    0x01-0x7F - duration in seconds
	 *    0x80-0xFE - duration in minutes
	 *    0xFF - Factory default
	 */
	public byte getDimmingDuration() {
		return dimmingDuration;
	}
	
	/**
	 * Set the override value for the scene. When override is "OFF" or 0 a scene is programmed
	 * using the current device's value as the scene stored value. If override is "ON", or 1, 
	 * the current device value is ignored and the value provided by the controller is used
	 * to configure the scene.
	 * 
	 * @param newOverride
	 */
	public void setOverride(byte newOverride) {
		override = newOverride;
	}
	
	/**
	 * Get the scene override value
	 * @return 0 or 1 
	 */
	public byte getOverride() {
		return override;
	}

	/**
	 * Set Scene ID. All Z-Wave scene devices must support 255 scenes 
	 * @param newId
	 */
	public void setId(int newId) {
		sceneId = newId;
	}

	/**
	 * Set scene name
	 * @param newName
	 */
	public void setName(String newName) {
		sceneName = newName;
	}
	
	/**
	 * Add a new scene controller to the scene and bind a group on the device to activate it.
	 * Groups are usually associated with physical buttons on the device. Please check the 
	 * device manual
	 * 
	 * @param ZWaveSceneController
	 * @param group 
	 */
	public void putSceneController(ZWaveSceneController sc, int group) {
		ZWaveNode node = sc.getNode();
		if (node != null) {
			if (sc.isButtonIdValid(group)) {
				sceneControllers.put(node.getNodeId(), sc);
				logger.info("Node {} group {} bound to scene {}", node.getNodeId(), group, sceneId);
				sceneControllerButtons.put(node.getNodeId(), group);
			}
			else {
				logger.warn("Node {} attempt to bind an invalid group to scene controller.", node.getNodeId());
			}
		}
		else {
			logger.warn("Scene Conotroller object does not have valid node information. Cannot add it to scene");
		}
	}
	
	
	public void syncSceneControllers(int nodeId, boolean on) {
		if(sceneControllerButtons.size() <= 1) {
			// nothing to sync if it's only one controller in this scene
			return;
		}
		
		for(Integer controllerId : sceneControllerButtons.keySet()) {
			
			if (controllerId == nodeId) {
				continue;
			}
			
			int buttonId = sceneControllerButtons.get(controllerId);
			ZWaveSceneController sc = sceneControllers.get(controllerId);
			if (on) {
				logger.info("NODE {} Scene Controller Sync Set Button {} to ON for Scene {}", nodeId, buttonId, sceneId);
				sc.setButtonOn(buttonId);
			}
			else {
				logger.info("NODE {} Scene Controller Sync Set Button {} to OFF for Scene {}", nodeId, buttonId, sceneId);
				sc.setButtonOff(buttonId);
			}
			sceneControllers.put(controllerId, sc);
		}
	}
	
	/**
	 * Add a new scene controller to the scene and bind a group on the device to activate it.
	 * Groups are usually associated with physical buttons on the device. Please check the 
	 * device manual
	 * 
	 * @param node ID of the scene controller
	 * @param group 
	 */
	public void putSceneController(int nodeId, int group) {
		ZWaveNode node = controller.getNode(nodeId);
		if (node != null) {
			ZWaveSceneController sc = new ZWaveSceneController(controller, node);
			putSceneController(sc, group);
		}
	}
	
	/**
	 * Remove a scene controller from scene
	 * @param scene controller
	 */
	public void removeSceneController(ZWaveSceneController sc) {
		ZWaveNode node = sc.getNode();
		if (node != null) {
			sceneControllers.remove(node.getNodeId());
			sceneControllerButtons.remove(node.getNodeId());
		}
		else {
			logger.warn("Scene Conotroller object does not have valid node information. Cannot remove it to scene");
		}
	}
	
	/**
	 * Remove an existing scene controller, i.e., this controller can no longer
	 * trigger this scene.
	 * @param nodeId
	 */
	public void removeSceneController(int nodeId) {
		if (sceneControllers.containsKey(nodeId)) {
			sceneControllers.remove(nodeId);
			sceneControllerButtons.remove(nodeId);
		}
		else {
			logger.warn("Scene Conotroller object does not have valid node information. Cannot remove it to scene");
		}
	}
	
	/**
	 * Get list of all scene controllers bound to scene
	 * @return hash list of controllers
	 */
	public HashMap<Integer, ZWaveSceneController> getSceneControllers() {
		return sceneControllers;
	}
	
	/**
	 * Get Scene Controller based on node ID
	 * @param nodeId
	 * @return scene controller
	 */
	public ZWaveSceneController getSceneController(int nodeId) {
		return sceneControllers.get(nodeId);
	}
	
	/**
	 * Activate scene and sync scene controller indicators if the INDICATOR Command Class
	 * is supported
	 */
	public void activate() {
		// Get all the controllers associated with scene
 		if (!sceneControllers.isEmpty()) {
 			logger.info("Scene {} activate", sceneId);
 			
 			// Iterate all nodes that can control this scene and update their button status to ON.
 			for (int nodeId : sceneControllerButtons.keySet()) {
 				int buttonId = sceneControllerButtons.get(nodeId);
 				
 				ZWaveSceneController sc = sceneControllers.get(nodeId);
 				sc.setButtonOn(buttonId);
 				sceneControllers.put(nodeId, sc);
 			} 			
 		}
 		else {
 			logger.info("Scene {} has no scene controllers bound to it.", sceneId);
 		}
	}
	
	/**
	 * Create groups of devices that do not support the  scene activation command class by value.
	 * @param sceneId
	 * @return H<value, NodeIdList>
	 */
	public HashMap<Integer, ArrayList<Integer>> groupDevicesByLevels() {
		// Init groups
		HashMap<Integer, ArrayList<Integer>>  groups = new HashMap<Integer, ArrayList<Integer>>();

		// Iterate all devices
		for( ZWaveSceneDevice d : devices.values()) {

			// Get device value
			int value = d.getValue();

			// Does device support scene activation command class?
			if (d.isSceneSupported()) {
				// Scene activation command class supported no need to group
				// Scene configuration for these devices is done directly with
				// Scene Actuator Configuration command class.
				continue;
			}

			ArrayList<Integer> deviceList;

			if (groups.containsKey(value)) {
				// There's at least one device in the list already
				deviceList = groups.get(value);
			}
			else {
				// Add a new device list
				deviceList = new ArrayList<Integer>();
			}

			deviceList.add(d.getNodeId());
			groups.put((Integer) value, deviceList);
		}

		return groups;
	}
	
	/**
	 * Get a list of nodes that support the SCENE ACTIVATION command class
	 * @return list of nodes
	 */
	public ArrayList<Integer> getNodesSupportingSceneActivation() {
		
		ArrayList<Integer> sceneCapableDevices = new ArrayList<Integer>();
		
		for(ZWaveSceneDevice device : devices.values()) {
			ZWaveNode node = device.getNode();
			if (node == null) {
				logger.error("Scene {} has a device without node information", sceneId);
				return null;
			}
			ZWaveSceneActivationCommandClass sceneActivationCC = (ZWaveSceneActivationCommandClass) node.getCommandClass(CommandClass.SCENE_ACTIVATION);
			if (sceneActivationCC != null) {
				sceneCapableDevices.add(node.getNodeId());
			}
		}
		
		return sceneCapableDevices;
	}
		
	private byte[] toByteArray(ArrayList<Integer> a) {
		byte[] out = new byte[a.size()];
		for(int i=0; i< a.size(); i++){
			out[i] = a.get(i).byteValue();
		}
		return out;
	}
	
	public void resetSceneControllerAssociations() {
		// Iterate all scene controllers bound to this scene
		for(ZWaveSceneController sceneController : sceneControllers.values()) {
			
			ZWaveNode node = sceneController.getNode();
			if (node == null) {
				logger.error("Scene Controller does not have Z-Wave node information. Set node first!");
				return;
			}
			
			// What device button, AKA group is going to be programmed.
			int groupId = sceneControllerButtons.get(node.getNodeId());
			
			// remove all nodes from association group 
			ZWaveAssociationCommandClass associationCmdClass = (ZWaveAssociationCommandClass) node.getCommandClass(CommandClass.ASSOCIATION);
			SerialMessage message = associationCmdClass.removeAllAssociatedNodesMessage(groupId, node.getNodeId());
			controller.sendData(message);
		}
	}
	
	/**
	 * Program scene into Scene controllers and scene nodes
	 */
	public void programSceneControllersWithNonSceneCapableDevices() {
		
		HashMap<Integer, ArrayList<Integer>> basicDevices = groupDevicesByLevels();
		
		// Iterate all scene controllers bound to this scene
		for(ZWaveSceneController sceneController : sceneControllers.values()) {
			
			ZWaveNode node = sceneController.getNode();
			if (node == null) {
				logger.error("Scene Controller does not have Z-Wave node information. Set node first!");
				return;
			}
			
			// What device button, AKA group is going to be programmed.
			int groupId = sceneControllerButtons.get(node.getNodeId());
			 
			ZWaveAssociationCommandClass associationCmdClass = (ZWaveAssociationCommandClass) node.getCommandClass(CommandClass.ASSOCIATION);
			
			ZWaveConfigurationCommandClass configurationCmdClass = (ZWaveConfigurationCommandClass) node.getCommandClass(CommandClass.CONFIGURATION);
			
			// First program all scene devices that do not support SCENE ACTIVATION Command Class
			if(!basicDevices.isEmpty()) {
				for(Integer value : basicDevices.keySet()) {
					logger.info("NODE {} Program scene {} associations for group {}. Adding non scene capable nodes: {}", node.getNodeId(), sceneId, groupId, basicDevices.get(value).toString());
					
					// Get nodes
					byte[] nodes = toByteArray(basicDevices.get(value));
					
					// Set associations
					SerialMessage msg = associationCmdClass.setAssociationMessage(groupId, nodes);
					controller.sendData(msg);
					
					// Configure the level to send to these nodes
					ConfigurationParameter parameter = new ConfigurationParameter(groupId, value, 1);
					msg = configurationCmdClass.setConfigMessage(parameter);
					controller.sendData(msg);
					logger.info("NODE {} Configuration for group {} with value {} sent", node.getNodeId(), groupId, value);
				}
			}
		}
	}
	
	public void programSceneControllersWithSceneCapableDevices() {
		
		// Iterate all scene controllers bound to this scene
		for(ZWaveSceneController sceneController : sceneControllers.values()) {
			
			ZWaveNode node = sceneController.getNode();
			if (node == null) {
				logger.error("Scene Controller does not have Z-Wave node information. Set node first!");
				return;
			}
			
			// What device button, AKA group is going to be programmed.
			int groupId = sceneControllerButtons.get(node.getNodeId());
			
			ZWaveSceneControllerConfCommandClass sceneControllerCmdClass = (ZWaveSceneControllerConfCommandClass) node.getCommandClass(CommandClass.SCENE_CONTROLLER_CONF);
			logger.info("NODE {} Program controller with scene {} group {} dimming duration: {}", node.getNodeId(), sceneId, groupId, dimDurationToString());
			SerialMessage msg = sceneControllerCmdClass.setValueMessage((byte)groupId, (byte)sceneId, dimmingDuration);
			controller.sendData(msg);
			
			// Set associations
			ZWaveAssociationCommandClass associationCmdClass = (ZWaveAssociationCommandClass) node.getCommandClass(CommandClass.ASSOCIATION);
			ArrayList<Integer> sceneNodes = getNodesSupportingSceneActivation();
			if (sceneNodes == null) {
				logger.info("NODE {} Program scene {} has no nodes! Stopping scene programming.", node.getNodeId(), sceneId);
				return;
			}
			
			logger.info("NODE {} Program scene {} associations for group {}. Adding scene capable nodes: {}", node.getNodeId(), sceneId, groupId, sceneNodes.toString());
			
			// Add the Z-Wave controller to the association group so it can receive messages from device 
			sceneNodes.add(1);
			
			byte[] nodes = toByteArray(sceneNodes);
			msg = associationCmdClass.setAssociationMessage(groupId, nodes);
			controller.sendData(msg);
		}
	}
	
	/**
	 * Configure Scene in all Scene Capable actuators, or nodes.
	 */
	public void programSceneCapableNodes() {
		// Get all scene supporting nodes 
		ArrayList<Integer> nodes = getNodesSupportingSceneActivation();
		if (nodes == null) {
			logger.info("Scene {} has no scene capable devices.", sceneId);
			return;
		}
		
		for(Integer nodeId : nodes) {
			ZWaveSceneDevice device = devices.get(nodeId);
			if (device == null) {
				logger.error("NODE {} is not associated with a Scene Device.", nodeId);
				return;
			}
			
			ZWaveNode node = device.getNode();
			if( node == null) {
				logger.error("Programming scene capable devices. Device has no node information");
				continue;
			}
			
			ZWaveSceneActuatorConfCommandClass sceneActuatorCmdClass = (ZWaveSceneActuatorConfCommandClass) node.getCommandClass(CommandClass.SCENE_ACTUATOR_CONF);
			logger.info("NODE {} Program scene {} level {} dimming duration: {}", node.getNodeId(), sceneId, device.getValue(), dimDurationToString());
			
			SerialMessage msg = sceneActuatorCmdClass.setValueMessage((byte) sceneId, device.getValue(), dimmingDuration, override == 1);
			controller.sendData(msg);
		}
	}
	
	/**
	 * Program scenes into Z-Wave nodes
	 */
	public void program() {
		resetSceneControllerAssociations();
		programSceneControllersWithNonSceneCapableDevices();
		programSceneControllersWithSceneCapableDevices();
		programSceneCapableNodes();
	}
	
	/**
	 * Convert the dimming duration to a string
	 * @return the dimming time
	 */
	public String dimDurationToString() {
		if( dimmingDuration == 0) {
			return "Instant";
		}
		if( dimmingDuration < 0x80 ) {
			return dimmingDuration + " seconds";
		}
		if( dimmingDuration < 0xFF) {
			return (dimmingDuration - 0x80) + " minutes";
		}
		return "factory default duration";
	}
	
	/**
	 * Add a new ZWave device to a Z-Wave Scene
	 * @param nodeId id of the node being added to the scene
	 * @param d ZWaveSceneDevice with the values and node information to 
	 * 		  set when scene is activated
	 */
	public void addDevice(ZWaveSceneDevice d) {
		if (d != null) {
			devices.put(d.getNodeId(), d);
		}
	}

	/**
	 * Add a new ZWave device to a Z-Wave Scene
	 * @param nodeId id of the node being added to the scene
	 * @param value of the node that should be set when scene is activated
	 */
	public void addDevice(int nodeId, int value) {

		ZWaveNode node = controller.getNode(nodeId);
		if ( node == null) {
			logger.error("Scene {} Cannot add device with null node object", sceneId);
			return;
		}
		
		ZWaveSceneDevice d = new ZWaveSceneDevice();
		d.setNode(node);
		d.setValue(value);
		devices.put(nodeId, d);
	}

	/**
	 * Get device from node ID
	 * @param nodeId
	 * @return scene device
	 */
	public ZWaveSceneDevice getDevice(int nodeId) {
		return devices.get(nodeId);
	}

	/**
	 * Remove a device from the scene
	 * @param nodeId
	 */
	public void removeDevice(int nodeId) {
		devices.remove(nodeId);
	}

	/** 
	 * Get all the devices that are part of the scene
	 * @return 
	 *    HashMap<Integer, ZWaveSceneDevice> with the nodeIds of every node in the scene, and the value is a SceneDevice
	 */
	public HashMap<Integer, ZWaveSceneDevice> getDevices() {
		return devices;
	}

	/**
	 * Add a value to an existing node.
	 * @param nodeId
	 * @param value
	 */
	public void addValue(int nodeId, byte value) {
		ZWaveSceneDevice d = new ZWaveSceneDevice();
		
		if (devices.containsKey(nodeId)) {
			d = devices.get(nodeId);
			d.setValue(value);
			devices.put(nodeId, d);
		}
	}
}
