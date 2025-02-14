package org.goplanit.network.virtual.physical;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.graph.CentroidVertex;
import org.goplanit.utils.network.virtual.physical.ConnectoidLink;
import org.goplanit.utils.network.virtual.physical.ConnectoidLinkFactory;

/**
 * Factory for creating connectoid links on connectoid link container
 * 
 * @author markr
 */
public class ConnectoidLinkFactoryImpl extends GraphEntityFactoryImpl<ConnectoidLink> implements ConnectoidLinkFactory {

  /**
   * Constructor
   *
   * @param groupId         to use
   * @param connectoidEdges to use
   */
  protected ConnectoidLinkFactoryImpl(final IdGroupingToken groupId, GraphEntities<ConnectoidLink> connectoidEdges) {
    super(groupId, connectoidEdges);
  }

  /**
   * {@inheritDoc}
   */
  public ConnectoidLink registerNew(CentroidVertex centroidVertex, DirectedVertex nonCentroidVertex, double lengthKm){
      /* create and register connectoid edge */
    ConnectoidLink newConnectoidLink = new ConnectoidLinkImpl(
            getIdGroupingToken(), centroidVertex, nonCentroidVertex, lengthKm);
      getGraphEntities().register(newConnectoidLink);
    return newConnectoidLink;
  }
}
