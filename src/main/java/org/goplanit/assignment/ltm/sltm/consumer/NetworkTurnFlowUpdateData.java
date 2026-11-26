package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingFactorData;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingSendingFlowData;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingSplittingRateData;
import org.goplanit.assignment.ltm.sltm.loading.UnconstrainedFlowData;

/**
 * Base class to help update the network turn flows during loading. Derived classes can, for example, take a path or
 * bush based approach to this update. This class stores the current network state needed for the update, whatever
 * the assignment strategy (bush, path).
 * <p>
 * The update differs slightly by solution scheme:
 * <p>
 * POINT QUEUE BASIC: also update the network sending flow. Only in the basic point queue solution scheme are sending
 * flows NOT updated locally in the sending flow update step: it updates only the sending flows of outgoing links of
 * potentially blocking nodes, so most links are not updated. An incoming link of a node that is not also an outgoing
 * link of another potentially blocking node keeps its sending flow, even if it changes due to further upstream changes
 * in restrictions. This is taken care of by updating the sending flows during (this) loading on the path level, so
 * sending flows must be updated here.
 * <p>
 * ANY OTHER SOLUTION APPROACH: all used nodes are updated, and sending flows are updated iteratively and propagated
 * locally without needing the loading in the sending flow update. So there is no need to update the sending flows.
 * On the other hand, the turn flows are updated on all used nodes rather than only the potentially blocking ones.
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
   * @param numCompiledTurns the number of compiled turns available in the layer/network used
   */
  public NetworkTurnFlowUpdateData(
      final boolean trackAllNodeTurnFlows,
      NetworkLoadingSendingFlowData networkLoadingSendingFlowData,
      final NetworkLoadingSplittingRateData splittingRateData,
      NetworkLoadingFactorData networkLoadingFactorData,
      final double[] inflows,
      final double[] outflows,
      final UnconstrainedFlowData unconstrainedFlowData,
      final int numCompiledTurns) {
    super(
        networkLoadingSendingFlowData,
        networkLoadingFactorData.getCurrentFlowAcceptanceFactors(),
        inflows,
        outflows,
        unconstrainedFlowData);
    this.acceptedTurnFlows = new double[numCompiledTurns];
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
   * @param numCompiledTurns the number of compiled turns available in the layer/network used
   */
  public NetworkTurnFlowUpdateData(
          final boolean trackAllNodeTurnFlows,
          NetworkLoadingSendingFlowData networkLoadingSendingFlowData,
          final NetworkLoadingSplittingRateData splittingRateData,
          NetworkLoadingFactorData networkLoadingFactorData,
          final long numCompiledTurns) {
    super(networkLoadingSendingFlowData, networkLoadingFactorData);
    this.acceptedTurnFlows = new double[(int) numCompiledTurns];
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
   * @param numCompiledTurns the number of compiled turns available in the layer/network used
   */
  public NetworkTurnFlowUpdateData(
          final boolean trackAllNodeTurnFlows,
          final NetworkLoadingSplittingRateData splittingRateData,
          NetworkLoadingFactorData networkLoadingFactorData,
          final long numCompiledTurns) {
    super(networkLoadingFactorData);
    this.acceptedTurnFlows = new double[(int) numCompiledTurns];
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
