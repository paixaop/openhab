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
 * This class provides a storage class for zwave scenes
 * within the node class. This is then serialised to XML.
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
	private int dimmingDuration;
	private ZWaveController controller;

	public class ZWaveSceneDevice {
		ZWaveNode node;
		Integer value;
		boolean sceneSupport;

		ZWaveSceneDevice() {
			node = null;
			value = 0;
			sceneSupport = false;
		}

		public void setNode(ZWaveNode n) {
			node = n;
		}

		public ZWaveNode getNode() {
			return node;
		}

		public boolean isSceneSupported() {
			return sceneSupport;
		}

		public void setSceneSupport(boolean b) {
			sceneSupport = b;
		}

		public void setValue(Integer v) {
			value = v;
		}

		public Integer getValueInt() {
			return value;
		}

	}

	ZWaveScene(ZWaveController zController) {
		controller = zController;
	}

	HashMap<Integer, ZWaveSceneDevice> devices = new HashMap<Integer, ZWaveSceneDevice>();

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

	/**
	 * Add a new ZWave device to a Z-Wave Scene
	 * @param nodeId id of the node being added to the scene
	 * @param d ZWaveSceneDevice with the values and node information to
	 * 		  set when scene is activated
	 */
	public void addDevice(Integer nodeId, ZWaveSceneDevice d) {
		devices.put(nodeId, d);
	}

	/**
	 * Add a new ZWave device to a Z-Wave Scene
	 * @param nodeId id of the node being added to the scene
	 * @param value of the node that should be set when scene is activated
	 */
	public void addDevice(Integer nodeId, Integer value) {
		ZWaveSceneDevice d = new ZWaveSceneDevice();

		d.setNode(controller.getNode(nodeId));
		d.setValue(value);

		devices.put(nodeId, d);
	}

	public ZWaveSceneDevice getDevice(Integer nodeId) {
		return devices.get(nodeId);
	}

	public void removeDevice(Integer nodeId) {
		devices.remove(nodeId);
	}

	public HashMap<Integer, ZWaveSceneDevice> getDevices() {
		return devices;
	}

	public void addValue(Integer nodeId, String commandClass, Integer value) {
		ZWaveSceneDevice d = new ZWaveSceneDevice();

		if (devices.containsKey(nodeId)) {
			d = devices.get(nodeId);
		}

		if (d.nodeId != 0) {
			d.value = value;
		}
	}
}
