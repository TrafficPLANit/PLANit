package org.goplanit.assignment.traditionalstatic;

/**
 * Object to store the flows for each mode during the assignment iterations
 * 
 * @author gman6028
 *
 */
public class ModeData {

  /**
   * Empty array for quick memory based copying
   */
  private double[] emptySegmentArray;

  /**
   * Flows derived for the previous iteration
   */
  private double[] currentNetworkSegmentFlows = null;

  /**
   * Flows for the next iteration
   */
  private double[] nextNetworkSegmentFlows = null;

  /**
   * Constructor
   * 
   * @param emptySegmentArray
   *          empty array used to initialize data stores
   */
  public ModeData(double[] emptySegmentArray) {
    this.emptySegmentArray = emptySegmentArray;
    resetCurrentNetworkSegmentFlows();
    resetNextNetworkSegmentFlows();
  }

  /**
   * Reset the segment flows for the coming iteration
   */
  public void resetNextNetworkSegmentFlows() {
    nextNetworkSegmentFlows = emptySegmentArray.clone();
  }

  /**
   * Reset current network segment flows
   */
  public void resetCurrentNetworkSegmentFlows() {
    currentNetworkSegmentFlows = emptySegmentArray.clone();
  }
  
  /** collect next segment flows
   * @return next segment flows
   */
  public double[] getNextSegmentFlows() {
    return nextNetworkSegmentFlows;
  }
  
  /** collect current segment flows
   * @return current segment flows
   */
  public double[] getCurrentSegmentFlows() {
    return currentNetworkSegmentFlows;
  }

  /** set segment flows
   * 
   * @param segmentFlows to set as current
   */
  public void setCurrentSegmentFlows(double[] segmentFlows) {
    this.currentNetworkSegmentFlows = segmentFlows;    
  }

  /** add passed in flow to next segment flows for given id 
   * @param edgeSegmentId segment to add flow to
   * @param flow to add
   */
  public void addToNextSegmentFlows(long edgeSegmentId, double flow) {
    nextNetworkSegmentFlows[(int)edgeSegmentId] += flow;    
  }

}
