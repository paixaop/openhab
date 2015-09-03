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

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * This class provides a storage class for zwave scenes
 * within the node class. This is then serialised to XML.
 * 
 * @author Pedro Paixao
 * @since 1.8.0
 *
 */


@XStreamAlias("scene")
public class Scene {
	private String sceneName;
	private int sceneId;
	private int dimmingDuration;
	
	public class SceneDevice {
		Integer nodeId;
		String commandClass;
		Integer value;
		boolean sceneSupport;
		
		SceneDevice() {
			node = 0;
			commandClass = "";
			value = 0;
			sceneSupport = false;
		}
	}
	
	HashMap<Integer, SceneDevice> devices = new HashMap<Integer, SceneDevice>();
	
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
	
	public void addDevice(Integer nodeId, SceneDevice d) {
		devices.put(nodeId, d);
	}
	
	public SceneDevice getDevice(Integer nodeId) {
		return devices.get(nodeId);
	}
	
	public void removeDevice(Integer nodeId) {
		devices.remove(nodeId);
	}
	
	public HashMap<Integer, SceneDevice> getDevices() {
		return devices;
	}
	
	public void addValue(Integer nodeId, String commandClass, Integer value) {
		SceneDevice d = new SceneDevice();
		
		if (devices.containsKey(nodeId)) {
			d = devices.get(nodeId);
		}
		
		if (d.nodeId != 0) {
			d.value = value;
		}
	}
}
