package org.planit.supply.network.nodemodel;

import org.planit.exceptions.PlanItException;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * Tampere node model traffic component
 *
 * @author markr
 *
 */
public class TampereNodeModel extends NodeModel {

	/** generated UID */
	private static final long serialVersionUID = 624108273657030487L;

	// register to be eligible in PLANit
    static {
        try {
            TrafficAssignmentComponentFactory.registerTrafficAssignmentComponentType(TampereNodeModel.class);
        } catch (final PlanItException e) {
            e.printStackTrace();
        }
    }

	/**
	 * Base Constructor
	 */
	public TampereNodeModel() {
		super();
	}

}
