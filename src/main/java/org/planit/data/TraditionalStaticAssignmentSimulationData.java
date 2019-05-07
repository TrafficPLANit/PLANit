package org.planit.data;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
     * Store the mode specific data required during assignment
     */
    private final Map<Mode,ModeData> modeSpecificData = new TreeMap<Mode,ModeData>(); // specific to tsa
    
 /**
  * Constructor
  */
    public TraditionalStaticAssignmentSimulationData() {
        modalNetworkSegmentFlows = new HashMap<Mode, double[]>();
    }
    
 /**
  * Reset modal network segment flows by cloning empty array
  * 
  * @param mode    the mode whose flows are to be reset
  */
    public void resetModalNetworkSegmentFlows(Mode mode) {
        setModalNetworkSegmentFlows(mode, (double[]) emptySegmentArray.clone()); 
   }
    
/**
 * Return an empty segment array
 * 
 * @return        empty segment array
 */
   public double[] getEmptySegmentArray() {
      return emptySegmentArray;
   }

  public void setEmptySegmentArray(double[] emptySegmentArray) {
     this.emptySegmentArray = emptySegmentArray;
 }

/**
 * Get the flows for a specified mode
 * 
 * @param mode        the specified mode
 * @return                  array of flows for current mode
 */
  public double[] getModalNetworkSegmentFlows(Mode mode) {
      return modalNetworkSegmentFlows.get(mode);
    }

 /**
  * Set the flows for a specifed mode
  * 
  * @param mode                                                the specified mode
  * @param modalNetworkSegmentFlows         array of flows for the specified mode
  */
    public void setModalNetworkSegmentFlows(Mode mode, double[] modalNetworkSegmentFlows) {
      this.modalNetworkSegmentFlows.put(mode, modalNetworkSegmentFlows);
    }
  
  public Map<Mode, ModeData> getModeSpecificData() {
    return modeSpecificData;
  }

/**
 * Calculate the total flows in the network over all modes
 * 
 * @return       the total flows in the network
 */
  public double[] getTotalNetworkSegmentFlows() {
      double [] totalNetworkSegmentFlows = (double[]) emptySegmentArray.clone();
      int size = totalNetworkSegmentFlows.length;
      for (int i=0; i<size; i++) {
          for (Mode mode : modalNetworkSegmentFlows.keySet()) {
              totalNetworkSegmentFlows[i] += modalNetworkSegmentFlows.get(mode)[i];
          }
      }
      return totalNetworkSegmentFlows;
    }

}
