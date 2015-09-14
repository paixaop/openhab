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
import java.util.Collection;
import java.util.HashMap;

import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveSceneActivationCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * This class manages all Z-Wave Scenes
 * This is then serialized to XML.
 *
 * @author Pedro Paixao
 * @since 1.8.0
 *
 */

/**
 * Z-Wave Scene Support
 * 
 * Scene Manager manages scenes for one Z-Wave controller
 * 
 * Each Z-Wave network supports up to 255 scenes, each with many devices or Z-Wave nodes.
 * Only nodes in the same network, i.e, same Home ID can participate in a scene.
 *   
 * @author Pedro Paixao
 */

@XStreamAlias("zwaveSceneManager")
public class ZWaveSceneManager implements ZWaveEventListener {
	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneManager.class);

	// Maximum number of scenes supported by ZWave
	private static final int MAX_NUMBER_OF_SCENES = 256;
	
	// Hash of Z-Wave Home IDs as Keys and scenes as Values
	// scenes are themselves a hash of scene ID as key and ZWaveScene values
	private ZWaveSceneManagerStore sceneManagerStore;
	private ZWaveSceneControllerStore sceneControllerStore;
	
	
	@XStreamOmitField
	private ZWaveController controller;

	ZWaveSceneManager(ZWaveController zController) {
		controller = zController;
		sceneManagerStore = new ZWaveSceneManagerStore();
		sceneControllerStore = new ZWaveSceneControllerStore();
	}

	/**
	 * Get the number of registered scenes.
	 * @return number of registered scenes.
	 */
	public int numberOfScenes() {
		return sceneManagerStore.size();
	}
	
	/**
	 * Add a scene controller to the controller store and bind a scene to a button (groupID) on
	 * said controller
	 * @param sceneId ID of the Z-Wave Scene to bind to controller and button
	 * @param nodeId ID of the scene controller node
	 * @param groupId ID of the button which will be used to activate the scene
	 */
	public void addSceneController(int sceneId, int nodeId, int groupId) {
		ZWaveScene scene = new ZWaveScene(controller, sceneId);
		
		if (sceneManagerStore.containsKey(sceneId)) {
			scene = sceneManagerStore.get(sceneId);
		}
		
		ZWaveSceneController sc = new ZWaveSceneController(controller, nodeId);
		if (sc.getNode() != null) {
			scene.putSceneController(sc, groupId);
			sceneManagerStore.put(sceneId, scene);
			logger.info("NODE {} Scene Controller Button {} assigned to Scene {}", nodeId, groupId, sceneId);
		}
		else {
			logger.error("NODE {} is not a scene controller. Ignoring", nodeId);
		}
	}
	
	/**
	 * Remove a scene controller/button scene binding
	 * @param sceneId
	 * @param nodeId
	 */
	public void removeSceneController(int sceneId, int nodeId) {
		if (sceneManagerStore.containsKey(sceneId)) {
			logger.info("Removing Scene Controller {} from scene {}", sceneId, nodeId);
			ZWaveScene scene = sceneManagerStore.get(sceneId);
			scene.removeSceneController(nodeId);
			sceneManagerStore.put(sceneId, scene);
		}
		else {
			logger.info("Scene Controller {} is not controlling scene {}. Ignoring", sceneId, nodeId);
		}
	}
	
	/**
	 * Get the list of controllers that can control a scene
	 * @param sceneId ID of the scene
	 * @return Hash list of controllers' node IDs and button/group IDs
	 */
	public HashMap<Integer, ZWaveSceneController> getSceneControllers(int sceneId) {
		if (sceneManagerStore.containsKey(sceneId)) {
			return sceneManagerStore.get(sceneId).getSceneControllers();
		}
		else {
			logger.info("Scene {} does not exist.", sceneId);
			return null;
		}
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
		if (sceneManagerStore.size() < MAX_NUMBER_OF_SCENES) {
			return sceneManagerStore.size() + 1;
		}
		else {
			logger.info("Maximum number of scenes ({}) reached. Cannot add new scenes until you delete some.", MAX_NUMBER_OF_SCENES);
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
		if (sceneManagerStore.size() < MAX_NUMBER_OF_SCENES) {
			int sceneId = getLowestUnusedSceneId();
			ZWaveScene zTemp = new ZWaveScene(controller, sceneId);
			zTemp.setName(newName);
			sceneManagerStore.put(sceneId, zTemp);
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
	
	/**
	 * 
	 * @param sceneId
	 */
	public void activateScene(int sceneId) {
		if (sceneManagerStore.containsKey(sceneId)) {
			sceneManagerStore.get(sceneId).activate();
			return;
		}
		logger.info("Scene {} not found. Cannot activate it", sceneId);
	}
	
	/**
	 * Get a list of nodes that support the SCENE ACTIVATION command class
	 * @return list of nodes
	 */
	public ArrayList<ZWaveNode> getNodesSupportingSceneActivation() {
		Collection<ZWaveNode> nodes = controller.getNodes();
		ArrayList<ZWaveNode> sceneActivationNodes = new ArrayList<ZWaveNode>();
		
		for(ZWaveNode node : nodes) {
			ZWaveSceneActivationCommandClass sceneActivationCC = (ZWaveSceneActivationCommandClass)node.getCommandClass(CommandClass.SCENE_ACTIVATION);
			if (sceneActivationCC != null) {
				sceneActivationNodes.add(node);
			}
		}
		
		return sceneActivationNodes;
	}
	
	/**
	 * Program scene into Scene controllers and scene nodes
	 * @param sceneId int the scene ID 
	 */
	public void saveScenesToNodes(int sceneId) {
		
	}
	
	/**
	 * Process Z-Wave events and if they are SCENE_ACTIVATION command class value events
	 * extract the scene ID
	 * @param Z-Wave Events from controller
	 */
	@Override
	public void ZWaveIncomingEvent(ZWaveEvent event) {
		
		// Check if we got a Z-Wave Value Event
		if (event instanceof ZWaveCommandClassValueEvent) {
			ZWaveCommandClassValueEvent valueEvent = (ZWaveCommandClassValueEvent) event;
			
			// Is it an INDICATOR Command Class event for this node
			if (valueEvent.getCommandClass() == CommandClass.SCENE_ACTIVATION) {
				
				// get the indicator value from the event
				int sceneId = ((Integer) valueEvent.getValue()).intValue();
				
				// the indicator state is now valid
				activateScene(sceneId);
			}
		}
	}
	
	@XStreamAlias("sceneManagerStore")
	private class ZWaveSceneManagerStore extends HashMap<Integer, ZWaveScene> {		
		private static final long serialVersionUID = 614162034025808031L;
		ZWaveSceneManagerStore() {
			super();
		}
	}
	
	/**
	 * Store Scene Controller Data Store
	 *  
	 * sceneId.nodeId.groupId
	 * 
	 * sceneId -  ID of the scene that a controller can activate typically through a button press
	 * nodeId  -  ID of the Z-Wave node of the scene controller  
	 * groupId -  ID of the scene controller group, AKA, controller button
	 * 
	 * So this hash maps scenes to nodes and nodes to buttons on the scene controller.
	 *
	 */
	@XStreamAlias("sceneControllerStore")
	private class ZWaveSceneControllerStore extends HashMap<Integer, HashMap<Integer, Integer>> {		
		private static final long serialVersionUID = -4610300876380231219L;
		ZWaveSceneControllerStore() {
			super();
		}
	}
	
}
