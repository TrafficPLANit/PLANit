package org.goplanit.assignment.ltm.sltm.loading;

import org.goplanit.assignment.ltm.sltm.common.LinkSegmentData;

/**
 * POJO to store the sLTM variables used for unconstrained flow updates in network loading when required
 * 
 * @author markr
 *
 */
public class UnconstrainedFlowData extends LinkSegmentData {

  /**
   * Unconstrained flows for all link segments by internal id
   */
  private double[] unconstrainedFlowsPcuH = null;

  /**
   * Constructor
   *
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public UnconstrainedFlowData(double[] emptySegmentArray) {
    super(emptySegmentArray);
    unconstrainedFlowsPcuH = new double[emptySegmentArray.length];
    reset();
  }

  /**
   * Reset all unconstrained flows
   */
  public void reset() {
    unconstrainedFlowsPcuH = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * collect current unconstrained flows
   * 
   * @return current unconstrained flows
   */
  public double[] getUnconstrainedFlows() {
    return unconstrainedFlowsPcuH;
  }

}
