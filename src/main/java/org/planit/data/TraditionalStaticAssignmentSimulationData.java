package org.planit.data;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.planit.network.physical.LinkSegment;
import org.planit.odmatrix.skim.ODSkimMatrix;
import org.planit.userclass.Mode;

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
    
    private Map<Mode, ODSkimMatrix> modalSkimMatrixMap;
    
    /**
     * Constructor
     */
    public TraditionalStaticAssignmentSimulationData() {
        modalNetworkSegmentFlows = new HashMap<Mode, double[]>();
        modeSpecificData = new TreeMap<Mode, ModeData>();
        modalNetworkSegmentCostsMap = new HashMap<Mode, double[]>();
        modalSkimMatrixMap = new HashMap<Mode, ODSkimMatrix>();
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
        return modalNetworkSegmentFlows.values().stream()
                .collect((Collectors.summingDouble(flows -> flows[(int) linkSegment.getId()])));
    }

    /**
     * Reset modal network segment flows by cloning empty array
     * 
     * @param mode
     *            the mode whose flows are to be reset
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
     * @param mode
     *            the specified mode
     * @param modalNetworkSegmentFlows
     *            array of flows for the specified mode
     */
    public void setModalNetworkSegmentFlows(Mode mode, double[] modalNetworkSegmentFlows) {
        this.modalNetworkSegmentFlows.put(mode, modalNetworkSegmentFlows);
    }
    
    public void setModalNetworkSegmentCosts(Mode mode, double[] modalNetworkSegmentCosts) {
    	modalNetworkSegmentCostsMap.put(mode, modalNetworkSegmentCosts );
    }
    
    public double[] getModalNetworkSegmentCosts(Mode mode) {
        return modalNetworkSegmentCostsMap.get(mode);
    }
    
    public void setSkimMatrixValue(Mode mode, long originZone, long destinationZone, double tripCost) {
    	modalSkimMatrixMap.get(mode).setValue(originZone, destinationZone, tripCost);
    }

    public void resetSkimMatrix(Mode mode, int numberOfTravelAnalysisZones) {
        ODSkimMatrix odSkimMatrix = new ODSkimMatrix(numberOfTravelAnalysisZones);
        modalSkimMatrixMap.put(mode, odSkimMatrix);
    }
    
    public ODSkimMatrix getODSkimMatrix(Mode mode) {
    	return modalSkimMatrixMap.get(mode);
    }
}

