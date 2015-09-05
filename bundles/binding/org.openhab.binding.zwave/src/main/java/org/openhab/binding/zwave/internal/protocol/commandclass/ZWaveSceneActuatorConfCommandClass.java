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
 * Handles the SceneActuatorConf command class.
 * The Scene Actuator Configuration Command Class is used to configure
 * scenes in scene devices like multilevel scene switch, binary
 * scene switch etc. A scene device must support 255 scene IDs.
 *
 * @author Pedro Paixao
 * @since 1.8.0
 */

@XStreamAlias("sceneActuatorConfCommandClass")
public class ZWaveSceneActuatorConfCommandClass extends ZWaveCommandClass {

	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveSceneActuatorConfCommandClass.class);

	private static final int SCENE_ACTUATOR_CONF_SET = 0x01;
	private static final int SCENE_ACTUATOR_CONF_GET = 0x02;
	private static final int SCENE_ACTUATOR_CONF_REPORT = 0x03;

	private int sceneId;
	private int level;
	private int dimmingDuration;

	private boolean isGetSupported = true;

	/**
	 * Creates a new instance of the ZWaveSceneActuatorConfCommandClass class.
	 * @param node the node this command class belongs to
	 * @param controller the controller to use
	 * @param endpoint the endpoint this Command class belongs to
	 */
	public ZWaveSceneActuatorConfCommandClass(ZWaveNode node,
			ZWaveController controller, ZWaveEndpoint endpoint) {
		super(node, controller, endpoint);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public CommandClass getCommandClass() {
		return CommandClass.SCENE_ACTUATOR_CONF;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleApplicationCommandRequest(SerialMessage serialMessage,
			int offset, int endpoint) {
		logger.debug("NODE {}: Received SceneActuatorConf Request", this.getNode().getNodeId());
		int command = serialMessage.getMessagePayloadByte(offset);
		switch (command) {
			case SCENE_ACTUATOR_CONF_SET:
				logger.debug("NODE {}: SceneActuatorConf Set sent to the controller will be processed as SceneActuatorConf Report", this.getNode().getNodeId());
				// Process this as if it was a value report.
				processSceneActuatorConfReport(serialMessage, offset, endpoint);
				break;
			case SCENE_ACTUATOR_CONF_GET:
				logger.warn(String.format("Command 0x%02X not implemented.", command));
				return;
			case SCENE_ACTUATOR_CONF_REPORT:
				logger.trace("NODE {}: Process SceneActuatorConf Report", this.getNode().getNodeId());
				processSceneActuatorConfReport(serialMessage, offset, endpoint);
				break;
			default:
				logger.warn(String.format("Unsupported Command 0x%02X for command class %s (0x%02X).",
					command,
					this.getCommandClass().getLabel(),
					this.getCommandClass().getKey()));
		}
	}

	/**
	 * Processes a SCENE_ACTUATOR_CONF_REPORT / SCENE_ACTUATOR_CONF_SET message.
	 * @param serialMessage the incoming message to process.
	 * @param offset the offset position from which to start message processing.
	 * @param endpoint the endpoint or instance number this message is meant for.
	 */
	protected void processSceneActuatorConfReport(SerialMessage serialMessage, int offset,
			int endpoint) {

		sceneId = serialMessage.getMessagePayloadByte(offset + 1);
		level = serialMessage.getMessagePayloadByte(offset + 2);
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

		logger.debug(String.format("NODE %d: SceneActuatorConf report, sceneID = %d, level=%d, dimming duration = %s", this.getNode().getNodeId(), sceneId, level, duration));
	}

	/**

	 * Gets a SerialMessage with the SCENE_ACTUATOR_CONF GET command
	 * @return the serial message
	 */
	public SerialMessage getValueMessage(int sceneId) {
		if(isGetSupported == false) {
			logger.debug("NODE {}: Node doesn't support get requests", this.getNode().getNodeId());
			return null;
		}

		logger.debug("NODE {}: Creating new message for application command SCENE_ACTUATOR_CONF_GET", this.getNode().getNodeId());
		SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData, SerialMessageType.Request, SerialMessageClass.ApplicationCommandHandler, SerialMessagePriority.Get);
		byte[] newPayload = { 	
							(byte) this.getNode().getNodeId(),
							3,
							(byte) getCommandClass().getKey(),
							(byte) SCENE_ACTUATOR_CONF_GET,
							(byte) sceneId };
		
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
	 * Gets a SerialMessage with the SCENE_ACTUATOR_CONF SET command
	 * @param the level to set.
	 * @return the serial message
	 */
	public SerialMessage setValueMessage(byte sceneId, byte level, byte duration, boolean override) {
		logger.debug("NODE {}: Creating new message for application command SCENE_ACTUATOR_CONF_SET", this.getNode().getNodeId());
		SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData, SerialMessageType.Request, SerialMessageClass.SendData, SerialMessagePriority.Set);
		byte o = 0;
		
		if (override) {
			o = (byte) 0x80;
		}

		byte[] newPayload = { 	
								(byte) this.getNode().getNodeId(),
								6,
								(byte) getCommandClass().getKey(),
								(byte) SCENE_ACTUATOR_CONF_SET,
								(byte) sceneId,
								(byte) duration,
								(byte) o,
								(byte) level
							};
		result.setMessagePayload(newPayload);
		return result;
	}
}
