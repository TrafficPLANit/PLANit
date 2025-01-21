package org.goplanit.output.adapter.traits;

import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;

import java.util.Optional;

/**
 * Interface defining the methods (traits) required for a network level link outputs adapter.
 * <p>
 * to be used to supplement hierarchy of output type adapter interfaces without it being an output type adapter itself
 * </p>
 * 
 * @author markr
 *
 */
public interface NetworkSegmentsOutputTypeAdapterTrait<ES extends EdgeSegment>  {

  /**
   * collect the infrastructure layer id this mode resides on
   *
   * @param mode to collect layer id for
   * @return infrastructure layer id, null if not found
   */
  public abstract Optional<Long> getInfrastructureLayerIdForMode(Mode mode);

  /**
   * Return the Link segments for the given layer
   * 
   * @param layerId to collect link segments for
   * @return a List of link segments for this assignment
   */
  public abstract GraphEntities<? extends ES> getLinkSegmentsForLayer(long layerId);

}
