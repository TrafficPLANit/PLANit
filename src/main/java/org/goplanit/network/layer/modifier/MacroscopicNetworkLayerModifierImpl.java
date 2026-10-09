package org.goplanit.network.layer.modifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.network.layer.macroscopic.intersection.modifier.IntersectionModifierEventProducerImpl;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RecreatedIntersectionsManagedIdsEvent;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RemoveIntersectionEvent;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.UpdateIntersectionsOnBreakLinkSegmentHandler;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.UpdateIntersectionsOnLinkSegmentRemovalHandler;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.UpdateIntersectionsOnVertexRemovalHandler;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventType;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.modifier.ModeAccessCleanupModifierResult;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * Modifier for macroscopic network layers. It keeps the layer's intersections consistent with every graph change it
 * makes, and adds the removal of intersections, with the events that go with it
 *
 * @author markr
 */
public class MacroscopicNetworkLayerModifierImpl
    extends UntypedNetworkLayerModifierImpl<Node, MacroscopicLink, MacroscopicLinkSegment>
    implements MacroscopicNetworkLayerModifier {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetworkLayerModifierImpl.class.getCanonicalName());

  /** the layer this modifier modifies */
  private final MacroscopicNetworkLayerImpl layer;

  /** the intersections of the layer, kept consistent with the changes of this modifier */
  private final IntersectionsImpl intersections;

  /** produces the intersection events to the intersection listeners registered on this modifier */
  private final IntersectionModifierEventProducerImpl intersectionEvents = new IntersectionModifierEventProducerImpl();

  /** keeps intersections consistent with removed nodes, and reports those left without member nodes */
  private final UpdateIntersectionsOnVertexRemovalHandler vertexRemovalHandler;

  /**
   * Log what the modifier changed, unless the caller keeps its own account of it
   *
   * @param message to log
   */
  private void logModification(String message) {
    if (isLogModifications()) {
      LOGGER.info(message);
    }
  }

  /**
   * Remove the intersections left without member nodes by the nodes just removed through this modifier, as reported
   * by the vertex removal handler
   */
  private void removeIntersectionsLeftWithoutMemberNodes() {
    removeIntersections(vertexRemovalHandler.drainIntersectionsLeftWithoutMemberNodes());
  }

  /**
   * Constructor
   *
   * @param layer to modify, whose intersections are kept consistent with the changes of this modifier
   * @param graph of the layer
   */
  public MacroscopicNetworkLayerModifierImpl(
      final MacroscopicNetworkLayerImpl layer,
      final UntypedDirectedGraphImpl<Node, MacroscopicLink, MacroscopicLinkSegment> graph) {
    super(graph);
    this.layer = layer;
    this.intersections = (IntersectionsImpl) layer.getIntersections();
    this.vertexRemovalHandler = new UpdateIntersectionsOnVertexRemovalHandler(intersections);
    graphModifier.addInternalListener(new UpdateIntersectionsOnBreakLinkSegmentHandler(intersections));
    graphModifier.addInternalListener(new UpdateIntersectionsOnLinkSegmentRemovalHandler(intersections));
    graphModifier.addInternalListener(vertexRemovalHandler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeVertex(Node vertex) {
    super.removeVertex(vertex);
    removeIntersectionsLeftWithoutMemberNodes();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingSubnetworks(
      final Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, boolean recreateManagedIds) {
    super.removeDanglingSubnetworks(belowSize, aboveSize, alwaysKeepLargest, false);
    removeIntersectionsLeftWithoutMemberNodes();
    if (recreateManagedIds) {
      recreateManagedIdEntities();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDanglingSubnetworks(
      final Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest, boolean recreateManagedIds,
      Predicate<? super MacroscopicLinkSegment> testEdgeSegment, Connectivity connectivity) {
    super.removeDanglingSubnetworks(belowSize, aboveSize, alwaysKeepLargest, false, testEdgeSegment, connectivity);
    removeIntersectionsLeftWithoutMemberNodes();
    if (recreateManagedIds) {
      recreateManagedIdEntities();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateManagedIdEntities() {
    super.recreateManagedIdEntities();
    intersections.recreateIds(true);
    if (intersectionEvents.hasListener(RecreatedIntersectionsManagedIdsEvent.EVENT_TYPE)) {
      intersectionEvents.fireEvent(new RecreatedIntersectionsManagedIdsEvent(this, intersections));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeIntersection(Intersection intersection) {
    if (intersections.get(intersection.getId()) != intersection) {
      return false;
    }
    intersections.remove(intersection);
    if (intersectionEvents.hasListener(RemoveIntersectionEvent.EVENT_TYPE)) {
      intersectionEvents.fireEvent(new RemoveIntersectionEvent(this, intersection));
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int removeIntersections(Collection<? extends Intersection> intersections) {
    int removed = (int) intersections.stream().filter(this::removeIntersection).count();
    if (removed > 0) {
      logModification(String.format("%s Removed %d intersections",
          LoggingUtils.networkLayerPrefix(layer.getId()), removed));
    }
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int removeIncompleteIntersections(boolean withoutMemberNodes, boolean withoutApproaches) {
    var incomplete = intersections.stream().filter(intersection ->
        (withoutMemberNodes && intersection.getMemberNodes().isEmpty()) ||
            (withoutApproaches && intersection.getApproachSegments().isEmpty())).collect(Collectors.toList());
    var removed = removeIntersections(incomplete);
    intersections.reindex();
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModeAccessCleanupModifierResult removeInfrastructureWithoutModeAccess() {
    /* what is removed along the way is stated by the result, not logged as it goes; intersections go both here and
     * with their nodes, so those removed are the ones present before and missing after */
    List<Intersection> intersectionsBefore = new ArrayList<>(intersections.size());
    intersections.forEach(intersectionsBefore::add);
    List<MacroscopicLinkSegment> unusableSegments;
    List<MacroscopicLink> emptyLinks;
    List<Node> danglingNodes;
    boolean logModifications = isLogModifications();
    setLogModifications(false);
    try {
      /* collected before removing throughout, since removal mutates the containers being iterated */
      unusableSegments = layer.getLinkSegments().stream()
          .filter(linkSegment -> !linkSegment.hasLinkSegmentType() ||
              !linkSegment.getLinkSegmentType().hasAllowedModes())
          .collect(Collectors.toList());
      unusableSegments.forEach(this::removeEdgeSegment);

      emptyLinks = layer.getLinks().stream()
          .filter(link -> !link.hasEdgeSegmentAb() && !link.hasEdgeSegmentBa())
          .collect(Collectors.toList());
      emptyLinks.forEach(this::removeEdge);

      danglingNodes = layer.getNodes().stream()
          .filter(node -> node.getEdges() == null || node.getEdges().isEmpty())
          .collect(Collectors.toList());
      danglingNodes.forEach(this::removeVertex);

      /* an intersection whose approaches all went controls no traffic; one that lost its nodes went with them */
      removeIncompleteIntersections(false, true);
    } finally {
      setLogModifications(logModifications);
    }
    Set<Intersection> remaining = Collections.newSetFromMap(new IdentityHashMap<>());
    intersections.forEach(remaining::add);
    List<Intersection> removedIntersections = intersectionsBefore.stream()
        .filter(intersection -> !remaining.contains(intersection)).collect(Collectors.toList());

    /* a type granting nothing has no purpose once the segments carrying it are gone */
    var unusedTypes = layer.getLinkSegmentTypes().stream()
        .filter(type -> !type.hasAllowedModes())
        .collect(Collectors.toList());
    for (var type : unusedTypes) {
      layer.getLinkSegmentTypes().remove(type);
    }

    return new ModeAccessCleanupModifierResult(
        unusableSegments, emptyLinks, danglingNodes, unusedTypes, removedIntersections);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAllNonInternalListeners() {
    super.removeAllNonInternalListeners();
    intersectionEvents.removeAllNonInternalListeners();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(IntersectionModifierListener listener) {
    intersectionEvents.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(IntersectionModifierListener listener, IntersectionModifierEventType eventType) {
    intersectionEvents.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(IntersectionModifierListener listener, IntersectionModifierEventType eventType) {
    intersectionEvents.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(IntersectionModifierListener listener) {
    intersectionEvents.removeListener(listener);
  }
}
