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
import java.util.Timer;
import java.util.TimerTask;

import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveIndicatorCommandClassChangeEvent;
import org.openhab.binding.zwave.internal.protocol.initialization.ZWaveSceneManagerSerializer;
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
	
	@XStreamOmitField
	private ZWaveSceneManagerSerializer sceneSerializer = new ZWaveSceneManagerSerializer();

	// Maximum number of scenes supported by ZWave
	private static final int MAX_NUMBER_OF_SCENES = 255;
	
	// Hash of Z-Wave Home IDs as Keys and scenes as Values
	// scenes are themselves a hash of scene ID as key and ZWaveScene values
	private HashMap<Integer, ZWaveScene> sceneManagerStore;
	
	// <controlerNodeId, <groupId, sceneId>>
	private HashMap<Integer, HashMap<Integer, Integer>> sceneControllerStore;
	
	@XStreamOmitField
	private ZWaveController controller;

	ZWaveSceneManager(ZWaveController zController) {
		controller = zController;
		sceneManagerStore = new HashMap<Integer, ZWaveScene>();
		sceneControllerStore = new HashMap<Integer, HashMap<Integer, Integer>>();
		
		logger.info("SCENE MANAGER: Register event listener");
		controller.addEventListener(this);
		
		Timer initTimer = new Timer();
		initTimer.schedule(new ProgramTestSceneTask(), 1000);
		
	}
	
	private class ProgramTestSceneTask extends TimerTask {
		@Override
		public void run() {
			testScene();
		}
	}
	
	public void testScene() {
		int sceneId = 0;
		try {
			sceneId = newScene("test");
		} catch (ZWaveSceneException e) {
			e.printStackTrace();
		}
		logger.info("Scene Manager Test Scene {}", sceneId);
		/* addDevice(sceneId, 4, 50);
		addSceneController(sceneId, 2, 1);
		addSceneController(sceneId, 3, 1);
		getScene("test").program();
		sceneSerializer.serialize(this); */
		sceneSerializer.deserialize(this);
	}
	
	public HashMap<Integer, ZWaveScene> getScenes() {
		return sceneManagerStore;
	}
	
	/**
	 * Get a scene object from the manager by scene ID
	 * @param id of the scene
	 * @return ZWaveScene
	 */
	public ZWaveScene getScene(int id) {
		return sceneManagerStore.get(id);
	}
	
	
	/**
	 * Get a scene object from the manager by name
	 * @param name of the scene
	 * @return ZWaveScene
	 */
	public ZWaveScene getScene(String name) {
		for(ZWaveScene s : sceneManagerStore.values()) {
			if (s.getName() == name) {
				return s;
			}
		}
		return null;
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
		
		if (!sceneManagerStore.containsKey(sceneId)) {
			logger.error("Scene {} not found. Cannot add scene controller {} to it., sceneId, nodeId");
			return;
		}
		
		ZWaveScene scene = sceneManagerStore.get(sceneId);
		ZWaveSceneController sc = new ZWaveSceneController(controller, nodeId);
		if (sc.getNode() != null) {
			scene.putSceneController(sc, groupId);
			sceneManagerStore.put(sceneId, scene);
			logger.info("NODE {} Scene Controller Button {} assigned to Scene {}", nodeId, groupId, sceneId);
			
			// Store all controller devices to ease event handling
			HashMap<Integer, Integer> scenes;
			if(sceneControllerStore.containsKey(nodeId)) {
				scenes = sceneControllerStore.get(nodeId);
			}
			else {
				scenes = new HashMap<Integer, Integer>();
			}
			scenes.put(groupId, sceneId);
			sceneControllerStore.put(nodeId, scenes);
		}
		else {
			logger.error("NODE {} is not a scene controller. Cannot add it to scene {}", nodeId, sceneId);
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
	 * Get the lowest scene ID that is free for usage
	 * @return int scene ID
	 */
	public int getLowestUnusedSceneId() {
		if (sceneManagerStore.isEmpty()) {
			return 1;
		}
		for(int i = 1; i< sceneManagerStore.size(); i++) {
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
	 * @throws ZWaveSceneException 
	 */
	public int newScene() throws ZWaveSceneException {
		return newScene("");
	}
	
	/**
	 * Add a new Z-Wave scene to the scene manager
	 * @param scene ID
	 * @throws ZWaveSceneException 
	 */
	public int newScene(int sceneId) throws ZWaveSceneException {
		return newScene(sceneId, "");
	}
	
	/**
	 * Add a new Z-Wave scene to the scene manager
	 * The next lowest scene number available will be used
	 * @param scene name
	 * @throws ZWaveSceneException 
	 */
	public int newScene(String name) throws ZWaveSceneException {
		return newScene(0, name);
	}


	/**
	 * Add a new Z-Wave scene to the scene manager with a scene name
	 * @param scene ID
	 * @param newName String with name of scene
	 * @return sceneId in case the scene is successful added, 0 otherwise
	 * @throws Exception 
	 */
	public int newScene(int sceneId, String newName) throws ZWaveSceneException {
		if (sceneId > MAX_NUMBER_OF_SCENES) {
			throw new ZWaveSceneException(String.format("Scene ID needs to be smaller than %d. You entered %d", MAX_NUMBER_OF_SCENES),sceneId);
		}
		if (sceneManagerStore.size() < MAX_NUMBER_OF_SCENES) {
			
			if (sceneId == 0) {
				sceneId = getLowestUnusedSceneId();
			}
			
			if(sceneId == 0) {
				throw new ZWaveSceneException(String.format("Scene Manager exceeded the number of available scenes %d", MAX_NUMBER_OF_SCENES),0);
			}
			
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
	
	public void setScene(int sceneId, ZWaveScene scene) {
		sceneManagerStore.put(sceneId, scene);
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

	public void addDevice(int sceneId, int nodeId, int value) {
		if (!sceneManagerStore.containsKey(sceneId)) {
			logger.error("Invalid sceneId {}", sceneId);
			return;
		}
		
		ZWaveScene scene = sceneManagerStore.get(sceneId);
		scene.addDevice(nodeId, (byte) value);
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

		return sceneManagerStore.get(sceneId).getDevices();
	}

	/**
	 * Activate a Scene
	 * @param sceneId
	 */
	public void activateScene(int sceneId) {
		if (sceneManagerStore.containsKey(sceneId)) {
			logger.info("Scene {} found. Activating it.", sceneId);
			ZWaveScene scene = sceneManagerStore.get(sceneId);
			if (scene != null) {
				scene.activate();
			}
			else {
				logger.error("Scene {} was found yet its object was null", sceneId);
			}
			return;
		}
		logger.info("Scene {} not found. Cannot activate it", sceneId);
	}
	
	/**
	 * Process Z-Wave events and if they are SCENE_ACTIVATION command class value events
	 * extract the scene ID
	 * @param Z-Wave Events from controller
	 */
	@Override
	public void ZWaveIncomingEvent(ZWaveEvent event) {
		// Ignore if event does not come from a Scene Controller that is bound 
		// to a scene in the scene manager
		if(!sceneControllerStore.containsKey(event.getNodeId())) {
			return;
		}
		
		// Check if we got a Z-Wave Value Event
		if (event instanceof ZWaveCommandClassValueEvent) {
			ZWaveCommandClassValueEvent valueEvent = (ZWaveCommandClassValueEvent) event;
			
			logger.info("SCENE MANAGER: Incoming Scene Controller Event from Node {} Comand Class {}", valueEvent.getNodeId(), valueEvent.getCommandClass().getLabel() );
			
			// Is it a SCENE ACTIVATION Command Class event?
			if (valueEvent.getCommandClass() == CommandClass.SCENE_ACTIVATION) {
				
				int sceneId = ((Integer) valueEvent.getValue()).intValue();
				logger.info("Scene Activation Event for scene {}", sceneId);
				activateScene(sceneId);
				return;
				
			}
			
			// Is it an INDICATOR Command Class event?
			if (valueEvent.getCommandClass() == CommandClass.INDICATOR) {
				
				ZWaveIndicatorCommandClassChangeEvent indicatorEvent = (ZWaveIndicatorCommandClassChangeEvent) event;
				int nodeId = valueEvent.getNodeId();
				logger.info("SCENE MANAGER: Incoming INDICATOR Event for node {}. Changes: ", nodeId, indicatorEvent.changes().toString());
				
				// For all the buttons that changed lets check what other buttons in 
				// other scene controllers are bound to the same scene
				for(Integer button : indicatorEvent.changes()) {
					// Get the scene
					int sceneId = sceneControllerStore.get(nodeId).get(button);
					ZWaveScene scene = sceneManagerStore.get(sceneId);
					if (scene.isSceneContollerBoundToScene(nodeId, button) ) {
						if (indicatorEvent.isBitOn(button)) {
							logger.info("NODE {} Got Indicator Change Event. Button {} changed from OFF to ON", nodeId, button);
						}
						else {
							logger.info("NODE {} Got Indicator Change Event. Button {} changed from ON to OFF", nodeId, button);
						}
						scene.syncSceneControllers(nodeId, indicatorEvent.isBitOn(button));
						sceneManagerStore.put(sceneId, scene);
					}
				}
			}
		}
	}
	
}
