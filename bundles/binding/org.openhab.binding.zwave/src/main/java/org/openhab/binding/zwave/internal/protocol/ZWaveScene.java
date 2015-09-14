/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol;

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

	private HashMap<Integer, ZWaveSceneDevice> devices;
	private HashMap<Integer, ZWaveSceneController> sceneControllers;

	ZWaveScene(ZWaveController zController) {
		controller = zController;
		sceneName = "";
		sceneId = 0;
		devices = new HashMap<Integer, ZWaveSceneDevice>();
		sceneControllers = new HashMap<Integer, ZWaveSceneController>();
	}

	ZWaveScene(ZWaveController zController, int sId) {
		controller = zController;
		sceneName = "";
		sceneId = sId;
		devices = new HashMap<Integer, ZWaveSceneDevice>();
		sceneControllers = new HashMap<Integer, ZWaveSceneController>();
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
	
	public void putSceneController(ZWaveSceneController sc) {
		
		ZWaveNode node = sc.getNode();
		if (node != null) {
			sceneControllers.put(node.getNodeId(), sc);
		}
		else {
			logger.warn("Scene Conotroller object does not have valid node information. Cannot add it to scene");
		}
		
	}
	
	public void removeSceneController(ZWaveSceneController sc) {
		ZWaveNode node = sc.getNode();
		if (node != null) {
			sceneControllers.remove(node.getNodeId());
		}
		else {
			logger.warn("Scene Conotroller object does not have valid node information. Cannot remove it to scene");
		}
	
	}
	
	public ZWaveSceneController getSceneController(int nodeId) {
		return sceneControllers.get(nodeId);
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
