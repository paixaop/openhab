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

import org.openhab.binding.zwave.internal.protocol.ZWaveScene;
import org.openhab.binding.zwave.internal.protocol.ZWaveSceneException;
import org.openhab.binding.zwave.internal.protocol.ZWaveSceneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * ZWaveSceneSerializer class. Serializes nodes to XML and back again.
 * 
 * @author Pedro Paixao
 * @since 1.8.0
 */
public class ZWaveSceneManagerSerializer {

	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneManagerSerializer.class);
	private final XStream stream = new XStream(new StaxDriver());
	private String folderName = "etc/zwave";

	/**
	 * Constructor. Creates a new instance of the {@link ZWaveSceneManagerSerializer}
	 * class.
	 */
	public ZWaveSceneManagerSerializer() {
		logger.trace("Initializing ZWaveSceneManagerSerializer.");

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
		//stream.processAnnotations(ZWaveScene.class);
		
		logger.trace("Initialized ZWaveSceneManagerSerializer.");
	}

	/**
	 * Serializes an XML tree of a {@link ZWaveScene}
	 * 
	 * @param node
	 *            the node to serialize
	 */
	public void serialize(ZWaveSceneManager sceneManager) {
		synchronized (stream) {
			File file = new File(this.folderName, String.format("scenes.xml"));
			BufferedWriter writer = null;

			logger.debug("Serializing Scene Manager to file {}", file.getPath());

			try {
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
				stream.registerConverter(new SceneManagerConverter());
                stream.alias("scenes", ZWaveSceneManager.class);
				stream.marshal(sceneManager, new PrettyPrintWriter(writer));
				writer.flush();
			} catch (IOException e) {
				logger.error("Scene Manager: Error serializing to file: {}", e.getMessage());
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
	public Object deserialize(ZWaveSceneManager sm) {
		synchronized (stream) {
			File file = new File(this.folderName, "scenes.xml");
			BufferedReader reader = null;

			logger.debug("SCENE {}: Deserializing from file {}", file.getPath());

			if (!file.exists()) {
				logger.debug("SCENE MANAGER: scenes.xml file does not exist.");
				return null;
			}

			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				stream.registerConverter(new SceneManagerConverter());
				stream.alias("scenes", ZWaveSceneManager.class);
				sm = (ZWaveSceneManager) stream.fromXML(reader, sm);
				return sm;
			} catch (IOException e) {
				logger.error("SCENE MANAGER: Error deserializing from file: {}", e.getMessage());
			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e) {
						logger.error("SCENE MANAGER: Error deserializing from file: {}", e.getMessage());
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
	public boolean deleteScene(int sceneId) {
		synchronized (stream) {
			File file = new File(this.folderName, String.format("scene%d.xml", sceneId));

			return file.delete();
		}
	}
	
	public class SceneManagerConverter implements Converter {

	    public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
	        return type.equals(ZWaveSceneManager.class);
	    }

	    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
	        ZWaveSceneManager sceneManager = (ZWaveSceneManager) source;
	        
	        for(ZWaveScene scene : sceneManager.getScenes().values()) {
	        	writer.startNode("scene");
		        writer.startNode("id");
		        writer.setValue(Integer.toString(scene.getId()));
		        writer.endNode();
		        
		        writer.startNode("name");
		        writer.setValue(scene.getName());
		        writer.endNode();
		        
		        writer.startNode("duration");
		        writer.setValue(Byte.toString(scene.getDimmingDuration()));
		        writer.endNode();
		        
		        writer.startNode("devices");
		        for (Integer nodeId : scene.getDevices().keySet()) {
		        	writer.startNode("device");
		        	writer.addAttribute("node", ((Integer)nodeId).toString());
		            writer.setValue(Byte.toString(scene.getDevice(nodeId).getValue()));
		            writer.endNode();
		        }
		        writer.endNode();
		        
		        writer.startNode("controllers");
		        for (Integer nodeId : scene.getSceneControllers().keySet()) {
		        	writer.startNode("controller");
		        	writer.addAttribute("node", ((Integer)nodeId).toString());
		            writer.setValue(((Integer)scene.getSceneControllerButton(nodeId)).toString());
		            writer.endNode();
		        }
		        writer.endNode();
		        
		        writer.endNode();
	        }
	    }
	    
	    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
	    	ZWaveSceneManager sceneManager = (ZWaveSceneManager)context.currentObject();
	    	if(sceneManager == null) {
	    		logger.error("Could not deserialize scenes.");
	    		return null;
	    	}
	    	try {
	    		int sceneId = 0;
	    		String nodeName = reader.getNodeName();
	    		if (!"scenes".equalsIgnoreCase(nodeName)) {
	    			 throw new ZWaveSceneException("Bad scenes.xml file format. Could not find <scenes></scenes> element", 0);
	    		}
	    		reader.moveDown();
	    		while (reader.hasMoreChildren()) {
		    		nodeName = reader.getNodeName();
		            if ("scene".equalsIgnoreCase(nodeName)) {
		            	while (reader.hasMoreChildren()) {
			            	// In the scene element
			            	reader.moveDown();
			            	nodeName = reader.getNodeName();
			            	
			            	if (!"id".equalsIgnoreCase(nodeName) && sceneId == 0) {
			            		logger.error("Scene ID must be defined before name in scenes.xml file {}", nodeName);
	            				throw new ZWaveSceneException("Scene ID must be defined before name in scenes.xml file", 0);
			            	}
			            	
		            		if ("id".equalsIgnoreCase(nodeName)) {
		            			sceneId = Math.abs(Integer.parseInt(reader.getValue()));
		            			if( sceneId != 0 ) {
		            				sceneManager.newScene(sceneId);
    	            			}
    	            			else {
    	            				logger.error("Scene ID needs to be >1 in scene {}", sceneId);
    	            				throw new ZWaveSceneException("Controller node ID needs to be >1", sceneId);
    	            			}
		            			
		            		}
		            		else if ("name".equalsIgnoreCase(nodeName)) {
		            			ZWaveScene s = sceneManager.getScene(sceneId);
		            			s.setName(reader.getValue());
		            			sceneManager.setScene(sceneId, s);
		            		}
		            		else if ("duration".equalsIgnoreCase(nodeName)) {
		            			ZWaveScene s = sceneManager.getScene(sceneId);
		            			s.setDimmingDuration(Byte.parseByte(reader.getValue()));
		            			sceneManager.setScene(sceneId, s);
		            		}
		            		else if ("devices".equalsIgnoreCase(nodeName)) {
		            			while (reader.hasMoreChildren()) {
		            				reader.moveDown();
		            				nodeName = reader.getNodeName();
		            				if ("device".equalsIgnoreCase(nodeName)) {
		    	            			int nodeId = Integer.parseInt(reader.getAttribute("node"));
		    	            			int value = Math.abs(Integer.parseInt(reader.getValue()));
		    	            			
		    	            			if( nodeId != 0 ) {
		    	            				sceneManager.addDevice(sceneId, nodeId, value);
		    	            			}
		    	            			else {
		    	            				logger.error("Scene Device node ID needs to be >1 in scene {}", sceneId);
		    	            				throw new ZWaveSceneException("Controller node ID needs to be >1", sceneId);
		    	            			}
		    	            			
		    	            			
		    	            		}
		            				reader.moveUp();
		            			}
		            			
		            		}
		            		else if ("controllers".equalsIgnoreCase(nodeName)) {
		            			while (reader.hasMoreChildren()) {
		            				reader.moveDown();
		            				nodeName = reader.getNodeName();
		            				if ("controller".equalsIgnoreCase(nodeName)) {
		    	            			int nodeId = Integer.parseInt(reader.getAttribute("node"));
		    	            			int button = Integer.parseInt(reader.getValue());
		    	            			if( nodeId != 0 && button !=0) {
		    	            				sceneManager.addSceneController(sceneId, nodeId, button);
		    	            			}
		    	            			else {
		    	            				logger.error("Scene Controller node and groupId/buttonId needs to be >1 in scene {}", sceneId);
		    	            				throw new ZWaveSceneException("Controller node and groupId/buttonId needs to be >1",sceneId);
		    	            			}
		    	            			
		    	            		}
		            				reader.moveUp();
		            			}
		            		}
		            		reader.moveUp();
		            	}
		            	
		            }
		            reader.moveUp();
		    	}
	    		
	    	}
	    	catch (ZWaveSceneException e) {
	    		logger.error(String.format("SCENE MANAGER: Error reading scenes from file. Error : %s", e.getMessage()));
	    	}
			return sceneManager;
	   }
	}
}
