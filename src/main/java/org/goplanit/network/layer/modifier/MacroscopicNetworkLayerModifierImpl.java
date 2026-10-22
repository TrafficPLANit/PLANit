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
import org.goplanit.network.layer.macroscopic.intersection.modifier.IntersectionModifierEventProducerImpl;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RecreatedIntersectionsManagedIdsEvent;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RemoveIntersectionEvent;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RemoveSpatialMovementEvent;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.UpdateIntersectionsOnBreakLinkSegmentHandler;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.UpdateIntersectionsOnLinkSegmentRemovalHandler;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler.UpdateIntersectionsOnVertexRemovalHandler;
import org.goplanit.network.layer.modifier.consolidator.IntersectionConsolidator;
import org.goplanit.network.layer.modifier.consolidator.IntersectionModeCollapser;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.BannedTurn;
import org.goplanit.utils.graph.directed.Connectivity;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierEventType;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionCollapseModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.modifier.ModeAccessCleanupModifierResult;
import org.goplanit.utils.network.layer.physical.Node;
import org.locationtech.jts.geom.Coordinate;

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

  /** produces the intersection events to the intersection listeners registered on this modifier */
  private final IntersectionModifierEventProducerImpl intersectionEvents = new IntersectionModifierEventProducerImpl();

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
   * Remove a banned turn from the layer, firing its removal event. For the collapse run by this modifier
   *
   * @param bannedTurn to remove
   */
  public void removeBannedTurn(BannedTurn bannedTurn) {
    graphModifier.removeBannedTurn(bannedTurn);
  }

  /**
   * Replace a spatial intersection left with one member node by a non-spatial intersection on that node. Its control
   * type, kinds, external id, approaches and their signals carry over. Each of its movements with a direct ban becomes
   * a banned turn at the node, banning the same modes; the banned turns the node already has stay. The spatial
   * intersection and its movements are removed with their removal events
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
    for (var approach : spatial.getApproachSegments()) {
      nonSpatial.addApproachSegment(approach);
      nonSpatial.setApproachSegmentHasOwnSignal(approach, spatial.hasOwnSignal(approach));
    }
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
    this.vertexRemovalHandler = new UpdateIntersectionsOnVertexRemovalHandler(layer.getIntersections());
    graphModifier.addInternalListener(
        new UpdateIntersectionsOnBreakLinkSegmentHandler(layer.getIntersections(), layer.getSpatialMovements()));
    graphModifier.addInternalListener(new UpdateIntersectionsOnLinkSegmentRemovalHandler(
        layer.getIntersections(), layer.getSpatialMovements(), this::removeSpatialMovement));
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
    layer.getIntersections().recreateIds(true);
    layer.getSpatialMovements().recreateIds(true);
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
    if (layer.getIntersections().get(intersection.getId()) != intersection) {
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
    if (spatialMovements.get(spatialMovement.getId()) != spatialMovement) {
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
    }

    return new ModeAccessCleanupModifierResult(
        unusableSegments, emptyLinks, danglingNodes, unusedTypes, removedIntersections);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<BannedTurn> moveLinkEndNode(
      MacroscopicLink link, Node currentEndNode, Node newEndNode, CoordinateReferenceSystem crs) {
    boolean atNodeA = link.getNodeA() == currentEndNode;
    PlanItRunTimeException.throwIf(!atNodeA && link.getNodeB() != currentEndNode,
        "Node %s is not an end of link %s", currentEndNode.getIdsAsString(), link.getIdsAsString());

    var coordinates = new ArrayList<>(link.hasGeometry() ? List.of(link.getGeometry().getCoordinates()) :
        List.of(link.getNodeA().getPosition().getCoordinate(), link.getNodeB().getPosition().getCoordinate()));
    var newPosition = newEndNode.getPosition().getCoordinate();
    if (atNodeA) {
      coordinates.add(0, newPosition);
    } else {
      coordinates.add(newPosition);
    }
    link.replace(currentEndNode, newEndNode);
    currentEndNode.removeEdge(link);
    newEndNode.addEdge(link);
    link.setGeometry(PlanitJtsUtils.createLineString(coordinates.toArray(Coordinate[]::new)));
    link.setLengthKm(new PlanitJtsCrsUtils(crs).getDistanceInKilometres(link.getGeometry()));

    /* the link's segments now meet other segments at the new end */
    var bansNoLongerMeeting = new ArrayList<BannedTurn>();
    for (var segment : link.getEdgeSegments()) {
      layer.getBannedTurns().getBySegment(segment).stream()
          .filter(ban -> !ban.hasCentreVertex() && !bansNoLongerMeeting.contains(ban))
          .forEach(bansNoLongerMeeting::add);
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
      Predicate<MacroscopicLinkSegment> hasSignalOfItsOwn) {
    return new IntersectionConsolidator(layer, crs, settings, hasSignalOfItsOwn).consolidate();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionCollapseModifierResult collapseIntersectionsToNonSpatialForModes(
      Collection<? extends SpatialIntersection> intersections, Collection<Mode> modes, CoordinateReferenceSystem crs) {
    return new IntersectionModeCollapser(layer, modes, crs).collapse(intersections);
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
}
