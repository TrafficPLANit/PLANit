package org.planit.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.planit.network.physical.LinkSegment;
import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.output.configuration.OriginDestinationOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.output.enums.OutputType;
import org.planit.userclass.Mode;
import org.planit.zoning.Zone;
import org.planit.zoning.Zoning;

/**
 * Simulation data which are specific to Traditional Static Assignment
 * 
 * @author gman6028
 *
 */
public class TraditionalStaticAssignmentSimulationData extends SimulationData {

    /**
     * empty array to quickly initialize new arrays when needed
     */
    private double[] emptySegmentArray = null; // specific to tsa

    /**
     * segment flows for each mode
     */
    private Map<Mode, double[]> modalNetworkSegmentFlows = null;

    /**
     * Stores the mode specific data required during assignment
     */
    private final Map<Mode, ModeData> modeSpecificData; // specific to tsa

    /**
     * Stores the array of link segment costs for each mode 
     */
    private Map<Mode, double[]> modalNetworkSegmentCostsMap;
    
   /**
    * Stores a skim matrix for each mode and skim output type(updated cell by cell for each iteration)
    */
    private Map<ODSkimOutputType, Map<Mode, ODSkimMatrix>> modalSkimMatrixMap;
    
    /**
     * Constructor
     * 
     * @param outputConfiguration the OutputConfiguration
     */
    public TraditionalStaticAssignmentSimulationData(OutputConfiguration outputConfiguration) {
        modalNetworkSegmentFlows = new HashMap<Mode, double[]>();
        modeSpecificData = new TreeMap<Mode, ModeData>();
        modalNetworkSegmentCostsMap = new HashMap<Mode, double[]>();
        modalSkimMatrixMap = new HashMap<ODSkimOutputType, Map<Mode, ODSkimMatrix>>();
		OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration = (OriginDestinationOutputTypeConfiguration) outputConfiguration.getOutputTypeConfiguration(OutputType.OD);
		Set<ODSkimOutputType> activeOdSkimOutputTypes = originDestinationOutputTypeConfiguration.getActiveOdSkimOutputTypes();
        for (ODSkimOutputType odSkimOutputType : activeOdSkimOutputTypes) {
        	modalSkimMatrixMap.put(odSkimOutputType, new HashMap<Mode, ODSkimMatrix>());
        }
   }

    /**
     * Return an empty segment array
     * 
     * @return empty segment array
     */
    public double[] getEmptySegmentArray() {
        return emptySegmentArray;
    }

    /**
     * Get the flows for a specified mode
     * 
     * @param mode
     *            the specified mode
     * @return array of flows for current mode
     */
    public double[] getModalNetworkSegmentFlows(Mode mode) {
        return modalNetworkSegmentFlows.get(mode);
    }

    /**
     * Collect the data per mode for all modes
     * 
     * @return mode specific data map
     */
    public Map<Mode, ModeData> getModeSpecificData() {
        return modeSpecificData;
    }

    /**
     * Return the total flow through a link segment across all modes
     * 
     * @param linkSegment
     *            the specified link segment
     * @return the total flow through this link segment
     */
    public double getTotalNetworkSegmentFlow(LinkSegment linkSegment) {
        return modalNetworkSegmentFlows.values().stream().collect((Collectors.summingDouble(flows -> flows[(int) linkSegment.getId()])));
    }

    /**
     * Reset modal network segment flows by cloning empty array
     * 
     * @param mode the mode whose flows are to be reset
     */
    public void resetModalNetworkSegmentFlows(Mode mode) {
        setModalNetworkSegmentFlows(mode, (double[]) emptySegmentArray.clone());
    }

    public void setEmptySegmentArray(double[] emptySegmentArray) {
        this.emptySegmentArray = emptySegmentArray;
    }

    /**
     * Set the flows for a specified mode
     * 
     * @param mode  the specified mode
     * @param modalNetworkSegmentFlows array of flows for the specified mode
     */
    public void setModalNetworkSegmentFlows(Mode mode, double[] modalNetworkSegmentFlows) {
        this.modalNetworkSegmentFlows.put(mode, modalNetworkSegmentFlows);
    }
    
    /**
     * Set the costs for a specified mode
     * 
     * @param mode the specified mode
     * @param modalNetworkSegmentCosts array of costs for the specified mode
     */
    public void setModalNetworkSegmentCosts(Mode mode, double[] modalNetworkSegmentCosts) {
    	modalNetworkSegmentCostsMap.put(mode, modalNetworkSegmentCosts );
    }
    
    /**
     * Retrieve the costs for a specified mode
     * 
     * @param mode the specified mode
     * @return array of costs for the specified mode
     */
    public double[] getModalNetworkSegmentCosts(Mode mode) {
        return modalNetworkSegmentCostsMap.get(mode);
    }
    
    /**
     * Set a cost value in the skim matrix for a specified mode, origin and destination for all activated skim output types
     * 
     * @param mode the specified mode
     * @param originZone Zone object defining the origin
     * @param destinationZone Zone object defining the destination
     * @param tripCost the trip cost value to be stored in the skim matrix cell
     */
    public void setSkimMatrixValue(Mode mode, Zone originZone, Zone destinationZone, double tripCost) {
     	modalSkimMatrixMap.keySet().forEach(odSkimOutputType -> {modalSkimMatrixMap.get(odSkimOutputType).get(mode).setValue(originZone, destinationZone, tripCost);});	
    }

    /**
     * Reset the skim matrix to all zeroes for a specified mode for all activated skim output types
     * 
     * @param odSkimOutputType the specified Skim Output type
     * @param mode the specified mode
     * @param zones Zones object containing all the origin and destination zones
     * @param odSkimOutputType the specified skim output type
     */
    public void resetSkimMatrix(Mode mode, Zoning.Zones zones) {
    	modalSkimMatrixMap.keySet().forEach(odSkimOutputType -> {modalSkimMatrixMap.get(odSkimOutputType).put(mode, new ODSkimMatrix(zones, odSkimOutputType));});	
    }
    
    /**
     * Retrieve the skim matrix for a specified mode and skim output type
     * 
     * @param odSkimOutputType the specified Skim Output type
     * @param mode the specified mode
     * @return the skim matrix for the specified mode
     */
    public ODSkimMatrix getODSkimMatrix(ODSkimOutputType odSkimOutputType, Mode mode) {
       	if (modalSkimMatrixMap.containsKey(odSkimOutputType)) {
       		return modalSkimMatrixMap.get(odSkimOutputType).get(mode);
       	}
       	return null;
    }
    
    /**
     * Returns a Set of activated OD skim output types
     * 
     * @return Set of activated OD skim output types
     */
    public Set<ODSkimOutputType> getActiveSkimOutputTypes() {
    	return modalSkimMatrixMap.keySet();
    }
    
}