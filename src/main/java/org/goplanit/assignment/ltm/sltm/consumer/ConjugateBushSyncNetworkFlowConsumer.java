package org.goplanit.assignment.ltm.sltm.consumer;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;

import java.util.logging.Logger;

/**
 * ConjugateBushSyncNetworkFlowConsumer
 */
public class ConjugateBushSyncNetworkFlowConsumer
        extends ConjugateBushTurnFlowUpdateConsumer {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConjugateBushSyncNetworkFlowConsumer.class.getCanonicalName());

  /**
   * constructor
   *
   * @param dataConfig to use
   * @param turn2ConjSegmentMapping to use
   */
  public ConjugateBushSyncNetworkFlowConsumer(final NetworkTurnFlowUpdateData dataConfig,
                                              final MultiKeyMap<Object, ConjugateEdgeSegment> turn2ConjSegmentMapping) {
    super(dataConfig,turn2ConjSegmentMapping);
  }

}
