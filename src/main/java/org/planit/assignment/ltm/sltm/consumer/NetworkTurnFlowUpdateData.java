package org.planit.assignment.ltm.sltm.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.assignment.ltm.sltm.loading.NetworkLoadingFactorData;
import org.planit.assignment.ltm.sltm.loading.SendingFlowData;
import org.planit.assignment.ltm.sltm.loading.SplittingRateData;
import org.planit.utils.misc.HashUtils;

/**
 * Base class to aid updating of the network turn flows during loading. Derived classes can apply a path or bush based approach to this update for example. This class stores the
 * current network state information required to perform the update regardless of the chosen assignment strategy (bush, path).
 * <p>
 * Depending on the applied solution scheme a slightly different approach is to be taken with this update where:
 * <p>
 * POINT QUEUE BASIC: Also update the network sending flow. Only during basic point queue solution scheme, sending flows are NOT locally updated in the sending flow update step.
 * Therefore sending flows of most links are not updated during the sending flow update because it only updates the sending flows of outgoing links of potentially blocking nodes.
 * When an incoming link of any node is not also an outgoing link of another potentially blocking node its sending flow remains the same even if it actually changes due to further
 * upstream changes in restrictions. In this approach this is taken care of by making sure the sending flows are updated during (this) loading on the path level. Hence, we must
 * update sending flows here.
 * <p>
 * ANY OTHER SOLUTION APPROACH: Here we update all used nodes and sending flows are updated iteratively and locally propagated without the need of the loading in the sending flow
 * update. Therefore, there is no need to update the sending flows. On the other hand we now update the turn flows on all used nodes rather than only the potentially blocking ones.
 * 
 * 
 * @author markr
 *
 */
public class NetworkTurnFlowUpdateData extends NetworkFlowUpdateData {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(NetworkTurnFlowUpdateData.class.getCanonicalName());

  /**
   * Splitting rate data to use
   */
  protected final SplittingRateData splittingRateData;

  /**
   * Flag indicating if we are tracking all (used) node turn flows or not
   */
  protected final boolean trackAllNodeTurnFlows;

  /**
   * The output of this update that can be collected after execution
   */
  protected final Map<Integer, Double> acceptedTurnFlows;

  /**
   * create hash code used for turn flow map
   * 
   * @param fromId               to use
   * @param currentEdgeSegmentId to use
   * @return created hash
   */
  protected static int createTurnHashCode(final int fromId, final int currentEdgeSegmentId) {
    return HashUtils.createCombinedHashCode(fromId, currentEdgeSegmentId);
  }

  /**
   * add to accepted turn flows
   * 
   * @param turnHashCode  to use
   * @param flowToAddPcuH to add
   */
  protected void addToAcceptedTurnFlows(int turnHashCode, double flowToAddPcuH) {
    acceptedTurnFlows.put(turnHashCode, acceptedTurnFlows.getOrDefault(turnHashCode, 0.0) + flowToAddPcuH);
  }

  /**
   * constructor
   * 
   * @param trackAllNodeTurnFlows    to apply
   * @param sendingFlowData          to use
   * @param splittingRateData        to use
   * @param networkLoadingFactorData to use
   */
  public NetworkTurnFlowUpdateData(final boolean trackAllNodeTurnFlows, SendingFlowData sendingFlowData, final SplittingRateData splittingRateData,
      NetworkLoadingFactorData networkLoadingFactorData) {
    super(sendingFlowData, networkLoadingFactorData);
    this.acceptedTurnFlows = new HashMap<Integer, Double>();
    this.splittingRateData = splittingRateData;

    /* see class description on why we use these flags */
    this.trackAllNodeTurnFlows = trackAllNodeTurnFlows;
  }

  /**
   * constructor where sending flows are not to be updated
   * 
   * @param splittingRateData        to use
   * @param solutionScheme           to apply
   * @param networkLoadingFactorData to use
   */
  public NetworkTurnFlowUpdateData(final boolean trackAllNodeTurnFlows, final SplittingRateData splittingRateData, NetworkLoadingFactorData networkLoadingFactorData) {
    super(networkLoadingFactorData);
    this.acceptedTurnFlows = new HashMap<Integer, Double>();
    this.splittingRateData = splittingRateData;

    /* see class description on why we use these flags */
    this.trackAllNodeTurnFlows = trackAllNodeTurnFlows;
  }

  /**
   * Access to the result, the accepted turn flows, where key comprises a combined hash of entry and exit edge segment ids and value is the accepted turn flow v_ab
   * 
   * @return accepted turn flows
   */
  public Map<Integer, Double> getAcceptedTurnFlows() {
    return this.acceptedTurnFlows;
  }
}
