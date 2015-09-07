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
 * This class manages all zwave sceneManagerStore
 * This is then serialized to XML.
 *
 * @author Pedro Paixao
 * @since 1.8.0
 *
 */

/**
 * Z-Wave Scene Support
 * 
 * Scene Manager manages sceneManagerStore for one or more Z-Wave networks, each identified by its
 * own Home ID.
 * 
 * Each Z-Wave network supports up to 255 sceneManagerStore, each with many devices or Z-Wave nodes.
 * Only nodes in the same network, i.e, same Home ID can participate in a scene.
 *   
 * @author Pedro Paixao
 */

@XStreamAlias("zwaveSceneManager")
public class ZWaveSceneManager {
	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneManager.class);

	// Maximum number of sceneManagerStore supported by ZWave
	private static final int MAX_NUMBER_OF_sceneManagerStore = 256;
	
	// Hash of Z-Wave Home IDs as Keys and sceneManagerStore as Values
	// sceneManagerStore are themselves a hash of scene ID as key and ZWaveScene values
	private ZWaveSceneManagerStore sceneManagerStore = new ZWaveSceneManagerStore();
	
	@XStreamOmitField
	private ZWaveController controller;

	ZWaveSceneManager(ZWaveController zController) {
		controller = zController;
		sceneManagerStore.clear();
	}

	/**
	 * Get the number of sceneManagerStore.
	 * @return int number of registered sceneManagerStore.
	 */
	public int numbersceneManagerStore() {
		return sceneManagerStore.size();
	}

	/**
	 * Get the lowest scene ID that is free for usage
	 * @return int scene ID
	 */
	public int getLowestUnusedSceneId() {
		if (sceneManagerStore.isEmpty()) {
			return 1;
		}
		for(int i = 0; i< sceneManagerStore.size(); i++) {
			if (!sceneManagerStore.containsKey(i)) {
				return i;
			}
		}
		if (sceneManagerStore.size() < MAX_NUMBER_OF_sceneManagerStore) {
			return sceneManagerStore.size() + 1;
		}
		else {
			logger.info("Maximum number of sceneManagerStore ({})reached. Cannot add new sceneManagerStore until you delete some.", MAX_NUMBER_OF_sceneManagerStore);
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
		if (sceneManagerStore.size() < MAX_NUMBER_OF_sceneManagerStore) {
			int sceneId = getLowestUnusedSceneId();
			ZWaveScene zTemp = new ZWaveScene(controller, sceneId);
			zTemp.setName(newName);
			sceneManagerStore.put(sceneId, zTemp);
			return sceneId;
		}
		else {
			logger.info("Maximum number of sceneManagerStore ({})reached. Cannot add new sceneManagerStore until you delete some.", MAX_NUMBER_OF_sceneManagerStore);
			return 0;
		}
	}

	/**
	 * Delete scene. This sceneId becomes available for new sceneManagerStore.
	 * @param sceneId
	 */
	public void removeScene(int sceneId) {
		if (!sceneManagerStore.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return;
		}
		logger.info("Removing scene {}", sceneId);
		sceneManagerStore.remove(sceneId);
	}

	/**
	 * Set a scene's name
	 * @param sceneId id of the scene
	 * @param newName new name for the scene
	 */
	public void setName(int sceneId, String newName) {
		if (!sceneManagerStore.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return;
		}
		ZWaveScene scene = sceneManagerStore.get(sceneId);
		scene.setName(newName);
		sceneManagerStore.put(sceneId, scene);
	}

	public void addDevice(int sceneId, int nodeId, byte value) {
		if (!sceneManagerStore.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return;
		}
		
		ZWaveScene scene = sceneManagerStore.get(sceneId);
		scene.addDevice(nodeId, value);
		sceneManagerStore.put(sceneId, scene);
	}

	/**
	 * Add a new ZWave device to a Z-Wave Scene
	 * @param nodeId id of the node being added to the scene
	 * @param d ZWaveSceneDevice with the values and node information to
	 * 		  set when scene is activated
	 */
	public void addDevice(int sceneId, ZWaveSceneDevice d) {
		if (!sceneManagerStore.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return;
		}
		
		ZWaveScene scene = sceneManagerStore.get(sceneId);
		scene.addDevice(d);
		sceneManagerStore.put(sceneId, scene);
	}

	public HashMap<Integer, ZWaveSceneDevice> getDevices(int sceneId) {
		if (!sceneManagerStore.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return null;
		}

		ZWaveScene zTemp = sceneManagerStore.get(sceneId);
		return zTemp.getDevices();
	}

	/**
	 * Create groups of devices that do not support the  scene activation command class by value.
	 * @param sceneId
	 * @return HashMap<Integer, ArrayList<ZWaveSceneDevice>> H<value, ZWaveSceneDeviceList>
	 */
	public HashMap<Integer, ArrayList<ZWaveSceneDevice>> groupDevicesByLevels(int sceneId) {
		if (!sceneManagerStore.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return null;
		}

		// Init groups
		HashMap<Integer, ArrayList<ZWaveSceneDevice>>  groups = new HashMap<Integer, ArrayList<ZWaveSceneDevice>>();

		// Get scene devices
		ZWaveScene zTemp = sceneManagerStore.get(sceneId);
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
	
	@XStreamAlias("sceneManagerStore")
	private class ZWaveSceneManagerStore extends HashMap<Integer, ZWaveScene> {		
		private static final long serialVersionUID = 614162034025808031L;
		ZWaveSceneManagerStore() {
			super();
		}
	}
	
}
