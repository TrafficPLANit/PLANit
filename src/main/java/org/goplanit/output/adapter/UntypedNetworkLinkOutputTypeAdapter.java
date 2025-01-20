package org.goplanit.output.adapter;

import java.util.Optional;

import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.EdgeUtils;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.locationtech.jts.geom.Geometry;

/**
 * Interface defining the methods required for a link output adapter in a network (layer) context
 * 
 * @author markr
 *
 */
public interface UntypedNetworkLinkOutputTypeAdapter<ES extends EdgeSegment> extends UntypedLinkOutputTypeAdapter<ES> {

  /**
   * Return the Link segments for the given layer
   * 
   * @param layerId to collect link segments for
   * @return a List of link segments for this assignment
   */
  public abstract GraphEntities<? extends ES> getLinkSegmentsForLayer(long layerId);

}
