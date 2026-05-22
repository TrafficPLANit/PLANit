package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingFactorData;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingSendingFlowData;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingSplittingRateData;
import org.goplanit.assignment.ltm.sltm.loading.UnconstrainedFlowData;
import org.goplanit.utils.network.layer.physical.Movement;

/**
 * Base class to aid updating of the network turn flows during loading. Derived classes can apply a path or bush
 * based approach to this update for example. This class stores the current network state information required to
 * perform the update regardless of the chosen assignment strategy (bush, path).
 * <p>
 * Depending on the applied solution scheme a slightly different approach is to be taken with this update where:
 * <p>
 * POINT QUEUE BASIC: Also update the network sending flow. Only during basic point queue solution scheme, sending
 * flows are NOT locally updated in the sending flow update step. Therefore, sending flows of most links are not
 * updated during the sending flow update because it only updates the sending flows of outgoing links of potentially
 * blocking nodes. When an incoming link of any node is not also an outgoing link of another potentially blocking
 * node its sending flow remains the same even if it actually changes due to further upstream changes in restrictions.
 * In this approach this is taken care of by making sure the sending flows are updated during (this) loading on the
 * path level. Hence, we must update sending flows here.
 * <p>
 * ANY OTHER SOLUTION APPROACH: Here we update all used nodes and sending flows are updated iteratively and locally
 * propagated without the need of the loading in the sending flow update. Therefore, there is no need to update
 * the sending flows. On the other hand we now update the turn flows on all used nodes rather than only the
 * potentially blocking ones.
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
  protected final NetworkLoadingSplittingRateData nlSplittingRateData;

  /**
   * Flag indicating if we are tracking all (used) node turn flows or not
   */
  protected final boolean trackAllNodeTurnFlows;

  /**
   * The output of this update that can be collected after execution. Turn flows are indexed by an id relevant to
   * the invoking party, e.g., movement ids or conjugate segment ids, etc.
   */
  protected final double[] acceptedTurnFlows;

  /**
   * add to accepted turn flows
   * 
   * @param turnFlowIndex  the index to use (abstracted out what this index is)
   * @param flowToAddPcuH to add
   */
  protected void addToAcceptedTurnFlows(final int turnFlowIndex, double flowToAddPcuH) {
    acceptedTurnFlows[turnFlowIndex] += flowToAddPcuH;
  }

  /**
   * Constructor
   *
   * @param trackAllNodeTurnFlows    to apply
   * @param networkLoadingSendingFlowData          to use
   * @param splittingRateData        to use
   * @param networkLoadingFactorData to use
   * @param inflows to use
   * @param outflows to use
   * @param unconstrainedFlowData to use
   * @param numMovements the number of movements available in the layer/network used
   */
  public NetworkTurnFlowUpdateData(
      final boolean trackAllNodeTurnFlows,
      NetworkLoadingSendingFlowData networkLoadingSendingFlowData,
      final NetworkLoadingSplittingRateData splittingRateData,
      NetworkLoadingFactorData networkLoadingFactorData,
      final double[] inflows,
      final double[] outflows,
      final UnconstrainedFlowData unconstrainedFlowData,
      final int numMovements) {
    super(
        networkLoadingSendingFlowData,
        networkLoadingFactorData.getCurrentFlowAcceptanceFactors(),
        inflows,
        outflows,
        unconstrainedFlowData);
    this.acceptedTurnFlows = new double[numMovements];
    this.nlSplittingRateData = splittingRateData;

    /* see class description on why we use these flags */
    this.trackAllNodeTurnFlows = trackAllNodeTurnFlows;
  }

  /**
   * Constructor
   * 
   * @param trackAllNodeTurnFlows    to apply
   * @param networkLoadingSendingFlowData          to use
   * @param splittingRateData        to use
   * @param networkLoadingFactorData to use
   * @param numMovements the number of movements available in the layer/network used
   */
  public NetworkTurnFlowUpdateData(
          final boolean trackAllNodeTurnFlows,
          NetworkLoadingSendingFlowData networkLoadingSendingFlowData,
          final NetworkLoadingSplittingRateData splittingRateData,
          NetworkLoadingFactorData networkLoadingFactorData,
          final long numMovements) {
    super(networkLoadingSendingFlowData, networkLoadingFactorData);
    this.acceptedTurnFlows = new double[(int) numMovements];
    this.nlSplittingRateData = splittingRateData;

    /* see class description on why we use these flags */
    this.trackAllNodeTurnFlows = trackAllNodeTurnFlows;
  }

  /**
   * constructor where sending flows are not to be updated
   * 
   * @param trackAllNodeTurnFlows    flag indicating where or not to track all node turn flows
   * @param splittingRateData        to use
   * @param networkLoadingFactorData to use
   * @param numMovements the number of movements available in the layer/network used
   */
  public NetworkTurnFlowUpdateData(
          final boolean trackAllNodeTurnFlows,
          final NetworkLoadingSplittingRateData splittingRateData,
          NetworkLoadingFactorData networkLoadingFactorData,
          final long numMovements) {
    super(networkLoadingFactorData);
    this.acceptedTurnFlows = new double[(int) numMovements];
    this.nlSplittingRateData = splittingRateData;

    /* see class description on why we use these flags */
    this.trackAllNodeTurnFlows = trackAllNodeTurnFlows;
  }

  /**
   * Access to the result, the accepted turn flows, index by an id relevant to invoking party, e.g., movement id or
   * conjugate segment id, etc., while value is the accepted
   * turn flow v_ab
   * 
   * @return accepted turn flows
   */
  public double[] getAcceptedTurnFlows() {
    return this.acceptedTurnFlows;
  }

  public NetworkLoadingSplittingRateData getNlSplittingRateData() {
    return nlSplittingRateData;
  }

  public boolean isTrackAllNodeTurnFlows() {
    return trackAllNodeTurnFlows;
  }
}
