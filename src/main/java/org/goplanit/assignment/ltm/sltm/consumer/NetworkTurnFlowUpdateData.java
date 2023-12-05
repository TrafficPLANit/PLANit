package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.loading.NetworkLoadingFactorData;
import org.goplanit.assignment.ltm.sltm.loading.SendingFlowData;
import org.goplanit.assignment.ltm.sltm.loading.SplittingRateData;
import org.goplanit.utils.graph.directed.EdgeSegment;

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
   * The output of this update that can be collected after execution. They key of this map is forged from an entry and exit segment
   */
  protected final MultiKeyMap<Object, Double> acceptedTurnFlows;

  /**
   * add to accepted turn flows
   * 
   * @param entrySegment  to use
   * @param exitSegment   to use
   * @param flowToAddPcuH to add
   */
  protected void addToAcceptedTurnFlows(final EdgeSegment entrySegment, final EdgeSegment exitSegment, double flowToAddPcuH) {
    Double currentAcceptedTurnFlow = acceptedTurnFlows.get(entrySegment, exitSegment);
    if (currentAcceptedTurnFlow == null) {
      acceptedTurnFlows.put(entrySegment, exitSegment, flowToAddPcuH);
    } else {
      acceptedTurnFlows.put(entrySegment, exitSegment, currentAcceptedTurnFlow + flowToAddPcuH);
    }
  }

  /**
   * Constructor
   * 
   * @param trackAllNodeTurnFlows    to apply
   * @param sendingFlowData          to use
   * @param splittingRateData        to use
   * @param networkLoadingFactorData to use
   */
  public NetworkTurnFlowUpdateData(final boolean trackAllNodeTurnFlows, SendingFlowData sendingFlowData, final SplittingRateData splittingRateData,
      NetworkLoadingFactorData networkLoadingFactorData) {
    super(sendingFlowData, networkLoadingFactorData);
    this.acceptedTurnFlows = new MultiKeyMap<Object, Double>();
    this.splittingRateData = splittingRateData;

    /* see class description on why we use these flags */
    this.trackAllNodeTurnFlows = trackAllNodeTurnFlows;
  }

  /**
   * constructor where sending flows are not to be updated
   * 
   * @param trackAllNodeTurnFlows    flag indicating where or not to track all node turn flows
   * @param splittingRateData        to use
   * @param networkLoadingFactorData to use
   */
  public NetworkTurnFlowUpdateData(final boolean trackAllNodeTurnFlows, final SplittingRateData splittingRateData, NetworkLoadingFactorData networkLoadingFactorData) {
    super(networkLoadingFactorData);
    this.acceptedTurnFlows = new MultiKeyMap<>();
    this.splittingRateData = splittingRateData;

    /* see class description on why we use these flags */
    this.trackAllNodeTurnFlows = trackAllNodeTurnFlows;
  }

  /**
   * Access to the result, the accepted turn flows, where key comprises a combined hash of entry and exit edge segment ids and value is the accepted turn flow v_ab
   * 
   * @return accepted turn flows
   */
  public MultiKeyMap<Object, Double> getAcceptedTurnFlows() {
    return this.acceptedTurnFlows;
  }
}
