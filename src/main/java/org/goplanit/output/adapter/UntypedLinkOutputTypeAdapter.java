package org.goplanit.output.adapter;

import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.EdgeUtils;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.locationtech.jts.geom.Geometry;

import java.util.Optional;

/**
 * Interface defining the methods required for a link output adapter agnostic to how these link (segments) are
 * containerized or stored in a larger setting.
 *
 * @author gman6028, markr
 * @param <T> type of segment
 */
public interface UntypedLinkOutputTypeAdapter<T extends LinkSegment> extends UntypedEdgeOutputTypeAdapter<T> {

  /**
   * Returns the number of lanes of the current link
   * 
   * @param linkSegment LinkSegment object containing the required data
   * @return the number of lanes of the current link
   */
  public default Optional<Integer> getNumberOfLanes(T linkSegment){
    return Optional.of(linkSegment.getNumberOfLanes());
  }

  /**
   * Return the value of a specified output property of a link segment
   * 
   * @param outputProperty the specified output property
   * @param linkSegment    the specified link segment
   * @return the value of the specified output property (or an Exception if an error occurs)
   */
  public abstract Optional<?> getLinkSegmentOutputPropertyValue(OutputProperty outputProperty, T linkSegment);
}
