package org.planit.assignment.ltm.sltm;

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
  private double[] currentSendingFlows = null;

  /**
   * Newly computed flows for all link segments by internal id
   */
  private double[] nextSendingFlows = null;

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
    nextSendingFlows = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * Reset current network segment flows
   */
  public void resetCurrentSendingFlows() {
    currentSendingFlows = this.createEmptyLinkSegmentDoubleArray();
  }

  /**
   * collect next sending flows
   * 
   * @return next sending flows
   */
  public double[] getNextSendingFlows() {
    return nextSendingFlows;
  }

  /**
   * collect current sending flows
   * 
   * @return current sending flows
   */
  public double[] getCurrentSendingFlows() {
    return currentSendingFlows;
  }

  /**
   * add passed in flow to next segment flows for given id
   * 
   * @param edgeSegmentId  segment to add flow to
   * @param flowPcuPerHour to add
   */
  public void addToNextSendingFlows(long edgeSegmentId, double flowPcuPerHour) {
    nextSendingFlows[(int) edgeSegmentId] += flowPcuPerHour;
  }

}
