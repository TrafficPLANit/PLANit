package org.planit.output.adapter;

import java.util.Set;

import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.output.enums.OutputType;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.userclass.Mode;

public class TraditionalStaticAssignmentODOutputTypeAdapter extends OutputTypeAdapterImpl implements ODOutputTypeAdapter {

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
	 * Returns a Set of OD skim output types which have been activated
	 * 
	 * @return Set of OD skim output types which have been activated
	 */
    public Set<ODSkimOutputType> getActiveSkimOutputTypes() {
		TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		return traditionalStaticAssignmentSimulationData.getActiveSkimOutputTypes();
    }
    
    /**
     * Retrieve an OD skim matrix for a specified OD skim output type and mode
     * 
     * @param odSkimOutputType the specified OD skim output type
     * @param mode the specified mode
     * @return the OD skim matrix
     */
    public  ODSkimMatrix getODSkimMatrix(ODSkimOutputType odSkimOutputType, Mode mode) {
		TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) trafficAssignment.getSimulationData();
		return traditionalStaticAssignmentSimulationData.getODSkimMatrix(odSkimOutputType, mode);
    }
      
}