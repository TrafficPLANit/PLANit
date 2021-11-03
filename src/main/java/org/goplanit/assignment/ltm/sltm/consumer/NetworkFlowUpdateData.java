package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

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
  protected double[] flowAcceptanceFactors;

  /**
   * The sending flows to update in case the applied solution scheme is the POINT_QUEUE_BASIC scheme
   */
  protected final double[] sendingFlows;

  /** flag indicating if link sending flows are to be updated */
  protected boolean updateLinkSendingFlows;

  /**
   * constructor
   * 
   * @param sendingFlowData          to use
   * @param networkLoadingFactorData to use
   */
  public NetworkFlowUpdateData(final SendingFlowData sendingFlowData, NetworkLoadingFactorData networkLoadingFactorData) {
    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
    this.sendingFlows = sendingFlowData.getCurrentSendingFlows();
    this.updateLinkSendingFlows = true;
  }

  /**
   * constructor, special case where link sending flows are not to be updated
   * 
   * @param networkLoadingFactorData to use
   */
  public NetworkFlowUpdateData(NetworkLoadingFactorData networkLoadingFactorData) {
    this.flowAcceptanceFactors = networkLoadingFactorData.getCurrentFlowAcceptanceFactors();
    this.sendingFlows = null;
    this.updateLinkSendingFlows = false;
  }

}
