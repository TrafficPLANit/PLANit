package org.goplanit.network.virtual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.virtual.CentroidVertex;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidEdgeFactory;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.UndirectedConnectoid;
import org.goplanit.utils.zoning.Zone;

/**
 * Factory for creating connectoid edges on connectoid edge container
 * 
 * @author markr
 */
public class ConnectoidEdgeFactoryImpl extends GraphEntityFactoryImpl<ConnectoidEdge> implements ConnectoidEdgeFactory {

  /**
   * Constructor
   * 
   * @param groupId         to use
   * @param connectoidEdges to use
   */
  protected ConnectoidEdgeFactoryImpl(final IdGroupingToken groupId, GraphEntities<ConnectoidEdge> connectoidEdges) {
    super(groupId, connectoidEdges);
  }

  /**
   * {@inheritDoc}
   */
  public ConnectoidEdge registerNew(CentroidVertex centroidVertex, DirectedVertex nonCentroidVertex, double lengthKm){
      /* create and register connectoid edge */
      ConnectoidEdge newConnectoidEdge = new ConnectoidEdgeImpl(getIdGroupingToken(), centroidVertex, nonCentroidVertex, lengthKm);
      getGraphEntities().register(newConnectoidEdge);
    return newConnectoidEdge;
  }
}
