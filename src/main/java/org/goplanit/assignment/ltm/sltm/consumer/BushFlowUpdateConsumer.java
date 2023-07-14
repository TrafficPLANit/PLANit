package org.goplanit.assignment.ltm.sltm.consumer;

import java.util.function.Consumer;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.assignment.ltm.sltm.Bush;

/**
 * Placeholder for bush based network loading flow update for each origin bush
 * 
 * @author markr
 *
 */
public interface BushFlowUpdateConsumer<B extends Bush> extends Consumer<B> {

  /**
   * The found accepted turn flows by the combined entry-exit segment.
   * 
   * @return accepted turn flows
   */
  public default MultiKeyMap<Object, Double> getAcceptedTurnFlows() {
    // TODO: ugly should be refactored
    // when (Derived) consumer is turn based this can be overridden to provide results
    return null;
  }

}
