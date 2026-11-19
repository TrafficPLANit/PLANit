package org.goplanit.network.layer.macroscopic.intersection.modifier.event;

import java.util.List;

import org.goplanit.utils.event.EventImpl;
import org.goplanit.utils.event.EventState;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventType;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Event fired when collapse starts on a spatial intersection, and again when it is done, saying which by its state
 * <p>
 * Collapsing an intersection for some modes changes many links and nodes in turn. A listener that acts on the
 * intersection as a whole, such as a zoning keeping its stops, works out what to do when collapse starts, while the
 * intersection is still spatial, and acts when it is done, once all its changes are made, rather than on each change
 * as it comes
 * </p>
 *
 * @author markr
 */
public class CollapseSpatialIntersectionEvent extends EventImpl implements IntersectionModificationEvent {

  /** event type fired off when collapse starts, or is done, on a spatial intersection */
  public static final IntersectionModifierEventType EVENT_TYPE =
      new IntersectionModifierEventType("INTERSECTIONMODIFIER.SPATIALINTERSECTION.COLLAPSE");

  /**
   * Constructor
   *
   * @param source layer modifier firing the event
   * @param layer the intersection is on
   * @param intersection collapsed
   * @param centreNode the intersection is collapsed onto
   * @param memberNodes of the intersection when collapse starts, the centre node excluded
   * @param collapsedModes the modes collapsed onto the centre node
   * @param state START when collapse is about to change the intersection, DONE when all its changes are made
   */
  public CollapseSpatialIntersectionEvent(final MacroscopicNetworkLayerModifier source,
      final MacroscopicNetworkLayer layer, final SpatialIntersection intersection, final Node centreNode,
      final List<Node> memberNodes, final List<Mode> collapsedModes, final EventState state) {
    super(EVENT_TYPE, source,
        new Object[] {layer, intersection, centreNode, List.copyOf(memberNodes), List.copyOf(collapsedModes)}, state);
  }

  /**
   * The layer the intersection is on, holding its banned turns
   *
   * @return the layer
   */
  public MacroscopicNetworkLayer getLayer() {
    return (MacroscopicNetworkLayer) getContent()[0];
  }

  /**
   * The intersection collapsed
   *
   * @return the intersection
   */
  public SpatialIntersection getIntersection() {
    return (SpatialIntersection) getContent()[1];
  }

  /**
   * The centre node the intersection is collapsed onto
   *
   * @return the centre node
   */
  public Node getCentreNode() {
    return (Node) getContent()[2];
  }

  /**
   * The member nodes of the intersection when collapse started, the centre node excluded
   *
   * @return the member nodes
   */
  @SuppressWarnings("unchecked")
  public List<Node> getMemberNodes() {
    return (List<Node>) getContent()[3];
  }

  /**
   * The modes collapsed onto the centre node
   *
   * @return the collapsed modes
   */
  @SuppressWarnings("unchecked")
  public List<Mode> getCollapsedModes() {
    return (List<Mode>) getContent()[4];
  }
}
