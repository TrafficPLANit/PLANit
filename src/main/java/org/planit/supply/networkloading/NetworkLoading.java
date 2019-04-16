package org.planit.supply.networkloading;

import java.util.logging.Logger;
import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Network loading traffic component
 * 
 * @author gman6028
 *
 */
public abstract class NetworkLoading extends TrafficAssignmentComponent<NetworkLoading>   {
				
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(NetworkLoading.class.getName());
        
	public NetworkLoading() {
		super();
	}
		
}
