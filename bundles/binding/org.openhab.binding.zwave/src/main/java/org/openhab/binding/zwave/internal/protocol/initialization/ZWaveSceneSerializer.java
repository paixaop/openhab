/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.initialization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.openhab.binding.zwave.internal.protocol.ZWaveDeviceClass;
import org.openhab.binding.zwave.internal.protocol.ZWaveEndpoint;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.ZWaveScene;
import org.openhab.binding.zwave.internal.protocol.ZWaveSceneController;
import org.openhab.binding.zwave.internal.protocol.ZWaveSceneDevice;
import org.openhab.binding.zwave.internal.protocol.ZWaveSceneManager;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveMeterCommandClass.MeterScale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * ZWaveSceneSerializer class. Serializes nodes to XML and back again.
 * 
 * @author Pedro Paixao
 * @since 1.8.0
 */
public class ZWaveSceneSerializer {

	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneSerializer.class);
	private final XStream stream = new XStream(new StaxDriver());
	private String folderName = "etc/zwave";

	/**
	 * Constructor. Creates a new instance of the {@link ZWaveSceneSerializer}
	 * class.
	 */
	public ZWaveSceneSerializer() {
		logger.trace("Initializing ZWaveSceneSerializer.");

		// Change the folder for OH2
		// ConfigConstants.getUserDataFolder();
		final String USERDATA_DIR_PROG_ARGUMENT = "smarthome.userdata";
		final String eshUserDataFolder = System.getProperty(USERDATA_DIR_PROG_ARGUMENT);
		if (eshUserDataFolder != null) {
		    folderName = eshUserDataFolder + "/zwave";
		}

		final File folder = new File(folderName);

		// create path for serialization.
		if (!folder.exists()) {
			logger.debug("Creating directory {}", folderName);
			folder.mkdirs();
		}
		stream.processAnnotations(ZWaveScene.class);
		stream.processAnnotations(ZWaveSceneManager.class);
		stream.processAnnotations(ZWaveSceneController.class);
		stream.processAnnotations(ZWaveSceneDevice.class);
		
		logger.trace("Initialized ZWaveSceneSerializer.");
	}

	/**
	 * Serializes an XML tree of a {@link ZWaveScene}
	 * 
	 * @param node
	 *            the node to serialize
	 */
	public void SerializeScene(ZWaveScene scene) {
		synchronized (stream) {
			File file = new File(this.folderName, String.format("scene%d.xml", scene.getId()));
			BufferedWriter writer = null;

			logger.debug("SCENE {}: Serializing to file {}", scene.getId(), file.getPath());

			try {
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
				stream.marshal(scene, new PrettyPrintWriter(writer));
				writer.flush();
			} catch (IOException e) {
				logger.error("SCENE {}: Error serializing to file: {}", scene.getId(), e.getMessage());
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	/**
	 * Deserializes an XML tree of a {@link ZWaveScene}
	 * 
	 * @param sceneId
	 *            the number of the node to deserialize
	 * @return returns the scene or null in case Serialization failed.
	 */
	public ZWaveScene DeserializeNode(int sceneId) {
		synchronized (stream) {
			File file = new File(this.folderName, String.format("scene%d.xml", sceneId));
			BufferedReader reader = null;

			logger.debug("SCENE {}: Serializing from file {}", ©, file.getPath());

			if (!file.exists()) {
				logger.debug("SCENE {}: Error serializing from file: file does not exist.", sceneId);
				return null;
			}

			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				return (ZWaveScene)stream.fromXML(reader);
			} catch (IOException e) {
				logger.error("NODE {}: Error serializing from file: {}", sceneId, e.getMessage());
			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e) {
					}
			}
			return null;
		}
	}
	
	/**
	 * Deletes the persistence store for the specified scene.
	 * 
	 * @param nodeId The node ID to remove
	 * @return true if the file was deleted
	 */
	public boolean DeleteScene(int sceneId) {
		synchronized (stream) {
			File file = new File(this.folderName, String.format("scene%d.xml", sceneId));

			return file.delete();
		}
	}
}
