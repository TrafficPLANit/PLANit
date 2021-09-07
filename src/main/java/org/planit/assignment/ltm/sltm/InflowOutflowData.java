package org.planit.assignment.ltm.sltm;

/**
 * During sLTM loading we use temporary inflow and outflow rates resulting from node model updates. These flows are then used to determine the factors for the relevant algorithm
 * step upon convergence of that step.
 * 
 * @author markr
 *
 */
public class InflowOutflowData extends LinkSegmentData {

  /**
   * tracked inflow rates for relevant link segments by internal id
   */
  private double[] inflowsFlowsPcuH = null;

  /**
   * tracked outflow rates for relevant link segments by internal id
   */
  private double[] outflowsFlowsPcuH = null;

  /**
   * Constructor
   * 
   * @param emptySegmentArray empty array used to initialize data stores
   */
  public InflowOutflowData(double[] emptySegmentArray) {
    super(emptySegmentArray);
    resetInflows();
    resetOutflows();
  }

  /**
   * Reset the inflows
   */
  public void resetInflows() {
    inflowsFlowsPcuH = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * Reset the outflows
   */
  public void resetOutflows() {
    outflowsFlowsPcuH = this.createinitialStateLinkSegmentDoubleArray();
  }

  /**
   * access to the tracked inflows
   * 
   * @return inflows
   */
  public double[] getInflows() {
    return this.inflowsFlowsPcuH;
  }

  /**
   * access to the tracked inflows
   * 
   * @return inflows
   */
  public double[] getOutflows() {
    return this.outflowsFlowsPcuH;
  }

  /**
   * Reset to initial state
   */
  public void reset() {
    resetInflows();
    resetOutflows();
  }
}
