/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.converter;

import java.util.ArrayList;
import java.util.Map;

import org.openhab.binding.zwave.internal.converter.command.IntegerCommandConverter;
import org.openhab.binding.zwave.internal.converter.command.ZWaveCommandConverter;
import org.openhab.binding.zwave.internal.converter.state.BigDecimalOnOffTypeConverter;
import org.openhab.binding.zwave.internal.converter.state.IntegerDecimalTypeConverter;
import org.openhab.binding.zwave.internal.converter.state.ZWaveStateConverter;
import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveIndicatorCommandClass;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveIndicatorCommandClassChangeEvent;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * ZWaveIndicatorConverter class. Converter for communication with the 
 * {@link ZWaveIndicatorCommandClass}.
 * @author Pedro Paixao
 * @since 1.8.0
 */
public class ZWaveIndicatorConverter extends ZWaveCommandClassConverter<ZWaveIndicatorCommandClass> {

	private static final Logger logger = LoggerFactory.getLogger(ZWaveIndicatorConverter.class);
	private static final int REFRESH_INTERVAL = 0; // refresh interval in seconds for the binary switch;

	/**
	 * Constructor. Creates a new instance of the {@link ZWaveIndicatorConverter} class.
	 * @param controller the {@link ZWaveController} to use for sending messages.
	 * @param eventPublisher the {@link EventPublisher} to use to publish events.
	 */
	public ZWaveIndicatorConverter(ZWaveController controller, EventPublisher eventPublisher) {
		super(controller, eventPublisher);
		
		// State and command converters used by this converter. 
		this.addStateConverter(new IntegerDecimalTypeConverter());
		this.addStateConverter(new BigDecimalOnOffTypeConverter());
		this.addCommandConverter(new IntegerCommandConverter());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SerialMessage executeRefresh(ZWaveNode node, 
			ZWaveIndicatorCommandClass commandClass, int endpointId, Map<String,String> arguments) {
		logger.debug("NODE {}: Generating poll message for {}, endpoint {}", node.getNodeId(), commandClass.getCommandClass().getLabel(), endpointId);
		return node.encapsulate(commandClass.getValueMessage(), commandClass, endpointId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleEvent(ZWaveCommandClassValueEvent event, Item item, Map<String,String> arguments) {
		ZWaveStateConverter<?,?> converter = this.getStateConverter(item, event.getValue());
		
		if (converter == null) {
			logger.warn("NODE {}: No converter found for item = {}, node = {} endpoint = {}, ignoring event.", event.getNodeId(), item.getName(), event.getEndpoint());
			return;
		}
		
		int bit = Integer.parseInt(arguments.get("bit"));
		ZWaveIndicatorCommandClassChangeEvent e = (ZWaveIndicatorCommandClassChangeEvent) event;
		ArrayList<Integer> bitChanges = e.changes();
		
		// Check if any of the "changed" or pressed buttons match the desired button
		for(Integer changedBit : bitChanges) {
			if( bit == changedBit) { 
				if( e.isBitOn(bit) ) {
					// Turn ON
					this.getEventPublisher().postUpdate(item.getName(),converter.convertFromValueToState(0xFF));
				}
				else {
					// Turn OFF
					this.getEventPublisher().postUpdate(item.getName(),converter.convertFromValueToState(0x00));
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void receiveCommand(Item item, Command command, ZWaveNode node,
			ZWaveIndicatorCommandClass commandClass, int endpointId, Map<String,String> arguments) {
		ZWaveCommandConverter<?,?> converter = this.getCommandConverter(command.getClass());
		
		if (converter == null) {
			logger.warn("NODE {}: No converter found for item = {}, endpoint = {}, ignoring command.", node.getNodeId(), item.getName(), endpointId);
			return;
		}

		int bit = Integer.parseInt(arguments.get("bit"));
		
		if( !(command instanceof OnOffType) )  {
			return;
		}
			
		int newIndicator;
		if (command == OnOffType.ON) {
			newIndicator = commandClass.setBitOn(bit);
		}
		else {
			newIndicator = commandClass.setBitOff(bit);
		}
		
		SerialMessage serialMessage = node.encapsulate(commandClass.setValueMessage(newIndicator), commandClass, endpointId);
		
		if (serialMessage == null) {
			logger.warn("NODE {}: Generating message failed for command class = {}, endpoint = {}", node.getNodeId(), commandClass.getCommandClass().getLabel(), endpointId);
			return;
		}
		
		this.getController().sendData(serialMessage);

		if (command instanceof State) {
			this.getEventPublisher().postUpdate(item.getName(), (State)command);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	int getRefreshInterval() {
		return REFRESH_INTERVAL;
	}
}
