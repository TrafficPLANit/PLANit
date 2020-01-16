package org.planit.output.adapter;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

/**
 *Adapter providing access to the data of the TraditionalStaticAssignment class
 * relevant for origin-destination outputs without exposing the internals of the traffic
 * assignment class itself 
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticAssignmentODOutputTypeAdapter extends ODOutputTypeAdapterImpl {
	
	/**
	 * Constructor
	 * 
	 * @param outputType the output type for the current persistence
	 * @param trafficAssignment the traffic assignment used to provide the data
	 */
	public TraditionalStaticAssignmentODOutputTypeAdapter(OutputType outputType, TrafficAssignment trafficAssignment) {
		super(outputType, trafficAssignment);
	}

    /**
     * Retrieve an OD skim matrix for a specified OD skim output type and mode
     * 
     * @param odSkimOutputType the specified OD skim output type
     * @param mode the specified mode
     * @return the OD skim matrix
     */
    public ODSkimMatrix getODSkimMatrix(ODSkimSubOutputType odSkimOutputType, Mode mode) {
		TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		return traditionalStaticAssignmentSimulationData.getODSkimMatrix(odSkimOutputType, mode);
    }
      
}