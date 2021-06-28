package org.planit.network.physical;

import org.planit.graph.EdgesWrapper;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edges;
import org.planit.utils.graph.Vertex;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.Links;

/**
 * 
 * Links implementation wrapper that simply utilises passed in edges of the desired generic type to delegate registration and creation of its links on
 * 
 * @author markr
 * 
 * @param <L> link type
 */
public class LinksImpl<L extends Link> extends EdgesWrapper<L> implements Links<L> {

  /**
   * Constructor
   * 
   * @param edges the edges to use to create and register links on
   */
  public LinksImpl(final Edges<L> edges) {
    super(edges);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public L registerNew(Vertex vertexA, Vertex vertexB, double lengthKm, boolean registerOnVertices) throws PlanItException {
    L link = (L) registerNew(vertexA, vertexB, false);
    link.setLengthKm(lengthKm);
    return link;
  }

}
