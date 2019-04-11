package org.planit.event;

import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.transport.TransportNetwork;
import org.planit.zoning.Zoning;

/**
 * Listener which is invoked whenever a project component is created. To be used to populate the
 * project components from some data source
 * 
 * @author markr
 *
 */
public interface InputBuilder extends EventListener {
	
	
/** 
 * Process event and call the right onX method
 * @param event			event to be processed
 * @see org.planit.event.EventListener#process(org.planit.event.Event)
 */
	@Override
	default public void process(Event event)  throws PlanItException {
		try {
			if (event instanceof CreatedProjectComponentEvent<?>) {
				onCreateProjectComponent((CreatedProjectComponentEvent<?>) event);
			}
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}
	
/** 
 * Whenever a project component is created this method will be invoked
 * 
 * @param event 			event containing the created (and empty) project component
 */
	public void onCreateProjectComponent(CreatedProjectComponentEvent<?> event) throws PlanItException;

/**
 * Creates the integrated TransportNetwork object
 * 
 * This method integrates the physical and virtual networks.  
 * 
 * @param physicalNetwork				the physical network object
 * @param zoning								the zoning object (contain the virtual network)
 * @return											the integrated transport network
 * @throws PlanItException
 */
	public TransportNetwork buildTransportNetwork(PhysicalNetwork physicalNetwork, Zoning zoning) throws PlanItException;
	
}
