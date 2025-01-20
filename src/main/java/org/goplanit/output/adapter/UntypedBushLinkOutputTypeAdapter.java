package org.goplanit.output.adapter;

import org.goplanit.assignment.ltm.sltm.Bush;
import org.goplanit.assignment.ltm.sltm.RootedBush;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.EdgeUtils;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ACyclicSubGraph;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.locationtech.jts.geom.Geometry;

import java.util.Collection;
import java.util.Optional;

/**
 * Interface defining the methods required for a bush-based specific link output adapter, e.g., per bush allow for
 * persisting of its link information.
 * 
 * @author markr
 *
 */
public interface UntypedBushLinkOutputTypeAdapter<ES extends EdgeSegment> extends UntypedNetworkLinkOutputTypeAdapter<ES> {

  /**
   * Provide access to all bushes eligible for persisting. Each bush contains a subset of the physical network used by
   * the traffic assignment
   *
   * @return collection of bushes
   */
  public abstract RootedBush<? extends DirectedVertex, ? extends ES>[] getBushes();
}
