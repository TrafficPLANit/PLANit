package org.planit.cost.virtual;

import org.planit.cost.Cost;
import org.planit.event.RequestAccesseeEvent;
import org.planit.network.virtual.ConnectoidSegment;
import org.planit.trafficassignment.TrafficAssignmentComponent;

public abstract class VirtualCost extends TrafficAssignmentComponent<VirtualCost> implements Cost<ConnectoidSegment> {

	public VirtualCost() {
		super();
	}
	/** Indicate if cost object requires an interaction to be able to perform its cost computation
	 * @return
	 */
	public boolean requiresInteractor() {
		return false;
	}
	
	public void performInteraction() {
		// only to be implemented when interaction is required		
	}	
	
	/** Method to create the interactor event request
	 *  
	 * @return requestedInteractorEvent, null by default
	 */
	public RequestAccesseeEvent createInteractorRequest() {
		return null;
	}
}
