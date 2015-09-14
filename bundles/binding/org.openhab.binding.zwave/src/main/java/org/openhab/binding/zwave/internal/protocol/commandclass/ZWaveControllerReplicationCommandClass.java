/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.commandclass;

import java.util.Arrays;

import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEndpoint;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Handles Controller Replication Messages
 * @author Pedro Paixao
 * @since 1.8.0
 */

@XStreamAlias("controllerReplicationCommandClass")
public class ZWaveControllerReplicationCommandClass extends ZWaveCommandClass {

	@XStreamOmitField
	private static final Logger logger = LoggerFactory.getLogger(ZWaveBasicCommandClass.class);
	
	private static final int CTRL_REPLICATION_TRANSFER_GROUP = 0x31;
	private static final int CTRL_REPLICATION_TRANSFER_GROUP_NAME = 0x32;
	private static final int CTRL_REPLICATION_TRANSFER_SCENE = 0x33;
	private static final int CTRL_REPLICATION_TRANSFER_SCENE_NAME = 0x34;
	
	/**
	 * Creates a new instance of the ZWaveControllerReplicationCommandClass class.
	 * @param node the node this command class belongs to
	 * @param controller the controller to use
	 * @param endpoint the endpoint this Command class belongs to
	 */
	public ZWaveControllerReplicationCommandClass(ZWaveNode node,
			ZWaveController controller, ZWaveEndpoint endpoint) {
		super(node, controller, endpoint);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public CommandClass getCommandClass() {
		return CommandClass.CONTROLLER_REPLICATION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleApplicationCommandRequest(SerialMessage serialMessage,
			int offset, int endpoint) {
		logger.debug(String.format("Received Controller Replication for Node ID = %d", this.getNode().getNodeId()));
		int command = serialMessage.getMessagePayloadByte(offset);
		switch (command) {
			case CTRL_REPLICATION_TRANSFER_GROUP:
				logger.debug("Controller Replication Transfer Group");
				processControllerReplicationTransferGroup(serialMessage, offset, endpoint);
				break;
			case CTRL_REPLICATION_TRANSFER_GROUP_NAME:
				logger.debug("Controller Replication Transfer Group Name");
				processControllerReplicationTransferGroupName(serialMessage, offset, endpoint);
				break;
			case CTRL_REPLICATION_TRANSFER_SCENE:
				logger.debug("Controller Replication Transfer Scene");
				processControllerReplicationTransferScene(serialMessage, offset, endpoint);
				break;
			case CTRL_REPLICATION_TRANSFER_SCENE_NAME:
				logger.debug("Controller Replication Transfer Scene Name");
				processControllerReplicationTransferSceneName(serialMessage, offset, endpoint);
				break;
			default:
				logger.warn(String.format("Unsupported Command 0x%02X for command class %s (0x%02X).", 
					command, 
					this.getCommandClass().getLabel(),
					this.getCommandClass().getKey()));
		}
	}
	
	/**
	 * Processes a CTRL_REPLICATION_TRANSFER_GROUP_NAME message.
	 * @param serialMessage the incoming message to process.
	 * @param offset the offset position from which to start message processing.
	 * @param endpoint the endpoint or instance number this message is meant for.
	 */
	protected void processControllerReplicationTransferGroup(SerialMessage serialMessage, int offset, int endpoint) {
        // skip first byte with sequence number              
        int groupId = serialMessage.getMessagePayloadByte(offset + 2);
        int nodeId = serialMessage.getMessagePayloadByte(offset + 3);
        
        logger.debug(String.format("Controller Replication Transfer Group - groupId = %d, nodeId = %s",groupId, nodeId));
	}

	/**
	 * Processes a CTRL_REPLICATION_TRANSFER_GROUP message.
	 * @param serialMessage the incoming message to process.
	 * @param offset the offset position from which to start message processing.
	 * @param endpoint the endpoint or instance number this message is meant for.
	 */
	protected void processControllerReplicationTransferGroupName(SerialMessage serialMessage, int offset, int endpoint) {
        // skip first byte with sequence number              
        int groupId = serialMessage.getMessagePayloadByte(offset + 2);
        byte[] msg = serialMessage.getMessagePayload();
        byte[] groupNameBytes = Arrays.copyOfRange(msg, offset + 3, msg.length);
        String groupName = new String(groupNameBytes);
        
        logger.debug(String.format("Controller Replication Transfer Group Name - groupId = %d, groupName = %s",groupId, groupName));
	}
	
	/**
	 * Processes a CTRL_REPLICATION_TRANSFER_SCENE message.
	 * @param serialMessage the incoming message to process.
	 * @param offset the offset position from which to start message processing.
	 * @param endpoint the endpoint or instance number this message is meant for.
	 */
	protected void processControllerReplicationTransferScene(SerialMessage serialMessage, int offset, int endpoint) {
        // skip first byte with sequence number              
        int sceneId = serialMessage.getMessagePayloadByte(offset + 2);
        int nodeId = serialMessage.getMessagePayloadByte(offset + 3);
        int level = serialMessage.getMessagePayloadByte(offset + 4);

        logger.debug(String.format("Controller Replication Transfer Scene - sceneId = %d, nodeId = %d, level = %d",sceneId, nodeId, level));
	}
	
	/**
	 * Processes a CTRL_REPLICATION_TRANSFER_SCENE_NAME message.
	 * @param serialMessage the incoming message to process.
	 * @param offset the offset position from which to start message processing.
	 * @param endpoint the endpoint or instance number this message is meant for.
	 */
	protected void processControllerReplicationTransferSceneName(SerialMessage serialMessage, int offset, int endpoint) {
        // skip first byte with sequence number              
        int sceneId = serialMessage.getMessagePayloadByte(offset + 2);
        
        byte[] msg = serialMessage.getMessagePayload();
        byte[] sceneNameBytes = Arrays.copyOfRange(msg, offset + 3, msg.length);
        String sceneName = new String(sceneNameBytes);
        
        logger.debug(String.format("Controller Replication Transfer Scene Name - sceneId = %d, sceneName = %s",sceneId, sceneName));
	}
}
