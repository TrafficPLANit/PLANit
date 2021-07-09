package org.planit.network.virtual;

import org.planit.graph.GraphEntityFactoryImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.virtual.ConnectoidEdge;
import org.planit.utils.network.virtual.ConnectoidSegment;
import org.planit.utils.network.virtual.ConnectoidSegmentFactory;

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
  public ConnectoidSegment registerNew(ConnectoidEdge parent, boolean directionAB) throws PlanItException {
    ConnectoidSegment connectoidSegment = new ConnectoidSegmentImpl(getIdGroupingToken(), parent, directionAB);
    parent.registerConnectoidSegment(connectoidSegment, directionAB);
    getGraphEntities().register(connectoidSegment);
    return connectoidSegment;
  }
}
