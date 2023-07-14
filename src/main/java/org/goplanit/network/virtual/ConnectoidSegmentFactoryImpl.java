package org.goplanit.network.virtual;

import org.goplanit.graph.GraphEntityFactoryImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.virtual.ConnectoidEdge;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegmentFactory;

/**
 * Factory for creating connectoid segments on connectoid segments container
 * 
 * @author markr
 */
public class ConnectoidSegmentFactoryImpl extends GraphEntityFactoryImpl<ConnectoidSegment> implements ConnectoidSegmentFactory {

  /**
   * Constructor
   * 
   * @param groupId            to use
   * @param connectoidSegments to use
   */
  protected ConnectoidSegmentFactoryImpl(final IdGroupingToken groupId, GraphEntities<ConnectoidSegment> connectoidSegments) {
    super(groupId, connectoidSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegment registerNew(ConnectoidEdge parent, boolean directionAb) {
    ConnectoidSegment connectoidSegment = new ConnectoidSegmentImpl(getIdGroupingToken(), parent, directionAb);
    parent.registerConnectoidSegment(connectoidSegment, directionAb);
    getGraphEntities().register(connectoidSegment);
    return connectoidSegment;
  }
}
