package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.loading.InflowOutflowData;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingFactorData;
import org.goplanit.assignment.ltm.sltm.loading.SendingFlowData;

/**
 * Base class to aid updating of the network link flows during loading. Derived classes can apply a path or bush based approach to this update for example. This class stores the
 * current network state information required to perform the update regardless of the chosen assignment strategy (bush, path).
 * <p>
 * Sending flows are allowed to be null as in certain derived classes they might not be required
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
   * The outflows to update if flagged as such
   */
  protected final double[] outFlows;

  /**
   * Constructor to update sending flows during flow update
   * 
   * @param sendingFlowData          to use
   * @param networkLoadingFactorData to use
   */
  public NetworkFlowUpdateData(final SendingFlowData sendingFlowData, final InflowOutflowData inflowOutflowdata, NetworkLoadingFactorData networkLoadingFactorData) {
    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
    this.sendingFlows = sendingFlowData.getCurrentSendingFlows();
    this.outFlows = inflowOutflowdata.getOutflows();
  }

  /**
   * Constructor to update sending flows during flow update
   * 
   * @param sendingFlowData          to use
   * @param networkLoadingFactorData to use
   */
  public NetworkFlowUpdateData(final SendingFlowData sendingFlowData, NetworkLoadingFactorData networkLoadingFactorData) {
    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
    this.sendingFlows = sendingFlowData.getCurrentSendingFlows();
    this.outFlows = null;
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
  }

  public boolean isSendingflowsUpdate() {
    return sendingFlows != null;
  }

  public boolean isOutflowsUpdate() {
    return outFlows != null;
  }
}
