package org.goplanit.network.virtual.graph;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.graph.ConnectoidDirectedEdge;
import org.goplanit.utils.network.virtual.graph.ConnectoidEdgeFactory;

/**
 * Factory for creating connectoid edges on connectoid edge container
 * 
 * @author markr
 */
public class ConnectoidEdgeFactoryImpl extends GraphEntityFactoryImpl<ConnectoidDirectedEdge> implements ConnectoidEdgeFactory {

  /**
   * Constructor
   * 
   * @param groupId         to use
   * @param connectoidEdges to use
   */
  protected ConnectoidEdgeFactoryImpl(final IdGroupingToken groupId, GraphEntities<ConnectoidDirectedEdge> connectoidEdges) {
    super(groupId, connectoidEdges);
  }

  /**
   * {@inheritDoc}
   */
  public ConnectoidDirectedEdge registerNew(CentroidVertex centroidVertex, DirectedVertex nonCentroidVertex, double lengthKm){
      /* create and register connectoid edge */
      ConnectoidDirectedEdge newConnectoidEdge = new ConnectoidDirectedEdgeImpl(getIdGroupingToken(), centroidVertex, nonCentroidVertex, lengthKm);
      getGraphEntities().register(newConnectoidEdge);
    return newConnectoidEdge;
  }
}
