package org.goplanit.network.virtual;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.NodeFactory;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.network.virtual.CentroidVertexFactory;
import org.goplanit.utils.network.virtual.CentroidVertices;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.zoning.Centroid;
import org.locationtech.jts.geom.Point;

/**
 * Factory for creating nodes on nodes container
 * 
 * @author markr
 */
public class CentroidVertexFactoryImpl extends GraphEntityFactoryImpl<CentroidVertex> implements CentroidVertexFactory {

  /**
   * Constructor
   * 
   * @param groupId  to use
   * @param vertices to use
   */
  protected CentroidVertexFactoryImpl(final IdGroupingToken groupId, final CentroidVertices vertices) {
    super(groupId, vertices);
  }

  /**
   * Create and register new entity (without setting its parent yet)
   *
   * @return new centroid vertex created
   */
  @Override
  public CentroidVertex registerNew() {
    return registerNew(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidVertex createNew(Centroid parent) {
    return new CentroidVertexImpl(getIdGroupingToken(), parent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidVertex registerNew(Centroid parent) {
    final CentroidVertex newVertex = createNew(parent);
    getGraphEntities().register(newVertex);
    return newVertex;
  }


}
