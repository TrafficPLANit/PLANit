package org.planit.supply.network.nodemodel;

import java.util.logging.Logger;
import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Node model traffic component
 * 
 * @author gman6028
 *
 */
public abstract class NodeModel extends TrafficAssignmentComponent<NodeModel> {
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(NodeModel.class.getName());
        

	public NodeModel() {
		super();
	}

}
