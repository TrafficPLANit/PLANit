package org.planit.supply.fundamentaldiagram;

import org.planit.exceptions.PlanItException;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * Newell fundamental diagram traffic component
 *
 * @author markr
 *
 */
public class NewellFundamentalDiagram extends FundamentalDiagram {


	/**generated UID */
	private static final long serialVersionUID = -3166623064510413929L;

	// register to be eligible in PLANit
    static {
        try {
            TrafficAssignmentComponentFactory.registerTrafficAssignmentComponentType(NewellFundamentalDiagram.class);
        } catch (final PlanItException e) {
            e.printStackTrace();
        }
    }

    /**
     * Base constructor
     */
    public NewellFundamentalDiagram() {
        super();
    }

}
