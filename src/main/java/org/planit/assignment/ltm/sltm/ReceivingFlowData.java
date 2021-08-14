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
   * Receiving flows for all link segments by internal id
   */
  private double[] currentReceivingFlowsPcuH = null;

  /**
   * Newly computed flows for all link segments by internal id
   */
  private double[] nextReceivingFlowsPcuH = null;

  /**
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public ReceivingFlowData(double[] emptySegmentArray) {
    super(emptySegmentArray);
    resetCurrentReceivingFlows();
    resetNextReceivingFlows();
  }

  /**
   * Reset the segment flows for the coming iteration
   */
  public void resetNextReceivingFlows() {
    nextReceivingFlowsPcuH = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current network segment flows
   */
  public void resetCurrentReceivingFlows() {
    currentReceivingFlowsPcuH = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * collect next receiving flows
   * 
   * @return next receiving flows
   */
  public double[] getNextReceivingFlows() {
    return nextReceivingFlowsPcuH;
  }

  /**
   * collect current receiving flows
   * 
   * @return current receiving flows
   */
  public double[] getCurrentReceivingFlows() {
    return currentReceivingFlowsPcuH;
  }

  /**
   * add passed in flow to next segment flows for given id
   * 
   * @param edgeSegmentId  segment to add flow to
   * @param flowPcuPerHour to add
   */
  public void addToNextReceivingFlows(long edgeSegmentId, double flowPcuPerHour) {
    nextReceivingFlowsPcuH[(int) edgeSegmentId] += flowPcuPerHour;
  }

  /**
   * Reduce all provided link segments' receiving flows to capacity in case they exceed it
   * 
   * @param linkSegments to use
   */
  public void limitNextReceivingFlowsToCapacity(MacroscopicLinkSegments linkSegments) {
    limitFlowsToCapacity(nextReceivingFlowsPcuH, linkSegments);
  }

}
