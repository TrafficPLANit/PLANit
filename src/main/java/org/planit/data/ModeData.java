package org.planit.data;

/**
 * Object to store the flows for each mode during the assignment iterations
 * 
 * @author gman6028
 *
 */
public class ModeData {

/**
 * Flows derived for the previous iteration
 */
    public double[] currentNetworkSegmentFlows  = null;
    
 /**
  * Flows for the next iteration
  */
    public double[] nextNetworkSegmentFlows     = null;
    private double[] emptySegmentArray;
  
 /**
  * Constructor
  * 
  * @param emptySegmentArray    empty array used to initialize data stores
  */
    public ModeData(double[] emptySegmentArray){
        this.emptySegmentArray = emptySegmentArray;
        resetCurrentNetworkSegmentFlows();
        resetNextNetworkSegmentFlows();         
    }
    
    public void resetNextNetworkSegmentFlows(){
        nextNetworkSegmentFlows = emptySegmentArray.clone();
    }
    
    public void resetCurrentNetworkSegmentFlows() {
        currentNetworkSegmentFlows = emptySegmentArray.clone();
    }

}
