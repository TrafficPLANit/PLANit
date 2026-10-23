package org.goplanit.zoning.modifier.event.handler;

import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.graph.directed.modifier.event.BreakEdgeSegmentEvent;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.utils.zoning.connectoid.DirectedConnectoidAccessZoneEntry;

/**
 * Whenever links are broken and these links are referenced by connectoids, it is possible we must update the access
 * link segments of this connectoid. This is what this class
 * ensures by listening to break edge segment events and taking action accordingly based on the provided connectoids
 * if they are affected.
 * <p>
 * Class is specifically designed to be used in tandem with breakLinksWithInternalNode. Make sure you identify
 * affected connectoids  access link segments that could be affected by any break link action on the network.
 * After a break link action this might no longer be the correct access segment of the connectoid.
 * If changed (due to break links conducted) then we assume the access link segment has been split in two
 * where the original is not attached to the access vertex anymore. Hence, we look one link segment
 * downstream and identify if we can match to the desired vertex. If so, we replace the access link segment,
 * if not we let the user know something strange has happened.
 * </p>
 * 
 * @author markr
 *
 */
public class UpdateDirectedConnectoidsOnBreakLinkSegmentHandler implements DirectedGraphModifierListener {

  private static final Logger LOGGER =
      Logger.getLogger(UpdateDirectedConnectoidsOnBreakLinkSegmentHandler.class.getCanonicalName());

  /** information on the connectoid's desired access node location (before the break link action) */
  //private final Map<Point, Set<DirectedConnectoid>> connectoidsAccessNodeLocationBeforeBreakLink;
  private final Set<TransferConnectoid> potentiallyAffectedConnectoids;

  /**
   * perform the actual update of the connectoids based on the broken edge segment
   *
   * @param vertexWeBrokeAt     we broke at
   * @param brokenEdgeSegment   that is now broken based on vertex
   * @param originalEdgeSegment that was the original instance before the link was broken
   */
  protected void updateConnectedAccessLinkSegment(
      DirectedVertex vertexWeBrokeAt, LinkSegment brokenEdgeSegment, LinkSegment originalEdgeSegment) {

    /* broken segment no longer on connectoid while original was/is. In that case we must act because
    * either it was repurposed or we should connect it to the new and remove the original because it no longer attaches
    * Note that: for each found we filter also based on the new broken segment attaching to the ref vertex of
    * connectoid, only when it does we should replace it, otherwise the other broken segment is the correct one
    */
    var connectoidsWithDiscrepancy = potentiallyAffectedConnectoids.stream().filter(c ->
        !c.hasAccessLinkSegment(brokenEdgeSegment) && c.hasAccessLinkSegment(originalEdgeSegment) &&
        brokenEdgeSegment.hasAnyVertex(c.getReferenceVertex())).collect(
            Collectors.toList());

    // locate the original access segment and replace it with the missing one. Only replace existing entries because
    // otherwise we would be getting false positives where the original was not registered as an access segment perhaps
    // for certain zone/type combinations
    for(var connectoid :  connectoidsWithDiscrepancy){
      for(var entry : connectoid){
        if(entry instanceof DirectedConnectoidAccessZoneEntry &&
            ((DirectedConnectoidAccessZoneEntry) entry).hasAccessLinkSegment(originalEdgeSegment)){
          var directedEntry = ((DirectedConnectoidAccessZoneEntry) entry);
          directedEntry.removeAccessLinkSegment(originalEdgeSegment);
          directedEntry.addAccessLinkSegment(brokenEdgeSegment);
        }
      }
    }
  }

  /**
   * Constructor taking information regarding the connectoids attached to links that are broken before any breaking
   * of links occurred. This will be used to update the connectoid to the correct configuration after the break
   * link in case this has been compromised.
   * 
   * @param potentiallyAffectedConnectoids to consider
   */
  public UpdateDirectedConnectoidsOnBreakLinkSegmentHandler(
      Set<TransferConnectoid> potentiallyAffectedConnectoids) {
    super();
    this.potentiallyAffectedConnectoids = potentiallyAffectedConnectoids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { BreakEdgeSegmentEvent.EVENT_TYPE };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    LOGGER.warning(String.format("%s only supports break edge segment events",
        UpdateDirectedConnectoidsOnBreakLinkSegmentHandler.class.getName()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
    if (!event.getType().equals(BreakEdgeSegmentEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports break edge segment events",
          UpdateDirectedConnectoidsOnBreakLinkSegmentHandler.class.getName()));
      return;
    }

    /* update the access link segment of the relevant connectoids (if any) based on broken edge segment */
    BreakEdgeSegmentEvent breakEdgeSegmentEvent = (BreakEdgeSegmentEvent) event;
    updateConnectedAccessLinkSegment(
        breakEdgeSegmentEvent.getVertexToBreakAt(),
        (LinkSegment) breakEdgeSegmentEvent.getNewlyBrokenEdgeSegment(),
        (LinkSegment) breakEdgeSegmentEvent.getOriginalEdgeSegment());
  }

}
