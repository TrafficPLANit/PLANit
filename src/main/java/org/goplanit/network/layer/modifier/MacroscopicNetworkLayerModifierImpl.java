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

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.graph.directed.UntypedDirectedGraphImpl;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.RecreatedIntersectionsManagedIdsEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.RemoveIntersectionEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.RemoveSpatialMovementEvent;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.UpdateIntersectionsOnBreakLinkSegmentHandler;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.UpdateIntersectionsOnLinkSegmentRemovalHandler;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.UpdateIntersectionsOnVertexRemovalHandler;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.RecreatedTollRoadsManagedIdsEvent;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.RemoveTollRoadEvent;
import org.goplanit.network.layer.macroscopic.tollroad.modifier.event.handler.UpdateTollRoadsOnBreakLinkSegmentHandler;
import org.goplanit.network.layer.macroscopic.tollroad.modifier.event.handler.UpdateTollRoadsOnLinkSegmentRemovalHandler;
import org.goplanit.network.layer.modifier.consolidator.IntersectionConsolidator;
import org.goplanit.network.layer.modifier.consolidator.IntersectionModeCollapser;
import org.goplanit.utils.network.layer.modifier.event.ChangeLinkSegmentTypeEvent;
import org.goplanit.utils.network.layer.modifier.event.RemoveLinkSegmentTypeEvent;
import org.goplanit.utils.containers.ContainerUtils;
import org.goplanit.utils.event.EventType;
import org.goplanit.utils.event.TypedEventProducer;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.BannedTurn;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventType;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModifierEventType;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModifierListener;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionCollapseModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionCollapserSettings;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModificationEvent;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierEventType;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierListener;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.modifier.ModeAccessCleanupModifierResult;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.locationtech.jts.geom.Coordinate;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventProducer;
import org.goplanit.utils.network.layer.modifier.event.LinkSegmentTypeModifierEventProducer;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModifierEventProducer;

/**
 * Modifier for macroscopic network layers. It keeps the layer's intersections and toll roads consistent with every
 * graph change it makes, and adds the removal of intersections, with the events that go with it
 * <p>
 * It does so through internal listeners on its graph modifier, one per kind of change: for example, when a link
 * segment is removed, one takes it off its intersection and removes the movements over it, and another takes it off its
 * toll road and has a toll road left empty removed. Internal listeners hear each change before any listener registered
 * from outside, so a zoning or a network pricing following the layer finds it consistent already. It holds one event
 * producer per kind of event it fires (intersections, link segment types, toll roads), so a listener of several kinds
 * hears each event once.
 * </p>
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

  /** produces the intersection events to the intersection listeners registered on this modifier */
  private final TypedEventProducer<IntersectionModifierListener, IntersectionModificationEvent,
      IntersectionModifierEventType> intersectionEvents =
      new TypedEventProducer<>(IntersectionModifierListener::onIntersectionModifierEvent,
          IntersectionModifierEventProducer.FIRED_INTERSECTION_EVENT_TYPES);

  /** produces the link segment type events to the link segment type listeners registered on this modifier */
  private final TypedEventProducer<LinkSegmentTypeModifierListener, LinkSegmentTypeModificationEvent,
      LinkSegmentTypeModifierEventType> linkSegmentTypeEvents =
      new TypedEventProducer<>(LinkSegmentTypeModifierListener::onLinkSegmentTypeModifierEvent,
          LinkSegmentTypeModifierEventProducer.FIRED_LINK_SEGMENT_TYPE_EVENT_TYPES);

  /** produces the toll road events to the toll road listeners registered on this modifier */
  private final TypedEventProducer<TollRoadModifierListener, TollRoadModificationEvent,
      TollRoadModifierEventType> tollRoadEvents =
      new TypedEventProducer<>(TollRoadModifierListener::onTollRoadModifierEvent,
          TollRoadModifierEventProducer.FIRED_TOLL_ROAD_EVENT_TYPES);

  /** keeps intersections consistent with removed nodes, and reports those left without member nodes */
  private final UpdateIntersectionsOnVertexRemovalHandler vertexRemovalHandler;

  /**
   * Remove the intersections left without member nodes by the nodes just removed through this modifier, as reported
   * by the vertex removal handler
   */
  private void removeIntersectionsLeftWithoutMemberNodes() {
    removeIntersections(vertexRemovalHandler.drainIntersectionsLeftWithoutMemberNodes());
    vertexRemovalHandler.drainSpatialIntersectionsLeftWithOneMemberNode()
        .forEach(this::replaceByNonSpatialIntersection);
  }

  /**
   * Replace a spatial intersection left with one member node by a non-spatial intersection on that node. Its control
   * type, kinds, external id, and approaches with their signals, green ratios and saturation flow factors carry over.
   * Each of its movements with a direct ban becomes a banned turn at the node, banning the same modes; the banned turns
   * the node already has stay. The spatial intersection and its movements are removed with their removal events
   *
   * @param spatial the spatial intersection to replace
   */
  private void replaceByNonSpatialIntersection(SpatialIntersection spatial) {
    var node = spatial.getMemberNodes().get(0);
    var types = List.copyOf(spatial.getTypes());
    var nonSpatial =
        layer.getIntersections().getFactory().createNonSpatial(node, spatial.getControlType(), types.get(0));
    types.forEach(nonSpatial::addType);
    nonSpatial.setExternalId(spatial.getExternalId());
    spatial.getApproaches().forEach(approach -> IntersectionUtils.addCopyOfApproach(nonSpatial, approach));
    for (var movement : spatial.getMovements()) {
      if (movement.hasDirectBan() && movement.getLinkSegmentFrom().getDownstreamVertex() == node &&
          movement.getLinkSegmentTo().getUpstreamVertex() == node) {
        MacroscopicNetworkLayerUtils.banTurnForModes(layer, movement.getLinkSegmentFrom(), movement.getLinkSegmentTo(),
            movement.getExplicitlyDirectlyBannedModes());
      }
    }
    removeIntersection(spatial);
    layer.getIntersections().getFactory().register(nonSpatial);
  }

  /**
   * Remove a toll road left without link segments from the layer, firing its removal event
   *
   * @param tollRoad to remove
   */
  private void removeTollRoad(TollRoad tollRoad) {
    layer.getTollRoads().remove(tollRoad);
    if (tollRoadEvents.hasListener(RemoveTollRoadEvent.EVENT_TYPE)) {
      tollRoadEvents.fireEvent(new RemoveTollRoadEvent(this, tollRoad));
    }
  }

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
   * Remove a banned turn from the layer, firing its removal event. For the collapse run by this modifier
   *
   * @param bannedTurn to remove
   */
  public void removeBannedTurn(BannedTurn bannedTurn) {
    graphModifier.removeBannedTurn(bannedTurn);
  }

  /**
   * Constructor
   *
   * @param layer to modify, whose intersections and toll roads are kept consistent with the changes of this modifier
   * @param graph of the layer
   */
  public MacroscopicNetworkLayerModifierImpl(
      final MacroscopicNetworkLayerImpl layer,
      final UntypedDirectedGraphImpl<Node, MacroscopicLink, MacroscopicLinkSegment> graph) {
    super(graph);
    this.layer = layer;
    this.vertexRemovalHandler = new UpdateIntersectionsOnVertexRemovalHandler(layer.getIntersections());
    graphModifier.addInternalListener(
        new UpdateIntersectionsOnBreakLinkSegmentHandler(layer.getIntersections(), layer.getSpatialMovements()));
    graphModifier.addInternalListener(new UpdateIntersectionsOnLinkSegmentRemovalHandler(
        layer.getIntersections(), layer.getSpatialMovements(), this::removeSpatialMovement));
    graphModifier.addInternalListener(vertexRemovalHandler);
    graphModifier.addInternalListener(new UpdateTollRoadsOnBreakLinkSegmentHandler(layer.getTollRoads()));
    graphModifier.addInternalListener(
        new UpdateTollRoadsOnLinkSegmentRemovalHandler(layer.getTollRoads(), this::removeTollRoad));
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
    layer.getIntersections().recreateIds(true);
    layer.getSpatialMovements().recreateIds(true);
    layer.getTollRoads().recreateIds(true);
    if (tollRoadEvents.hasListener(RecreatedTollRoadsManagedIdsEvent.EVENT_TYPE)) {
      tollRoadEvents.fireEvent(new RecreatedTollRoadsManagedIdsEvent(this, layer.getTollRoads()));
    }
    if (intersectionEvents.hasListener(RecreatedIntersectionsManagedIdsEvent.EVENT_TYPE)) {
      intersectionEvents.fireEvent(
          new RecreatedIntersectionsManagedIdsEvent(this, layer.getIntersections(), layer.getSpatialMovements()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeIntersection(Intersection intersection) {
    if (!layer.getIntersections().containsValue(intersection)) {
      return false;
    }
    if (intersection instanceof SpatialIntersection) {
      List.copyOf(((SpatialIntersection) intersection).getMovements()).forEach(this::removeSpatialMovement);
    }
    layer.getIntersections().remove(intersection);
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
    var incomplete = layer.getIntersections().stream().filter(intersection ->
        (withoutMemberNodes && intersection.getMemberNodes().isEmpty()) ||
            (withoutApproaches && intersection.getApproachSegments().isEmpty())).collect(Collectors.toList());
    var removed = removeIntersections(incomplete);
    layer.getIntersections().reindex();
    return removed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeSpatialMovement(SpatialMovement spatialMovement) {
    var spatialMovements = layer.getSpatialMovements();
    if (!spatialMovements.containsValue(spatialMovement)) {
      return false;
    }
    var intersection = layer.getIntersections().getBySegment(spatialMovement.getLinkSegmentFrom());
    if (intersection instanceof SpatialIntersection) {
      ((SpatialIntersection) intersection).removeMovement(spatialMovement);
    }
    spatialMovements.remove(spatialMovement);
    if (intersectionEvents.hasListener(RemoveSpatialMovementEvent.EVENT_TYPE)) {
      intersectionEvents.fireEvent(new RemoveSpatialMovementEvent(this, spatialMovement));
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ModeAccessCleanupModifierResult removeInfrastructureWithoutModeAccess() {
    /* what is removed along the way is stated by the result, not logged as it goes; intersections go both here and
     * with their nodes, so those removed are the ones present before and missing after */
    List<Intersection> intersectionsBefore = new ArrayList<>(layer.getIntersections().size());
    layer.getIntersections().forEach(intersectionsBefore::add);
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
    layer.getIntersections().forEach(remaining::add);
    List<Intersection> removedIntersections = intersectionsBefore.stream()
        .filter(intersection -> !remaining.contains(intersection)).collect(Collectors.toList());

    /* a type granting nothing has no purpose once the segments carrying it are gone */
    var unusedTypes = layer.getLinkSegmentTypes().stream()
        .filter(type -> !type.hasAllowedModes())
        .collect(Collectors.toList());
    for (var type : unusedTypes) {
      layer.getLinkSegmentTypes().remove(type);
      if (linkSegmentTypeEvents.hasListener(RemoveLinkSegmentTypeEvent.EVENT_TYPE)) {
        linkSegmentTypeEvents.fireEvent(new RemoveLinkSegmentTypeEvent(this, type));
      }
    }

    return new ModeAccessCleanupModifierResult(
        unusableSegments, emptyLinks, danglingNodes, unusedTypes, removedIntersections);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void changeLinkSegmentType(MacroscopicLinkSegment linkSegment, MacroscopicLinkSegmentType linkSegmentType) {
    var previousType = linkSegment.getLinkSegmentType();
    if (previousType == linkSegmentType) {
      return;
    }
    linkSegment.setLinkSegmentType(linkSegmentType);
    if (linkSegmentTypeEvents.hasListener(ChangeLinkSegmentTypeEvent.EVENT_TYPE)) {
      linkSegmentTypeEvents.fireEvent(new ChangeLinkSegmentTypeEvent(this, linkSegment, previousType, linkSegmentType));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<BannedTurn> moveLinkEndsAtNode(Collection<? extends MacroscopicLink> links,
      Node currentEndNode, Node newEndNode, CoordinateReferenceSystem crs) {
    /* each geometry runs on in a straight line to the new node, set before the move so its event sees finished links */
    var crsUtils = new PlanitJtsCrsUtils(crs);
    var newPosition = newEndNode.getPosition().getCoordinate();
    for (var link : links) {
      boolean atNodeA = link.getNodeA() == currentEndNode;
      PlanItRunTimeException.throwIf(!atNodeA && link.getNodeB() != currentEndNode,
          "Node %s is not an end of link %s", currentEndNode.getIdsAsString(), link.getIdsAsString());
      var coordinates = new ArrayList<>(link.hasGeometry() ? List.of(link.getGeometry().getCoordinates()) :
          List.of(link.getNodeA().getPosition().getCoordinate(), link.getNodeB().getPosition().getCoordinate()));
      if (atNodeA) {
        coordinates.add(0, newPosition);
      } else {
        coordinates.add(newPosition);
      }
      link.setGeometry(PlanitJtsUtils.createLineString(coordinates.toArray(Coordinate[]::new)));
      link.setLengthKm(crsUtils.getDistanceInKilometres(link.getGeometry()));
    }
    graphModifier.moveEdgeEndsAtVertex(links, currentEndNode, newEndNode);

    /* the links' segments now meet other segments at the new end */
    var bansNoLongerMeeting = new ArrayList<BannedTurn>();
    for (var link : links) {
      for (var segment : link.getEdgeSegments()) {
        layer.getBannedTurns().getBySegment(segment).stream()
            .filter(ban -> !ban.hasCentreVertex() && !bansNoLongerMeeting.contains(ban))
            .forEach(bansNoLongerMeeting::add);
      }
    }
    bansNoLongerMeeting.forEach(graphModifier::removeBannedTurn);
    return bansNoLongerMeeting;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionConsolidationModifierResult consolidateIntersections(
      CoordinateReferenceSystem crs,
      IntersectionConsolidatorSettings settings,
      Predicate<LinkSegment> hasSignalOfItsOwn) {
    return new IntersectionConsolidator(layer, crs, settings, hasSignalOfItsOwn).consolidate();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionCollapseModifierResult collapseIntersectionsToNonSpatialForModes(
      Collection<? extends SpatialIntersection> intersections, IntersectionCollapserSettings settings,
      CoordinateReferenceSystem crs) {
    return new IntersectionModeCollapser(layer, settings, crs, intersectionEvents).collapse(intersections);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAllNonInternalListeners() {
    super.removeAllNonInternalListeners();
    intersectionEvents.removeAllNonInternalListeners();
    linkSegmentTypeEvents.removeAllNonInternalListeners();
    tollRoadEvents.removeAllNonInternalListeners();
  }

  /**
   * The event types this modifier fires, of every kind: the graph and directed graph types of its graph modifier, and
   * its intersection, link segment type and toll road types
   *
   * @return unmodifiable set of the event types fired
   */
  @Override
  public Set<EventType> getFiredEventTypes() {
    return ContainerUtils.unmodifiableUnionOf(super.getFiredEventTypes(), intersectionEvents.getFiredEventTypes(),
        linkSegmentTypeEvents.getFiredEventTypes(), tollRoadEvents.getFiredEventTypes());
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void addInternalListener(IntersectionModifierListener listener) {
    intersectionEvents.addInternalListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeInternalListener(IntersectionModifierListener listener) {
    intersectionEvents.removeInternalListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(LinkSegmentTypeModifierListener listener) {
    linkSegmentTypeEvents.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(LinkSegmentTypeModifierListener listener, LinkSegmentTypeModifierEventType eventType) {
    linkSegmentTypeEvents.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(LinkSegmentTypeModifierListener listener, LinkSegmentTypeModifierEventType eventType) {
    linkSegmentTypeEvents.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(LinkSegmentTypeModifierListener listener) {
    linkSegmentTypeEvents.removeListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addInternalListener(LinkSegmentTypeModifierListener listener) {
    linkSegmentTypeEvents.addInternalListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeInternalListener(LinkSegmentTypeModifierListener listener) {
    linkSegmentTypeEvents.removeInternalListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(TollRoadModifierListener listener) {
    tollRoadEvents.addListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(TollRoadModifierListener listener, TollRoadModifierEventType eventType) {
    tollRoadEvents.addListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(TollRoadModifierListener listener, TollRoadModifierEventType eventType) {
    tollRoadEvents.removeListener(listener, eventType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(TollRoadModifierListener listener) {
    tollRoadEvents.removeListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addInternalListener(TollRoadModifierListener listener) {
    tollRoadEvents.addInternalListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeInternalListener(TollRoadModifierListener listener) {
    tollRoadEvents.removeInternalListener(listener);
  }
}
