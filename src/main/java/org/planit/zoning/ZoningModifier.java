package org.planit.zoning;

import java.util.Map;
import java.util.Map.Entry;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedVertex;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.zoning.DirectedConnectoid;

/**
 * Counterpart to its graph equivalent {@link GraphModifier}. Provides methods to make modifications to the zoning on a higher level. Currently mainly methods are 
 * provided to keep any references to network entities consistent when modifications are made to the network using the GraphModifier, e.g, break links which can cause
 * references to links or link segments on zoning entities to become invalid.
 * 
 * @author mark
 *
 */
public interface ZoningModifier {

  /**
   * Method specifically designed to be used in tandem with {@link breakLinksWithInternalNode}. Make sure you identify original downstream vertices of the access link segments 
   * for the directed connectoids that could be affected by any break link action on the network. After a break link action this might no be the downstream vertex of the registered 
   * access link segments because of the break link action. If changed (due to break links conducted) then we assume the access link segment has been split in two 
   * where the original is not closest to the vertex anymore, but the farthest. Hence, we look one link segment downstream and identify
   * if we can match to the desired vertex. If so, we replace the access link segment, if not we let the user know something strange has happened.
   * 
   * 
   * @param connectoidsDesiredDownstreamVertices containing original and desired downstream vertices of the directed connectoids in question
   * @throws PlanItException thrown if error
   */
  public static void updateLinkSegmentsForDirectedConnectoids(Map<DirectedConnectoid, DirectedVertex> connectoidsDesiredDownstreamVertices) throws PlanItException {
    for(Entry<DirectedConnectoid, DirectedVertex> entry : connectoidsDesiredDownstreamVertices.entrySet()) {
      DirectedConnectoid connectoid = entry.getKey();
      DirectedVertex desiredDownstreamVertex = entry.getValue();
      DirectedVertex currentDownstreamVertex =connectoid.getAccessLinkSegment().getDownstreamVertex();
      if(!currentDownstreamVertex.idEquals(desiredDownstreamVertex)) {
        /* due to breaking link, the link segment's downstream node is no longer the same. since we only use the link segment for direction bu practically are more interested
         * in the node location as this is the actual position of the stop, the stop_location of this existing connectoid has effectively moved due to breaking links for this current connectoid
         * therefore, to correct this and retain the original position of the stop location, we must update the reference link segment so that it again reflects the link segment closest to the original location of the original 
         * downstream vertex. This requires us to change the link segment reference to the 
         */
        boolean matchFound = false;
        for(EdgeSegment exitEdgeSegment :currentDownstreamVertex.getExitEdgeSegments()) {
          if(exitEdgeSegment.getDownstreamVertex().idEquals(desiredDownstreamVertex)){
            /* this is the new edge segment directly upstream of the reference vertex, use this instead */
            connectoid.replaceAccessLinkSegment(exitEdgeSegment);
            matchFound =true;
            break;
          }
        }
        
        if(matchFound == false) {
          throw new PlanItException(
              "Unable to replace access link segment of directed connectoid (stop_location) %s, could not find desired node %s directly downstream of original access link segment %s",
              connectoid.getExternalId(), desiredDownstreamVertex.getExternalId(), connectoid.getAccessLinkSegment().getExternalId()); 
        }
      }
    }
  }
}
