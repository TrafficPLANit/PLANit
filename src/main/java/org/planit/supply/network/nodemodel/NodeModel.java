package org.planit.supply.network.nodemodel;

import java.io.Serializable;

import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Node model traffic component
 * 
 * @author markr
 *
 */
public abstract class NodeModel extends TrafficAssignmentComponent<NodeModel>implements Serializable {
	
	/** generated UID */
	private static final long serialVersionUID = -6966680588075724261L;

	/**
	 * Base constructor
	 */
	public NodeModel() {
		super();
	}

}
