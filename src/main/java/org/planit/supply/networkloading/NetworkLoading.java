package org.planit.supply.networkloading;

import java.io.Serializable;

import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Network loading traffic component
 * 
 * @author markr
 *
 */
public abstract class NetworkLoading extends TrafficAssignmentComponent<NetworkLoading> implements Serializable   {
        
	/** generated UID */
	private static final long serialVersionUID = 6213911562665516698L;

	/**
	 * Base contructor
	 */
	public NetworkLoading() {
		super();
	}
		
}
