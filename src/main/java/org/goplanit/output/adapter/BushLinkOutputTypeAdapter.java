package org.goplanit.output.adapter;

import org.goplanit.output.adapter.traits.UntypedBushSegmentsOutputTypeAdapterTrait;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.time.TimePeriod;

import java.util.Optional;

/**
 * Interface defining the methods required for a bush edge (segment) output adapter, i.e., for edge segments that are
 * part of a bush. For now, these include both conjugate and non-conjugate bushes, so we keep the edge segment as the
 * base class to be universally usable (albeit limited in what can be outputted).
 * 
 * @author markr
 *
 */
public interface BushLinkOutputTypeAdapter
        extends UntypedBushSegmentsOutputTypeAdapterTrait<EdgeSegment>, UntypedEdgeOutputTypeAdapter<EdgeSegment> {

  /**
   * Return the value of a specified output property of a bush(used) eligible edge segment
   *
   * @param outputProperty the specified output property
   * @param linkSegment    the specified edge segment
   * @param mode           the current mode
   * @param timePeriod     the current time period
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  public Optional<?> getEdgeSegmentOutputPropertyValue(
      OutputProperty outputProperty, EdgeSegment linkSegment, Mode mode, TimePeriod timePeriod);

}
