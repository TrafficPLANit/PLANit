package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.loading.InflowOutflowData;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingFactorData;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingSendingFlowData;
import org.goplanit.assignment.ltm.sltm.loading.UnconstrainedFlowData;

/**
 * Base class to help update the network link flows during loading. Derived classes can, for example, take a path or
 * bush based approach to this update. This class stores the current network state needed for the update, whatever
 * the assignment strategy (bush, path).
 * <p>
 * Sending flows may be null, as some derived classes might not need them
 * 
 * @author markr
 *
 */
public class NetworkFlowUpdateData {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(NetworkFlowUpdateData.class.getCanonicalName());

  /**
   * Flow acceptance factors to use
   */
  final protected double[] flowAcceptanceFactors;

  /**
   * The sending flows to update if flagged as such
   */
  protected final double[] sendingFlows;

  /**
   * The unconstrained flows (without considering any capacity restrictions)
   * to update if flagged as such
   */
  protected final double[] unconstrainedFlows;

  /**
   * The outflows to update if flagged as such
   */
  protected final double[] outFlows;

  /**
   * The inflows to update if flagged as such
   */
  protected final double[] inFlows;

  /**
   * Constructor to update ALL flows, inflows, , sending flows, outflows and unconstrained flows
   * during flow update
   *
   * @param networkLoadingSendingFlowData          to use
   * @param flowAcceptanceFactors to use
   * @param inflows to use
   * @param outflows to use
   * @param unconstrainedFlowData to use
   */
  public NetworkFlowUpdateData(
      final NetworkLoadingSendingFlowData networkLoadingSendingFlowData,
      final double[] flowAcceptanceFactors,
      final double[] inflows,
      final double[] outflows,
      final UnconstrainedFlowData unconstrainedFlowData) {
    this.flowAcceptanceFactors = flowAcceptanceFactors;
    this.sendingFlows = networkLoadingSendingFlowData.getCurrentSendingFlows();
    this.outFlows = outflows;
    this.inFlows = inflows;
    this.unconstrainedFlows = unconstrainedFlowData.getUnconstrainedFlows();
  }

  /**
   * Constructor to update sending flows, outflows and unconstrained flows during flow update
   *
   * @param networkLoadingSendingFlowData          to use
   * @param inflowOutflowData        to use
   * @param networkLoadingFactorData to use
   * @param unconstrainedFlowData to use
   */
  public NetworkFlowUpdateData(
          final NetworkLoadingSendingFlowData networkLoadingSendingFlowData,
          final InflowOutflowData inflowOutflowData,
          NetworkLoadingFactorData networkLoadingFactorData,
          final UnconstrainedFlowData unconstrainedFlowData) {
    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
    this.sendingFlows = networkLoadingSendingFlowData.getCurrentSendingFlows();
    this.outFlows = inflowOutflowData.getOutflows();
    this.inFlows = null;
    this.unconstrainedFlows = unconstrainedFlowData.getUnconstrainedFlows();
  }

  /**
   * Constructor to update sending flows and outflows during flow update
   * 
   * @param networkLoadingSendingFlowData          to use
   * @param inflowOutflowdata        to use
   * @param networkLoadingFactorData to use
   */
  public NetworkFlowUpdateData(
          final NetworkLoadingSendingFlowData networkLoadingSendingFlowData,
          final InflowOutflowData inflowOutflowdata,
          NetworkLoadingFactorData networkLoadingFactorData) {
    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
    this.sendingFlows = networkLoadingSendingFlowData.getCurrentSendingFlows();
    this.outFlows = inflowOutflowdata.getOutflows();
    this.inFlows = null;
    this.unconstrainedFlows = null;
  }

  /**
   * Constructor to update sending flows and unconstrained flows during flow update
   * 
   * @param networkLoadingSendingFlowData          to use
   * @param networkLoadingFactorData to use
   * @param unconstrainedFlowData    to use
   */
  public NetworkFlowUpdateData(
          final NetworkLoadingSendingFlowData networkLoadingSendingFlowData,
          NetworkLoadingFactorData networkLoadingFactorData,
          final UnconstrainedFlowData unconstrainedFlowData) {
    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
    this.sendingFlows = networkLoadingSendingFlowData.getCurrentSendingFlows();
    this.outFlows = null;
    this.inFlows = null;
    this.unconstrainedFlows = unconstrainedFlowData.getUnconstrainedFlows();
  }

  /**
   * Constructor to update sending flows during flow update
   *
   * @param networkLoadingSendingFlowData          to use
   * @param networkLoadingFactorData to use
   */
  public NetworkFlowUpdateData(
          final NetworkLoadingSendingFlowData networkLoadingSendingFlowData,
          NetworkLoadingFactorData networkLoadingFactorData) {
    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
    this.sendingFlows = networkLoadingSendingFlowData.getCurrentSendingFlows();
    this.outFlows = null;
    this.inFlows = null;
    this.unconstrainedFlows = null;
  }

  /**
   * Constructor, special case where link sending flows are not to be updated
   * 
   * @param networkLoadingFactorData to use
   */
  public NetworkFlowUpdateData(NetworkLoadingFactorData networkLoadingFactorData) {
    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
    this.sendingFlows = null;
    this.outFlows = null;
    this.inFlows = null;
    this.unconstrainedFlows = null;
  }

  public boolean isSendingFlowsUpdate() {
    return sendingFlows != null;
  }

  public boolean isOutflowsUpdate() {
    return outFlows != null;
  }

  public boolean isInflowsUpdate() {
    return inFlows != null;
  }

  public boolean isUnconstrainedFlowsUpdate() {
    return unconstrainedFlows != null;
  }

  public double[] getFlowAcceptanceFactors() {
    return flowAcceptanceFactors;
  }

  public double[] getSendingFlows() {
    return sendingFlows;
  }

  public double[] getUnconstrainedFlows() {
    return unconstrainedFlows;
  }

  public double[] getOutFlows() {
    return outFlows;
  }

  public double[] getInFlows() {
    return inFlows;
  }
}
