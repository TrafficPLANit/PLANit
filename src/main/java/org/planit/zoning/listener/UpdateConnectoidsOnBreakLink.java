package org.planit.zoning.listener;

import java.util.Map;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Point;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.graph.modifier.BreakEdgeSegmentListener;
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
public class UpdateConnectoidsOnBreakLink<V extends Vertex, E extends Edge, ES extends EdgeSegment> implements BreakEdgeSegmentListener<V, E, ES> {

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

    if (connectoidsAccessNodeLocationBeforeBreakLink.containsKey(brokenEdgeSegment.getDownstreamVertex().getPosition())) {
      DirectedConnectoid connectoid = connectoidsAccessNodeLocationBeforeBreakLink.get(brokenEdgeSegment.getDownstreamVertex().getPosition());
      if (!connectoid.getAccessLinkSegment().idEquals(brokenEdgeSegment)) {
        /*
         * mismatch, meaning that because of breaking the link/linksegment and reusing the old link/linksegment for part of the broken link its downstream node now resides halfway
         * the link instead of its original location, causing an inconsistency between the access node and access link segment of the connectoid. correct this
         */

      }
    }

    // OLD

//    /*
//     * in case due to breaking links the access link segments no longer represent the link segment directly upstream of the original vertex (downstream of the access link segment
//     * before breaking the links, this method will update the directed connectoids to undo this and update their access link segments where needed
//     */
//    for (Entry<DirectedConnectoid, Point> entry : connectoidsAccessNodeLocationBeforeBreakLink.entrySet()) {
//      DirectedConnectoid connectoid = entry.getKey();
//      if (aToBreak.getVertexA().idEquals(connectoid.getAccessNode()))
//        
//      Point desiredAccessNodeLocation = entry.getValue();
//      Point currentAccessNodePosition = connectoid.getAccessLinkSegment().getDownstreamVertex().getPosition();
//      if (!currentAccessNodePosition.getCoordinate().equals2D(desiredAccessNodeLocation.getCoordinate())) {
//        /*
//         * due to breaking link, the link segment's downstream node is no longer the same. since we only use the link segment for direction bu practically are more interested in
//         * the node location as this is the actual position of the stop, the stop_location of this existing connectoid has effectively moved due to breaking links for this current
//         * connectoid therefore, to correct this and retain the original position of the stop location, we must update the reference link segment so that it again reflects the link
//         * segment closest to the original location of the original downstream vertex. This requires us to change the link segment reference to the
//         */
//        boolean matchFound = false;
//        for (EdgeSegment exitEdgeSegment : currentDownstreamVertex.getExitEdgeSegments()) {
//          if (exitEdgeSegment.getDownstreamVertex().idEquals(desiredDownstreamVertex)) {
//            /* this is the new edge segment directly upstream of the reference vertex, use this instead */
//            connectoid.replaceAccessLinkSegment(exitEdgeSegment);
//            matchFound = true;
//            break;
//          }
//        }
//
//        if (matchFound == false) {
//          LOGGER.severe(String.format(
//              "Unable to replace access link segment of directed connectoid (stop_location) %s, could not find desired node %s directly downstream of original access link segment %s",
//              connectoid.getExternalId(), desiredDownstreamVertex.getExternalId(), connectoid.getAccessLinkSegment().getExternalId()));
//        }
//      }
//    }
  }

}
