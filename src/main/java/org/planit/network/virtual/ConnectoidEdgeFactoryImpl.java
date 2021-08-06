package org.planit.network.virtual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.planit.graph.GraphEntityFactoryImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.GraphEntities;
import org.planit.utils.graph.directed.DirectedVertex;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.virtual.ConnectoidEdge;
import org.planit.utils.network.virtual.ConnectoidEdgeFactory;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.Zone;

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
  public Collection<ConnectoidEdge> registerNew(Connectoid connectoid) throws PlanItException {

    /* constructed from connectoid information */
    ArrayList<ConnectoidEdge> connectoidEdges = new ArrayList<ConnectoidEdge>();
    for (Zone accessZone : connectoid) {

      /* Access node */
      /* for now we only utilise a single access node, either the given one, or the downstream node of a directed connectoid */
      /* TODO: when we implement PT assignments this likely will change */
      DirectedVertex accessVertex = null;
      if (connectoid instanceof UndirectedConnectoid) {
        accessVertex = UndirectedConnectoid.class.cast(connectoid).getAccessVertex();
      } else if (connectoid instanceof DirectedConnectoid) {
        EdgeSegment accessEdgeSegment = DirectedConnectoid.class.cast(connectoid).getAccessLinkSegment();
        accessVertex = (Node) (accessEdgeSegment != null ? accessEdgeSegment.getDownstreamVertex() : null);
      } else {
        throw new PlanItException(String.format("connectoid %s is of unrecognised type and access node could not be retrieved", connectoid.getXmlId()));
      }

      /* create and register connectoid edge */
      Optional<Double> connectoidLength = connectoid.getLengthKm(accessZone);
      connectoidLength.orElseThrow(() -> new PlanItException("unable to retrieve lenght for connectoid %s (id:%d)", connectoid.getXmlId(), connectoid.getId()));
      ConnectoidEdge newConnectoidEdge = new ConnectoidEdgeImpl(getIdGroupingToken(), accessZone.getCentroid(), accessVertex, connectoidLength.get());
      getGraphEntities().register(newConnectoidEdge);
      connectoidEdges.add(newConnectoidEdge);
    }
    return connectoidEdges;
  }
}