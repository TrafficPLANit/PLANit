package org.planit.data;

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
  public double[] currentNetworkSegmentFlows = null;

  /**
   * Flows for the next iteration
   */
  public double[] nextNetworkSegmentFlows = null;

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

}
