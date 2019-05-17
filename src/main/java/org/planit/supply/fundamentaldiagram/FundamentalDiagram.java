package org.planit.supply.fundamentaldiagram;

import java.util.logging.Logger;
import org.planit.trafficassignment.TrafficAssignmentComponent;

/**
 * Fundamental diagram traffic component
 * 
 * @author markr
 *
 */
public abstract class FundamentalDiagram extends TrafficAssignmentComponent<FundamentalDiagram> {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(FundamentalDiagram.class.getName());

    /**
     * Base constructor
     */
    public FundamentalDiagram() {
        super();
    }

}
