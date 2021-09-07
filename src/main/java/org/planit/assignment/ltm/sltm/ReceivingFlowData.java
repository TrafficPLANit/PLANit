package org.planit.assignment.ltm.sltm;

import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;

/**
 * POJO to store the sLTM variables used for receiving flow updates (Step 4) in network loading
 * 
 * @author markr
 *
 */
public class ReceivingFlowData extends LinkSegmentData {

  /**
   * Receiving flows for all link segments by internal id (current and next)
   */
  private double[][] receivingFlowsPcuH = null;

  /**
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public ReceivingFlowData(double[] emptySegmentArray) {
    super(emptySegmentArray);
    receivingFlowsPcuH = new double[2][emptySegmentArray.length];
    resetCurrentReceivingFlows();
    resetNextReceivingFlows();
  }

  /**
   * Reset the segment flows for the coming iteration
   */
  public void resetNextReceivingFlows() {
    receivingFlowsPcuH[1] = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * Reset current network segment flows
   */
  public void resetCurrentReceivingFlows() {
    receivingFlowsPcuH[0] = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * collect next receiving flows
   * 
   * @return next receiving flows
   */
  public double[] getNextReceivingFlows() {
    return receivingFlowsPcuH[1];
  }

  /**
   * collect current receiving flows
   * 
   * @return current receiving flows
   */
  public double[] getCurrentReceivingFlows() {
    return receivingFlowsPcuH[0];
  }

  /**
   * Reduce all provided link segments' receiving flows to capacity in case they exceed it
   * 
   * @param linkSegments to use
   */
  public void limitNextReceivingFlowsToCapacity(MacroscopicLinkSegments linkSegments) {
    limitFlowsToCapacity(receivingFlowsPcuH[1], linkSegments);
  }

  /**
   * Swap the current and next receiving flows
   */
  public void swapCurrentAndNextReceivingFlows() {
    swap(0, 1, this.receivingFlowsPcuH);
  }

  /**
   * Reset to initial state
   */
  public void reset() {
    resetCurrentReceivingFlows();
    resetNextReceivingFlows();
  }

}
