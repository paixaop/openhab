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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.j3d.utils.scenegraph.io.retained.Controller;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * This class manages all zwave scenes
 * This is then serialized to XML.
 * 
 * @author Pedro Paixao
 * @since 1.8.0
 *
 */

@XStreamAlias("zwaveSceneManager")
public class ZWaveSceneManager {
	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneManager.class);
	
	// Maximum number of scenes supported by ZWave
	private static final int MAX_NUMBER_OF_SCENES = 256;
	
	private HashMap<Integer, ZWaveScene> scenes= new  HashMap<Integer, ZWaveScene>();
	private ZWaveController controller;
	
	ZWaveSceneManager(ZWaveController zController) {
		controller = zController;
		sceneCounter = 0;
		scenes.clear();
	}
	
	/**
	 * Get the number of scenes.
	 * @return int number of registered scenes.
	 */
	public int numberScenes() {
		return scenes.size();
	}
	
	/**
	 * Get the lowest scene ID that is free for usage
	 * @return int scene ID
	 */
	public int getLowestUnusedSceneId() {
		if (scenes.isEmpty()) {
			return 1;
		}
		for(int i = 0; i< scenes.size(); i++) {
			if (!scenes.containsKey(i)) {
				return i;
			}
		}
		if (scenes.size() < MAX_NUMBER_OF_SCENES) {
			return scenes.size() + 1;
		}
		else {
			logger.info("Maximum number of scenes ({})reached. Cannot add new scenes until you delete some.", MAX_NUMBER_OF_SCENES);
			return -1;
		}
	}
	
	/**
	 * Add a new Z-Wave scene to the scene manager 
	 */
	public int newScene() {
		return newScene("");
	}
	
	/**
	 * Add a new Z-Wave scene to the scene manager with a scene name
	 * @param newName String with name of scene 
	 * @return sceneId in case the scene is successful added, 0 otherwise
	 */
	public int newScene(String newName) {
		if (scenes.size() < MAX_NUMBER_OF_SCENES) {
			int sceneId = getLowestUnusedSceneId();
			ZWaveScene zTemp = new ZWaveScene(controller, sceneId);
			zTemp.setName(newName);
			scenes.put(sceneId, zTemp);
			return sceneId;
		}
		else {
			logger.info("Maximum number of scenes ({})reached. Cannot add new scenes until you delete some.", MAX_NUMBER_OF_SCENES);
			return 0;
		}
	}
	
	/**
	 * Delete scene. This sceneId becomes available for new scenes.
	 * @param sceneId
	 */
	public void removeScene(int sceneId) {
		if (!scenes.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return;
		}
		logger.info("Removing scene {}", sceneId);
		scenes.remove(sceneId);
	}
	
	/**
	 * Set a scene's name
	 * @param sceneId id of the scene 
	 * @param newName new name for the scene
	 */
	public void setName(int sceneId, String newName) {
		if (!scenes.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return;
		}
		ZWaveScene zTemp = scenes.get(sceneId);
		zTemp.setName(newName);
		scenes.put(sceneId, zTemp);
	}
	
	public void addDevice(int sceneId, int nodeId, byte value) {
		if (!scenes.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return;
		}
		
		ZWaveScene zTemp = scenes.get(sceneId);
		zTemp.addDevice(nodeId, value);
	}
	
	/**
	 * Add a new ZWave device to a Z-Wave Scene
	 * @param nodeId id of the node being added to the scene
	 * @param d ZWaveSceneDevice with the values and node information to 
	 * 		  set when scene is activated
	 */
	public void addDevice(int sceneId, int nodeId, ZWaveSceneDevice d) {
		if (!scenes.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return;
		}
		ZWaveScene zTemp = scenes.get(sceneId);
		zTemp.addDevice(nodeId, d);
		scenes.put(nodeId, zTemp);
	}
	
	public HashMap<Integer, ZWaveSceneDevice> getDevices(int sceneId) {
		if (!scenes.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return null;
		}
		
		ZWaveScene zTemp = scenes.get(sceneId);
		return zTemp.getDevices();
	}
	
	/** 
	 * Create groups of devices that do not support the  scene activation command class by value. 
	 * @param sceneId
	 * @return HashMap<Integer, ArrayList<ZWaveSceneDevice>> H<value, ZWaveSceneDeviceList>
	 */
	public HashMap<Integer, ArrayList<ZWaveSceneDevice>> groupDevicesByLevels(int sceneId) {
		if (!scenes.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return null;
		}
		
		// Init groups
		HashMap<Integer, ArrayList<ZWaveSceneDevice>>  groups = new HashMap<Integer, ArrayList<ZWaveSceneDevice>>(); 
		
		// Get scene devices
		ZWaveScene zTemp = scenes.get(sceneId);
		HashMap<Integer, ZWaveSceneDevice> devices = zTemp.getDevices();
		
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
	
	
}
