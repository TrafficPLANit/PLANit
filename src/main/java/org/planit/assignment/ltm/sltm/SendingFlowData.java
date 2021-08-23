package org.planit.assignment.ltm.sltm;

import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegments;

/**
 * POJO to store the sLTM variables used for sending flow updates (Step 2) in network loading
 * 
 * @author markr
 *
 */
public class SendingFlowData extends LinkSegmentData {

  /**
   * Sending flows for all link segments by internal id (current and next)
   */
  private double[][] sendingFlowsPcuH = null;

  /**
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public SendingFlowData(double[] emptySegmentArray) {
    super(emptySegmentArray);
    sendingFlowsPcuH = new double[2][emptySegmentArray.length];
    resetCurrentSendingFlows();
    resetNextSendingFlows();
  }

  /**
   * Reset the segment flows for the coming iteration
   */
  public void resetNextSendingFlows() {
    sendingFlowsPcuH[1] = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current network segment flows
   */
  public void resetCurrentSendingFlows() {
    sendingFlowsPcuH[0] = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * collect next sending flows
   * 
   * @return next sending flows
   */
  public double[] getNextSendingFlows() {
    return sendingFlowsPcuH[1];
  }

  /**
   * collect current sending flows
   * 
   * @return current sending flows
   */
  public double[] getCurrentSendingFlows() {
    return sendingFlowsPcuH[0];
  }

  /**
   * Reduce all current link segments' sending flows to capacity in case they exceed it
   * 
   * @param linkSegments to use
   */
  public void limitCurrentSendingFlowsToCapacity(MacroscopicLinkSegments linkSegments) {
    limitFlowsToCapacity(sendingFlowsPcuH[0], linkSegments);
  }

  /**
   * replace current sending flows by the next sending flows
   */
  public void swapCurrentAndNextSendingFlows() {
    swap(0, 1, sendingFlowsPcuH);
  }

}
