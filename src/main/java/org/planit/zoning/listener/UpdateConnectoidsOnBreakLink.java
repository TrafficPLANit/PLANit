package org.planit.zoning.listener;

import java.util.Map;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Point;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.modifier.BreakEdgeListener;
import org.planit.utils.graph.modifier.BreakEdgeSegmentListener;
import org.planit.utils.id.IdAbleImpl;
import org.planit.utils.zoning.DirectedConnectoid;

/**
 * Whenever links are broken and these link are references by connectoids, it is possible we must update the access vertex/link segment of this connectoid. This is what this class
 * ensures.
 * 
 * Class specifically designed to be used in tandem with {@link breakLinksWithInternalNode}. Make sure you identify original downstream vertices of the access link segments for the
 * directed connectoids that could be affected by any break link action on the network. After a break link action this might no be the downstream vertex of the registered access
 * link segments because of the break link action. If changed (due to break links conducted) then we assume the access link segment has been split in two where the original is not
 * closest to the vertex anymore, but the farthest. Hence, we look one link segment downstream and identify if we can match to the desired vertex. If so, we replace the access link
 * segment, if not we let the user know something strange has happened.
 * 
 * @author markr
 *
 * @param <V> type of vertex
 * @param <E> type of edge
 */
public class UpdateConnectoidsOnBreakLink<V extends Vertex, E extends Edge, ES extends EdgeSegment> extends IdAbleImpl implements BreakEdgeSegmentListener<V, E, ES> {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(UpdateConnectoidsOnBreakLink.class.getCanonicalName());

  /** information on the connectoid's desired access node location (before the break link action) */
  private final Map<Point, DirectedConnectoid> connectoidsAccessNodeLocationBeforeBreakLink;

  /**
   * constructor taking information regarding the access link segment downstream vertex that the connectoid is attached to before any breaking of links. This will be used to update
   * the connectoid to this same location after the break link in case this has been compromised
   * 
   * @param connectoidsAccessNodeLocationBeforeBreakLink
   */
  public UpdateConnectoidsOnBreakLink(Map<Point, DirectedConnectoid> connectoidsAccessNodeLocationBeforeBreakLink) {
    super(BreakEdgeListener.generateId());
    this.connectoidsAccessNodeLocationBeforeBreakLink = connectoidsAccessNodeLocationBeforeBreakLink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onBreakEdge(V vertex, E aToBreak, E breakToB) {
    /* do nothing, we deal with this per link segment */
  }

  /**
   * 
   * 
   * {@inheritDoc}
   * 
   */
  @Override
  public void onBreakEdgeSegment(V vertex, E brokenEdge, ES brokenEdgeSegment) {

    /* determine if connectoid is related to this broken edge segment somehow */
    DirectedConnectoid connectoid = null;
    if (connectoidsAccessNodeLocationBeforeBreakLink.containsKey(brokenEdgeSegment.getDownstreamVertex().getPosition())) {
      connectoid = connectoidsAccessNodeLocationBeforeBreakLink.get(brokenEdgeSegment.getDownstreamVertex().getPosition());
      if(!connectoid.isNodeAccessDownstream()) {
        LOGGER.severe(String.format("update of connectoids only supported when access node resides on downstream end of access link segment, but for connectoid %d this is not the case",connectoid.getId()));
        return;
      }
    }
      
    if (connectoid!= null && !connectoid.getAccessLinkSegment().idEquals(brokenEdgeSegment)) {
      /*
       * mismatch: connectoid access node no longer corresponds to access link segment downstream vertex, meaning that because of breaking the link/linksegment 
       * and reusing the old link/linksegment for part of the broken link its downstream node now resides halfway the link instead of its original location, 
       * causing an inconsistency between the access node and access link segment of the connectoid. correct this
       */
      if( brokenEdgeSegment.getUpstreamVertex().idEquals(connectoid.getAccessLinkSegment().getDownstreamVertex()) &&
          brokenEdgeSegment.getUpstreamVertex().idEquals(vertex)){
        /* the broken edge segment upstream vertex is the location where we broke the link and the original access link segment now ends at this vertex even though
         * it should end at the access node (which is the downstream node of the broken edge segment -> hence, the broken edge segment is now the access link segment
         * directly upstream the access node AND it resides on the right link since it is directly connected to the original access link segment via the broken vertex  
         */
        connectoid.replaceAccessLinkSegment(brokenEdgeSegment);          
      }
    }  
  }

}
