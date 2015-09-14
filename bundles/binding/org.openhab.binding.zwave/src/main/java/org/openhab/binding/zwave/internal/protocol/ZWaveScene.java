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

	private String sceneName;
	private int sceneId;
	private ZWaveController controller;

	// Map nodes to scene controlled devices Hash<nodeId, sceneDevice>
	private HashMap<Integer, ZWaveSceneDevice> devices;
	
	// map nodes to scene controller devices Hash<nodeId, sceneController>
	private HashMap<Integer, ZWaveSceneController> sceneControllers;
	
	// Map nodes to node buttons Hash<nodeId, buttonID>
	private HashMap<Integer, Integer> sceneControllerButtons;

	ZWaveScene(ZWaveController zController) {
		init(zController, 0, "");
	}
	
	ZWaveScene(ZWaveController zController, int sId) {
		init(zController, sId, "");
	}
	
	ZWaveScene(ZWaveController zController, int sId, String name) {
		init(zController, sId, name);
	}
	
	private void init(ZWaveController zController, int sId, String name) {
		controller = zController;
		sceneName = name;
		sceneId = sId;
		devices = new HashMap<Integer, ZWaveSceneDevice>();
		sceneControllers = new HashMap<Integer, ZWaveSceneController>();
		sceneControllerButtons = new HashMap<Integer, Integer>();
	}

	public int getId() {
		return sceneId;
	}

	public String getName() {
		return sceneName;
	}

	public void setId(int newId) {
		sceneId = newId;
	}

	public void setName(String newName) {
		sceneName = newName;
	}
	
	public void putSceneController(ZWaveSceneController sc, int button) {
		ZWaveNode node = sc.getNode();
		if (node != null) {
			sceneControllers.put(node.getNodeId(), sc);
			if (sc.isButtonIdValid(button)) {
				logger.info("Node {} button {} bound to scene {}", node.getNodeId(), button, sceneId);
				sceneControllerButtons.put(node.getNodeId(), button);
			}
			else {
				logger.warn("Node {} attempt to bind an invalid button to scene controller.", node.getNodeId());
			}
			
		}
		else {
			logger.warn("Scene Conotroller object does not have valid node information. Cannot add it to scene");
		}
	}
		
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
	
	public void removeSceneController(int nodeId) {
		if (sceneControllers.containsKey(nodeId)) {
			sceneControllers.remove(nodeId);
			sceneControllerButtons.remove(nodeId);
		}
		else {
			logger.warn("Scene Conotroller object does not have valid node information. Cannot remove it to scene");
		}
	}
	
	public HashMap<Integer, ZWaveSceneController> getSceneControllers() {
		return sceneControllers;
	}
	
	public ZWaveSceneController getSceneController(int nodeId) {
		return sceneControllers.get(nodeId);
	}
	
	public void activate() {
		// Get all the controllers associated with scene
 		if (!sceneControllers.isEmpty()) {
 			logger.info("Scene {} is being activated", sceneId);
 			
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
	 * @return HashMap<Integer, ArrayList<ZWaveSceneDevice>> H<value, ZWaveSceneDeviceList>
	 */
	public HashMap<Integer, ArrayList<ZWaveSceneDevice>> groupDevicesByLevels(int sceneId) {
		// Init groups
		HashMap<Integer, ArrayList<ZWaveSceneDevice>>  groups = new HashMap<Integer, ArrayList<ZWaveSceneDevice>>();

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

			ArrayList<ZWaveSceneDevice> deviceList = new ArrayList<ZWaveSceneDevice>();

			if (groups.containsKey(value)) {
				deviceList = groups.get(value);
			}

			deviceList.add(d);
			groups.put((Integer) value, deviceList);
		}

		return groups;
	}
	
	/**
	 * Add a new ZWave device to a Z-Wave Scene
	 * @param nodeId id of the node being added to the scene
	 * @param d ZWaveSceneDevice with the values and node information to 
	 * 		  set when scene is activated
	 */
	public void addDevice(ZWaveSceneDevice d) {
		devices.put(d.getNodeId(), d);
	}

	/**
	 * Add a new ZWave device to a Z-Wave Scene
	 * @param nodeId id of the node being added to the scene
	 * @param value of the node that should be set when scene is activated
	 */
	public void addDevice(int nodeId, byte value) {
		ZWaveSceneDevice d = new ZWaveSceneDevice();

		d.setNode(controller.getNode(nodeId));
		d.setValue(value);
		devices.put(nodeId, d);
	}

	public ZWaveSceneDevice getDevice(int nodeId) {
		return devices.get(nodeId);
	}

	public void removeDevice(int nodeId) {
		devices.remove(nodeId);
	}

	public HashMap<Integer, ZWaveSceneDevice> getDevices() {
		return devices;
	}

	public void addValue(int nodeId, byte value) {
		ZWaveSceneDevice d = new ZWaveSceneDevice();
		
		if (devices.containsKey(nodeId)) {
			d = devices.get(nodeId);
			d.setValue(value);
			devices.put(nodeId, d);
		}
	}
}
