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
   * Sending flows for all link segments by internal id
   */
  private double[] currentSendingFlowsPcuH = null;

  /**
   * Newly computed flows for all link segments by internal id
   */
  private double[] nextSendingFlowsPcuH = null;

  /**
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public SendingFlowData(double[] emptySegmentArray) {
    super(emptySegmentArray);
    resetCurrentSendingFlows();
    resetNextSendingFlows();
  }

  /**
   * Reset the segment flows for the coming iteration
   */
  public void resetNextSendingFlows() {
    nextSendingFlowsPcuH = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current network segment flows
   */
  public void resetCurrentSendingFlows() {
    currentSendingFlowsPcuH = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * collect next sending flows
   * 
   * @return next sending flows
   */
  public double[] getNextSendingFlows() {
    return nextSendingFlowsPcuH;
  }

  /**
   * collect current sending flows
   * 
   * @return current sending flows
   */
  public double[] getCurrentSendingFlows() {
    return currentSendingFlowsPcuH;
  }

  /**
   * add passed in flow to next segment flows for given id
   * 
   * @param edgeSegmentId  segment to add flow to
   * @param flowPcuPerHour to add
   */
  public void addToNextSendingFlows(long edgeSegmentId, double flowPcuPerHour) {
    nextSendingFlowsPcuH[(int) edgeSegmentId] += flowPcuPerHour;
  }

  /**
   * Reduce all current link segments' sending flows to capacity in case they exceed it
   * 
   * @param linkSegments to use
   */
  public void limitCurrentSendingFlowsToCapacity(MacroscopicLinkSegments linkSegments) {
    limitFlowsToCapacity(currentSendingFlowsPcuH, linkSegments);
  }

  /**
   * replace current sending flows by the next sending flows
   */
  public void setNextSendingFlowsAsCurrent() {
    this.currentSendingFlowsPcuH = this.nextSendingFlowsPcuH;
  }

}
