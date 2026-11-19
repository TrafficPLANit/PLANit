package org.goplanit.zoning.connectoid;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.goplanit.graph.modifier.event.RemoveVertexEvent;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.CollapseSpatialIntersectionEvent;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.DirectedGraphModifierListener;
import org.goplanit.utils.graph.modifier.event.GraphModificationEvent;
import org.goplanit.utils.graph.modifier.event.GraphModifierListener;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.zoning.connectoid.Connectoid;

/**
 * Keeps the connectoids of a container in step with the network of the zoning holding it: a removed node removes the
 * connectoids at it, through the zoning modifier, so each loss is reported
 * <p>
 * Connectoids join zones to a network that changes after they are made: nodes and link segments are removed or
 * replaced. So that they never refer to a node that is gone, the handler listens, as an internal listener, to the
 * modifier of each layer of the networks the zoning is attached to, and finds the connectoids affected by their access
 * node. A node collapse removes from an intersection it collapses is the exception: its connectoids move to the
 * intersection's centre node once the collapse is done, since the centre then stands for the whole intersection, and a
 * move is not a loss. What following a collapse needs to remember lives here, so the container stays a container.
 * </p>
 *
 * @author markr
 * @param <T> type of connectoid
 * @param <C> type of the container
 */
public class UpdateConnectoidsOnNetworkChangeHandler<T extends Connectoid, C extends ConnectoidsImpl<T>>
    implements DirectedGraphModifierListener, IntersectionModifierListener {

  /** the container whose connectoids are kept in step */
  private final C connectoids;

  /** the member nodes of the intersections being collapsed, from their START to their DONE, by identity */
  private final Set<DirectedVertex> intersectionMemberNodesBeingCollapsed =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /** the member nodes of intersections being collapsed that collapse removed, by identity, whose connectoids are kept
   * until the collapse is done */
  private final Set<DirectedVertex> memberNodesRemovedWhileCollapsing =
      Collections.newSetFromMap(new IdentityHashMap<>());

  /**
   * Move the connectoids at the member nodes collapse removed from an intersection it is done with to its centre node,
   * and stop keeping the intersection's member nodes, so one removed later is removed with its connectoids
   *
   * @param done the event reporting the intersection as done
   */
  private void moveConnectoidsAtRemovedMemberNodesToCentre(CollapseSpatialIntersectionEvent done) {
    for (var memberNode : done.getMemberNodes()) {
      intersectionMemberNodesBeingCollapsed.remove(memberNode);
      if (memberNodesRemovedWhileCollapsing.remove(memberNode)) {
        List.copyOf(connectoids.getByAccessNode(memberNode))
            .forEach(connectoid -> connectoid.setReferenceVertex(done.getCentreNode()));
      }
    }
  }

  /**
   * The container whose connectoids are kept in step
   *
   * @return the container
   */
  protected C getConnectoids() {
    return connectoids;
  }

  /**
   * Constructor
   *
   * @param connectoids container whose connectoids to keep in step with the network
   */
  public UpdateConnectoidsOnNetworkChangeHandler(final C connectoids) {
    this.connectoids = connectoids;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EventType[] getKnownSupportedEventTypes() {
    return new EventType[] { RemoveVertexEvent.EVENT_TYPE, CollapseSpatialIntersectionEvent.EVENT_TYPE };
  }

  /**
   * Listen, as an internal listener, to the graph and intersection changes made through a layer modifier
   *
   * @param layerModifier to listen to
   */
  public void listenTo(MacroscopicNetworkLayerModifier layerModifier) {
    layerModifier.addInternalListener((GraphModifierListener) this);
    layerModifier.addInternalListener((IntersectionModifierListener) this);
  }

  /**
   * Stop listening to a layer modifier
   *
   * @param layerModifier to stop listening to
   */
  public void stopListeningTo(MacroscopicNetworkLayerModifier layerModifier) {
    layerModifier.removeInternalListener((GraphModifierListener) this);
    layerModifier.removeInternalListener((IntersectionModifierListener) this);
  }

  /**
   * Remove the connectoids at a removed node, each whole, through the zoning modifier, unless collapse removed the node
   * from an intersection it is collapsing: those are kept until the collapse is done
   *
   * @param event of the graph change
   */
  @Override
  public void onGraphModificationEvent(GraphModificationEvent event) {
    if (event.getType().equals(RemoveVertexEvent.EVENT_TYPE)) {
      var removed = (DirectedVertex) ((RemoveVertexEvent) event).getRemovedVertex();
      if (intersectionMemberNodesBeingCollapsed.contains(removed)) {
        memberNodesRemovedWhileCollapsing.add(removed);
      } else {
        connectoids.getByAccessNode(removed).forEach(connectoids.getZoningModifier()::removeConnectoid);
      }
    }
  }

  /**
   * Changes to link segments concern the connectoids reached over them, see the transfer connectoids' handler
   *
   * @param event of the graph change
   */
  @Override
  public void onDirectedGraphModificationEvent(DirectedGraphModificationEvent event) {
  }

  /**
   * Follow the collapse of a spatial intersection: when it starts, keep the connectoids at its member nodes should
   * collapse remove one, and when it is done, move those at a removed member node to its centre node
   *
   * @param event of the intersection change
   */
  @Override
  public void onIntersectionModifierEvent(IntersectionModificationEvent event) {
    if (!event.getType().equals(CollapseSpatialIntersectionEvent.EVENT_TYPE)) {
      return;
    }
    var collapse = (CollapseSpatialIntersectionEvent) event;
    switch (collapse.getState().orElseThrow()) {
      case START:
        intersectionMemberNodesBeingCollapsed.addAll(collapse.getMemberNodes());
        break;
      case DONE:
        moveConnectoidsAtRemovedMemberNodesToCentre(collapse);
        break;
    }
  }
}
