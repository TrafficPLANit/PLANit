package org.planit.data;

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
     * network wide segment flows
     */
    private double[] totalNetworkSegmentFlows = null; // specific to tsa
    
    /**
     * Store the mode specific data required during assignment
     */
    private final Map<Mode,ModeData> modeSpecificData = new TreeMap<Mode,ModeData>(); // specific to tsa
    
 /**
  * reset total network segment flows by cloning empty array
  */
    public void resetTotalNetworkSegmentFlows() {
         setTotalNetworkSegmentFlows((double[]) emptySegmentArray.clone()); 
    }
    
   public double[] getEmptySegmentArray() {
      return emptySegmentArray;
   }

  public void setEmptySegmentArray(double[] emptySegmentArray) {
     this.emptySegmentArray = emptySegmentArray;
 }

  public double[] getTotalNetworkSegmentFlows() {
    return totalNetworkSegmentFlows;
  }

  public void setTotalNetworkSegmentFlows(double[] totalNetworkSegmentFlows) {
    this.totalNetworkSegmentFlows = totalNetworkSegmentFlows;
  }

  public Map<Mode, ModeData> getModeSpecificData() {
    return modeSpecificData;
  }

}
