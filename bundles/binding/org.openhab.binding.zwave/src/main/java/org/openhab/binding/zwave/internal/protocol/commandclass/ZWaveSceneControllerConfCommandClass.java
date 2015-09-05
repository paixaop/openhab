/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.commandclass;

import org.openhab.binding.zwave.internal.config.ZWaveDbCommandClass;
import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEndpoint;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageClass;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessagePriority;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Handles the SceneControllerConf command class.
 * The Scene Controller Configuration Command Class is used to configure 
 * scenes controlled from a scene controller 
 * Scene devices must support 255 scene IDs.
 * 
 * @author Pedro Paixao
 * @since 1.8.0
 */

@XStreamAlias("sceneControllerConfCommandClass")
public class ZWaveSceneControllerConfCommandClass extends ZWaveCommandClass {

	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneControllerConfCommandClass.class);
	
	private static final int SCENE_CONTROLLER_CONF_SET = 0x01;
	private static final int SCENE_CONTROLLER_CONF_GET = 0x02;
	private static final int SCENE_CONTROLLER_CONF_REPORT = 0x03;
	
	private int sceneId;
	private int groupId;
	private int dimmingDuration;

	private boolean isGetSupported = true;

	/**
	 * Creates a new instance of the ZWaveSceneControllerConfCommandClass class.
	 * @param node the node this command class belongs to
	 * @param controller the controller to use
	 * @param endpoint the endpoint this Command class belongs to
	 */
	public ZWaveSceneControllerConfCommandClass(ZWaveNode node,
			ZWaveController controller, ZWaveEndpoint endpoint) {
		super(node, controller, endpoint);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public CommandClass getCommandClass() {
		return CommandClass.SCENE_CONTROLLER_CONF;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleApplicationCommandRequest(SerialMessage serialMessage,
			int offset, int endpoint) {
		logger.debug("NODE {}: Received SceneControllerConf Request", this.getNode().getNodeId());
		int command = serialMessage.getMessagePayloadByte(offset);
		switch (command) {
			case SCENE_CONTROLLER_CONF_SET:
				logger.debug("NODE {}: SceneControllerConf Set sent to the controller will be processed as SceneControllerConf Report", this.getNode().getNodeId());
				// Process this as if it was a value report.
				processSceneControllerConfReport(serialMessage, offset, endpoint);
				break;
			case SCENE_CONTROLLER_CONF_GET:
				logger.warn(String.format("Command 0x%02X not implemented.", command));
				return;
			case SCENE_CONTROLLER_CONF_REPORT:
				logger.trace("NODE {}: Process SceneControllerConf Report", this.getNode().getNodeId());
				processSceneControllerConfReport(serialMessage, offset, endpoint);
				break;
			default:
				logger.warn(String.format("Unsupported Command 0x%02X for command class %s (0x%02X).", 
					command, 
					this.getCommandClass().getLabel(),
					this.getCommandClass().getKey()));
		}
	}

	/**
	 * Processes a SCENE_CONTROLLER_CONF_REPORT / SCENE_CONTROLLER_CONF_SET message.
	 * @param serialMessage the incoming message to process.
	 * @param offset the offset position from which to start message processing.
	 * @param endpoint the endpoint or instance number this message is meant for.
	 */
	protected void processSceneControllerConfReport(SerialMessage serialMessage, int offset,
			int endpoint) {
		
		groupId = serialMessage.getMessagePayloadByte(offset + 1);
		sceneId = serialMessage.getMessagePayloadByte(offset + 2);
		dimmingDuration = serialMessage.getMessagePayloadByte(offset + 3);
		
		String duration = "";
		if (dimmingDuration == 0) {
			duration = "Immediate";
		}
		else if (dimmingDuration <= 0x7F) {
			duration = dimmingDuration + " seconds";
		}
		else {
			duration = (dimmingDuration - 0x7F) + " minutes";
		}
		
		logger.debug(String.format("NODE %d: SceneControllerConf report, sceneID = %d, groupId=%d, dimming duration = %s", this.getNode().getNodeId(), sceneId, groupId, duration));
	}

	/**
	 * Gets a SerialMessage with the SCENE_CONTROLLER_CONF GET command 
	 * @return the serial message
	 */
	public SerialMessage getValueMessage(int groupId) {
		if(isGetSupported == false) {
			logger.debug("NODE {}: Node doesn't support get requests", this.getNode().getNodeId());
			return null;
		}

		logger.debug("NODE {}: Creating new message for application command SCENE_CONTROLLER_CONF_GET", this.getNode().getNodeId());
		SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData, SerialMessageType.Request, SerialMessageClass.ApplicationCommandHandler, SerialMessagePriority.Get);
    	byte[] newPayload = { 	(byte) this.getNode().getNodeId(), 
    							3, 
								(byte) getCommandClass().getKey(), 
								(byte) SCENE_CONTROLLER_CONF_GET,
								(byte) groupId };
    	result.setMessagePayload(newPayload);
    	return result;		
	}

	@Override
	public boolean setOptions (ZWaveDbCommandClass options) {
		if(options.isGetSupported != null) {
			isGetSupported = options.isGetSupported;
		}
		
		return true;
	}
	
	/**
	 * Gets a SerialMessage with the SCENE_CONTROLLER_CONF SET command 
	 * @param the level to set.
	 * @return the serial message
	 */
	public SerialMessage setValueMessage(byte groupId, byte sceneId, byte duration) {
		logger.debug("NODE {}: Creating new message for application command SCENE_CONTROLLER_CONF_SET", this.getNode().getNodeId());
		SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData, SerialMessageType.Request, SerialMessageClass.SendData, SerialMessagePriority.Set);
		
    	byte[] newPayload = { 	(byte) this.getNode().getNodeId(), 
    							5, 
								(byte) getCommandClass().getKey(), 
								(byte) SCENE_CONTROLLER_CONF_SET,
								(byte) groupId,
								(byte) sceneId,
								(byte) duration,
								};
    	result.setMessagePayload(newPayload);
    	return result;		
	}

}
