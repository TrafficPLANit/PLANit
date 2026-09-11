package org.goplanit.assignment.ltm.sltm.consumer;

import org.goplanit.utils.network.layer.physical.CompiledRelationMapping;

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
   * @param compiledMovementIds to use
   */
  public ConjugateBushSyncNetworkFlowConsumer(final NetworkTurnFlowUpdateData dataConfig,
                                              final CompiledRelationMapping compiledMovementIds) {
    super(dataConfig,compiledMovementIds);
  }

}
