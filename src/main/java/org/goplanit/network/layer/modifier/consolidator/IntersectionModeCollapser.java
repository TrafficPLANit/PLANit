package org.goplanit.network.layer.modifier.consolidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.network.layer.macroscopic.ModifiedLinkSegmentTypes;
import org.goplanit.network.layer.macroscopic.SpatialMovementUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitGraphGeoUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.directed.BannedTurn;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.id.IdMappingUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionCollapseModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionCollapserSettings;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.LinkUtils;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.NodeUtils;

/**
 * Collapses spatial intersections onto one node each for some modes, so for those modes each becomes in effect a
 * non-spatial intersection on its centre node (C8). Changes are made through the layer modifier, so each removal fires
 * its event. Used once per collapse, run through
 * {@link MacroscopicNetworkLayerModifier#collapseIntersectionsToNonSpatialForModes}, not on its own.
 *
 * @author markr
 */
public class IntersectionModeCollapser {

  /**
   * The link added for a collapsed mode from the centre node to a member node, see {@link #addLinkToCentreNode}. It is
   * the one link between the two that allows the mode, since the links moved to the centre end outside the intersection
   *
   * @param centre node of the intersection
   * @param memberNode the link joins the centre to
   * @param mode collapsed, the only mode the link allows
   * @return the link, null when none was added
   */
  private static Link findLinkToCentreNode(Node centre, Node memberNode, Mode mode) {
    return centre.getEdges(memberNode).stream().map(Link.class::cast)
        .filter(link -> LinkUtils.hasSegmentMatching(link, segment -> segment.isModeAllowed(mode)))
        .findFirst().orElse(null);
  }

  /** the layer whose spatial intersections are collapsed */
  private final MacroscopicNetworkLayerImpl layer;

  /** the modes to collapse onto the centre nodes, those of the settings */
  private final List<Mode> modes;

  /** the modes to collapse and the nodes excluded from collapse, a copy of the settings given */
  private final IntersectionCollapserSettings settings;

  /** to measure distances with, in the layer's coordinate reference system */
  private final PlanitJtsCrsUtils crsUtils;

  /** what was done, filled where each change is made */
  private final IntersectionCollapseModifierResult result = new IntersectionCollapseModifierResult();

  /**
   * Add the centre node of an intersection at the centroid of its member nodes, and make it a member
   *
   * @param intersection to add the centre node to
   * @return the centre node
   */
  private Node addCentreNode(SpatialIntersection intersection) {
    var centroid = PlanitGraphGeoUtils.createCentroid(intersection.getMemberNodes());
    var centre = layer.getNodes().getFactory().registerNew();
    centre.setPosition(centroid);
    centre.setXmlId(String.valueOf(centre.getId()));
    layer.getIntersections().update(intersection, i -> i.addMemberNode(centre));
    return centre;
  }

  /**
   * Add a link from the centre node to a member node, in both directions, for one mode only. Both segments are
   * internal. They take the type of the kept road the link extends: that type itself when it allows only the mode, and
   * otherwise a modified copy of it without the other modes. So no new type is made up, and no type is changed in place
   *
   * @param intersection the centre node and member node belong to
   * @param centre node of the intersection
   * @param memberNode to join it to
   * @param mode to allow
   * @param keptRoadSegment a segment of a kept road at the member node that allows the mode, whose type the link takes
   * @param modifiedTypes the modified copies of types made so far, reused when the same copy is needed again
   */
  private void addLinkToCentreNode(SpatialIntersection intersection, Node centre, Node memberNode, Mode mode,
      MacroscopicLinkSegment keptRoadSegment, ModifiedLinkSegmentTypes modifiedTypes) {
    /* the type of the kept road, without its other modes */
    var keptType = keptRoadSegment.getLinkSegmentType();
    var otherModes = new HashSet<>(keptType.getAllowedModes());
    otherModes.remove(mode);
    var type = MacroscopicNetworkLayerUtils.findOrRegisterTypeWithoutModes(layer, modifiedTypes, keptType, otherModes);
    /* a straight link from the centre to the member node, with a segment each way, both internal */
    var link = layer.getLinks().getFactory().registerNew(
        centre, memberNode, crsUtils.getDistanceInKilometres(centre, memberNode), true);
    link.setGeometry(PlanitJtsUtils.createLineString(
        centre.getPosition().getCoordinate(), memberNode.getPosition().getCoordinate()));
    link.setXmlId(String.valueOf(link.getId()));
    for (boolean directionAb : new boolean[] {true, false}) {
      var segment = layer.getLinkSegments().getFactory().registerNew(link, directionAb, true);
      segment.setLinkSegmentType(type);
      segment.setXmlId(String.valueOf(segment.getId()));
      layer.getIntersections().update(intersection, i -> i.addNewInternalSegment(segment));
    }
    result.addLinkAddedToCentreNode(link);
  }

  /**
   * Collapse one spatial intersection: a centre node is added, the internal segments lose access for the collapsed
   * modes, links used only by them move their member node end to the centre, and a link for each of them joins the
   * centre to each member node where a kept road still allows it. The collapsed modes are banned on the new turns so
   * each can make only what it could make while the intersection was spatial (C8 step 6)
   *
   * @param intersection to collapse
   * @param modifiedTypes the modified copies of types made so far, reused when the same copy is needed again
   */
  private void collapse(SpatialIntersection intersection, ModifiedLinkSegmentTypes modifiedTypes) {
    /* what the collapsed modes could do while spatial, worked out before anything changes, and the centre node */
    var memberNodes = List.copyOf(intersection.getMemberNodes());
    var collapsedModesBannedOfMovement = findCollapsedModesBannedOfMovements(intersection);
    var exitsReachedFromEntriesThatAreNotApproaches = findExitsReachedFromEntriesThatAreNotApproaches(intersection);
    var centre = addCentreNode(intersection);
    /* the internal segments lose access for the collapsed modes. Each is a link segment of the layer: a connectoid
     * segment has a centroid at one end, so it is never internal */
    for (var segment : List.copyOf(intersection.getInternalSegments())) {
      modes.stream().filter(segment::isModeAllowed).forEach(mode -> MacroscopicNetworkLayerUtils.withdrawModeAccess(
          layer, modifiedTypes, (MacroscopicLinkSegment) segment, mode));
    }
    /* links leaving the intersection that only collapsed modes use move their member node end to the centre */
    for (var memberNode : memberNodes) {
      /* links of the layer only: a connectoid link of an integrated zoning belongs to the zoning */
      for (var link : memberNode.getLinks().stream().filter(MacroscopicLink.class::isInstance)
          .map(MacroscopicLink.class::cast).collect(Collectors.toList())) {
        if (!intersection.hasMemberNode(link.getOtherNode(memberNode)) &&
            LinkUtils.isUsedOnlyByModes(link, modes)) {
          layer.getLayerModifier().moveLinkEndNode(link, memberNode, centre, crsUtils.getCoordinateReferenceSystem())
              .forEach(result::addRemovedBannedTurn);
          result.addLinkMovedToCentreNode(link);
        }
      }
    }
    /* a link per collapsed mode joins the centre to each member node where a kept road still allows that mode */
    for (var mode : modes) {
      for (var memberNode : memberNodes) {
        var keptRoadSegment = memberNode.getLinks().stream().filter(MacroscopicLink.class::isInstance)
            .map(MacroscopicLink.class::cast).filter(link -> !intersection.hasMemberNode(link.getOtherNode(memberNode)))
            .flatMap(link -> link.getLinkSegments().stream()).map(MacroscopicLinkSegment.class::cast)
            .filter(segment -> segment.isModeAllowed(mode)).min(IdAbleUtils.byId()).orElse(null);
        if (keptRoadSegment != null) {
          addLinkToCentreNode(intersection, centre, memberNode, mode, keptRoadSegment, modifiedTypes);
        }
      }
    }
    /* the collapsed modes keep only the movements they could make while the intersection was spatial */
    restrictNewTurnsToMovementsPossibleWhileSpatial(
        intersection, centre, collapsedModesBannedOfMovement, exitsReachedFromEntriesThatAreNotApproaches);
  }

  /**
   * For each movement of a spatial intersection, the collapsed modes that have access to its from and to segments but
   * could not make it, see {@link SpatialMovementUtils#findBannedModes}: the movement is banned for the mode directly,
   * every connection for the mode passes a turn banned at a member node, or every connection passes an inner link the
   * mode may not use. In the last case no ban says so at all, such as for an inner link that is one-way for cars and
   * two-way for pedestrians, so every movement is worked out. Worked out before collapse withdraws access, while the
   * intersection is still spatial
   *
   * @param intersection to work out
   * @return the collapsed modes banned, per movement with at least one
   */
  private Map<SpatialMovement, List<Mode>> findCollapsedModesBannedOfMovements(SpatialIntersection intersection) {
    /* the collapsed modes each movement is banned for */
    var bannedOfMovement = new LinkedHashMap<SpatialMovement, List<Mode>>();
    for (var movement : intersection.getMovements()) {
      var bannedModes = SpatialMovementUtils.findBannedModes(layer, intersection, movement, true, true);
      var banned = modes.stream().filter(bannedModes::contains).collect(Collectors.toList());
      if (!banned.isEmpty()) {
        bannedOfMovement.put(movement, banned);
      }
    }
    return bannedOfMovement;
  }

  /**
   * For each collapsed mode, the exits each segment entering a member node from outside that is not an approach could
   * reach while the intersection is spatial, banned turns respected, see
   * {@link MacroscopicNetworkLayerUtils#findExitsReachedThroughSpatialIntersection}. No movement starts from such a
   * segment, such as a footway entering a signalised junction whose approaches come from its vehicle signals, so this
   * is what the mode could do from it. Worked out before collapse withdraws access
   *
   * @param intersection to work out
   * @return per collapsed mode, each entering segment that is not an approach with an exit it could reach
   */
  private Map<Mode, List<Pair<LinkSegment, LinkSegment>>>
      findExitsReachedFromEntriesThatAreNotApproaches(SpatialIntersection intersection) {
    var exitsReached = new LinkedHashMap<Mode, List<Pair<LinkSegment, LinkSegment>>>();
    for (var entry : IntersectionUtils.<LinkSegment>findSegmentsEnteringFromOutsideThatAreNotApproaches(
        intersection, segment -> true)) {
      for (var mode : modes) {
        if (!entry.isModeAllowed(mode)) {
          continue;
        }
        MacroscopicNetworkLayerUtils.findExitsReachedThroughSpatialIntersection(
            layer, intersection, entry, mode, true)
            .keySet().
            forEach(exit ->
                exitsReached.computeIfAbsent(mode, key -> new ArrayList<>()).add(Pair.of(entry, exit)));
      }
    }
    return exitsReached;
  }

  /**
   * The turns a collapsed mode now makes to go from one segment of a collapsed intersection to another, such as from a
   * movement's from segment to its to segment, once the arms used only by collapsed modes end at the centre node and
   * links join the centre to the member nodes: at the centre, and at a member node between a kept arm and the link to
   * the centre. Empty when the two segments still meet at one member node, or when the mode has no connection between
   * them any more
   *
   * @param from segment arriving at the intersection
   * @param to segment leaving it
   * @param centre node of the intersection
   * @param mode collapsed
   * @return the turns as pairs of from and to segment, in order
   */
  private List<Pair<LinkSegment, LinkSegment>> findTurnsOfCollapsedMode(
      LinkSegment from, LinkSegment to, Node centre, Mode mode) {
    var fromNode = from.getDownstreamNode();
    var toNode = to.getUpstreamNode();
    if (fromNode == toNode && fromNode != centre) {
      return List.of();
    }
    /* from a member node onto the link to the centre */
    var turns = new ArrayList<Pair<LinkSegment, LinkSegment>>(3);
    var arriving = from;
    if (fromNode != centre) {
      var link = findLinkToCentreNode(centre, fromNode, mode);
      if (link == null) {
        return List.of();
      }
      var towardsCentre = (LinkSegment) link.getSegmentDownstreamFrom(fromNode);
      turns.add(Pair.of(arriving, towardsCentre));
      arriving = towardsCentre;
    }
    /* at the centre onto the link to the exit's member node and on to the exit, or straight onto the exit */
    if (toNode != centre) {
      var link = findLinkToCentreNode(centre, toNode, mode);
      if (link == null) {
        return List.of();
      }
      var fromCentre = (LinkSegment) link.getSegmentDownstreamFrom(centre);
      turns.add(Pair.of(arriving, fromCentre));
      turns.add(Pair.of(fromCentre, to));
    } else {
      turns.add(Pair.of(arriving, to));
    }
    return turns;
  }

  /**
   * Restrict the turns collapse made, at the centre node and at member nodes onto and off a link to the centre, so each
   * collapsed mode can make through the collapsed intersection only the movements it could make while the intersection
   * was spatial (C8 step 6). Collapse joins the arms at one centre node, so without bans a collapsed mode could make
   * movements there it could not make before, in two ways:
   * <ul>
   * <li>a movement the mode has access to but could not make, see
   * {@link #banNewTurnsOfMovementsTheModeCouldNotMake};</li>
   * <li>a turn between two arms no movement for the mode joins, see {@link #banNewTurnsNoMovementPasses}.</li>
   * </ul>
   * A segment entering the intersection that is not an approach has no movements, so what it could reach while
   * spatial counts as its movements the mode could make: the new turns those pass are not banned. A turn banned already
   * gains the modes, rather than a second banned turn being registered
   *
   * @param intersection collapsed
   * @param centre its centre node
   * @param collapsedModesBannedOfMovement the collapsed modes each movement could not be made by, worked out while the
   *                                       intersection was spatial
   * @param exitsReachedFromEntriesThatAreNotApproaches per collapsed mode, each entering segment that is not an
   *                                                    approach with an exit it could reach while spatial
   */
  private void restrictNewTurnsToMovementsPossibleWhileSpatial(SpatialIntersection intersection, Node centre,
      Map<SpatialMovement, List<Mode>> collapsedModesBannedOfMovement,
      Map<Mode, List<Pair<LinkSegment, LinkSegment>>>
          exitsReachedFromEntriesThatAreNotApproaches) {
    var bannedModesOfTurn = new LinkedHashMap<Pair<LinkSegment, LinkSegment>, List<Mode>>();
    for (var mode : modes) {
      /* the new turns of each movement the mode has access to, and the movements passing each turn */
      var turnsOfMovement = new LinkedHashMap<SpatialMovement, List<Pair<LinkSegment,
          LinkSegment>>>();
      var movementsOfTurn = new LinkedHashMap<Pair<LinkSegment, LinkSegment>,
          List<SpatialMovement>>();
      for (var movement : intersection.getMovements()) {
        if (!movement.getLinkSegmentFrom().isModeAllowed(mode) || !movement.getLinkSegmentTo().isModeAllowed(mode)) {
          continue;
        }
        var turns = findTurnsOfCollapsedMode(movement.getLinkSegmentFrom(), movement.getLinkSegmentTo(), centre, mode);
        turnsOfMovement.put(movement, turns);
        turns.forEach(turn -> movementsOfTurn.computeIfAbsent(turn, key -> new ArrayList<>()).add(movement));
      }
      /* the new turns of what each entering segment that is not an approach could reach */
      var turnsOfEntriesThatAreNotApproaches =
          new LinkedHashSet<Pair<LinkSegment, LinkSegment>>();
      exitsReachedFromEntriesThatAreNotApproaches.getOrDefault(mode, List.of()).forEach(reached ->
          turnsOfEntriesThatAreNotApproaches.addAll(
              findTurnsOfCollapsedMode(reached.first(), reached.second(), centre, mode)));
      /* ban the mode on the new turns where it could not move while the intersection was spatial */
      Consumer<Pair<LinkSegment, LinkSegment>> banForMode =
          turn -> bannedModesOfTurn.computeIfAbsent(turn, key -> new ArrayList<>()).add(mode);
      banNewTurnsOfMovementsTheModeCouldNotMake(mode, turnsOfMovement, movementsOfTurn,
          turnsOfEntriesThatAreNotApproaches, collapsedModesBannedOfMovement, banForMode);
      var turnsPassed = new HashSet<>(movementsOfTurn.keySet());
      turnsPassed.addAll(turnsOfEntriesThatAreNotApproaches);
      banNewTurnsNoMovementPasses(intersection, centre, mode, turnsPassed, banForMode);
    }
    /* register the bans, extending a turn banned already; a turn ban is physical, between link segments of the
     * layer, so a turn onto or off a connectoid segment of an integrated zoning is not banned */
    bannedModesOfTurn.forEach((turn, bannedModes) -> {
      var bannedTurn = MacroscopicNetworkLayerUtils.banTurnForModes(layer, turn.first(), turn.second(), bannedModes);
      if (bannedTurn != null) {
        result.addBannedTurnAddedOrExtendedForCollapsedModes(bannedTurn);
      }
    });
  }

  /**
   * Ban a collapsed mode on the new turns of the movements it has access to but could not make while the intersection
   * was spatial, see {@link #findCollapsedModesBannedOfMovements}: banned directly, by a turn banned at a member node,
   * or by an inner link the mode may not use. Collapse joins such a movement's arms through the centre node, so without
   * a ban the mode could now make it. A turn is banned only when every movement passing it is one the mode could not
   * make, and no entering segment that is not an approach passes it on the way to an exit it could reach, so nothing
   * the mode could do becomes banned. A movement whose new turns are all shared with something the mode could do
   * keeps no ban, and is listed as not held; this only happens for a movement with an arm that stays on its member
   * node, since the link from that node to the centre is shared by every arm there
   *
   * @param mode collapsed
   * @param turnsOfMovement the new turns of each movement the mode has access to
   * @param movementsOfTurn the movements the mode has access to passing each new turn
   * @param turnsOfEntriesThatAreNotApproaches the new turns passed by an entering segment that is not an approach on
   *                                           the way to an exit it could reach
   * @param collapsedModesBannedOfMovement the collapsed modes each movement could not be made by
   * @param banForMode bans the mode on a turn
   */
  private void banNewTurnsOfMovementsTheModeCouldNotMake(Mode mode,
      Map<SpatialMovement, List<Pair<LinkSegment, LinkSegment>>> turnsOfMovement,
      Map<Pair<LinkSegment, LinkSegment>, List<SpatialMovement>> movementsOfTurn,
      Set<Pair<LinkSegment, LinkSegment>> turnsOfEntriesThatAreNotApproaches,
      Map<SpatialMovement, List<Mode>> collapsedModesBannedOfMovement,
      Consumer<Pair<LinkSegment, LinkSegment>> banForMode) {
    /* a turn every movement passing it is one the mode could not make, and no entering segment needs */
    Predicate<SpatialMovement> couldNotMake = movement ->
        collapsedModesBannedOfMovement.getOrDefault(movement, List.of()).contains(mode);
    var turnsToBan = movementsOfTurn.entrySet().stream()
        .filter(entry -> entry.getValue().stream().allMatch(couldNotMake) &&
            !turnsOfEntriesThatAreNotApproaches.contains(entry.getKey()))
        .map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
    turnsToBan.forEach(banForMode);
    /* a movement it could not make with new turns, none of them banned, keeps no ban */
    turnsOfMovement.forEach((movement, turns) -> {
      if (couldNotMake.test(movement) && !turns.isEmpty() && turns.stream().noneMatch(turnsToBan::contains)) {
        result.addCollapsedModeBanNotHeld(movement, mode);
      }
    });
  }

  /**
   * Ban a collapsed mode on each new turn that no movement the mode has access to passes, nor an entering segment that
   * is not an approach on the way to an exit it could reach. Such a turn joins two arms between which no mode could go
   * while the intersection was spatial, so no movement was stored for them, for example
   * two one-way roads joined inside the intersection by a one-way link, where cars could not turn back from one onto
   * the other. Collapse joins every arm at the centre node, so without a ban the mode could now make that turn. The new
   * turns are those at the centre node, and those at a member node onto and off its link to the centre for the mode.
   * Turning back at the centre node onto the arm it arrived on is left as it is, since the mode could do so at that
   * arm's member node before. Turning back onto a link to a member node it just came along is banned, as it is inside a
   * spatial intersection, see {@link MacroscopicNetworkLayerUtils#findExitsReachedThroughSpatialIntersection}
   *
   * @param intersection collapsed
   * @param centre its centre node
   * @param mode collapsed
   * @param turnsPassed the new turns passed by a movement the mode has access to, or by an entering segment that is not
   *                    an approach on the way to an exit it could reach
   * @param banForMode bans the mode on a turn
   */
  private void banNewTurnsNoMovementPasses(SpatialIntersection intersection, Node centre, Mode mode,
      Set<Pair<LinkSegment, LinkSegment>> turnsPassed,
      Consumer<Pair<LinkSegment, LinkSegment>> banForMode) {
    Predicate<LinkSegment> allowsMode = segment -> segment.isModeAllowed(mode);
    var newTurns = new ArrayList<Pair<LinkSegment, LinkSegment>>();
    /* at the centre, between every arriving and leaving segment, other than back onto the arm it arrived on */
    for (var arriving : NodeUtils.<LinkSegment>findEntryLinkSegments(centre, allowsMode)) {
      for (var leaving : NodeUtils.<LinkSegment>findExitLinkSegments(centre, allowsMode)) {
        if (leaving.getParent() != arriving.getParent() || intersection.hasMemberNode(arriving.getUpstreamNode())) {
          newTurns.add(Pair.of(arriving, leaving));
        }
      }
    }
    /* at each member node, onto its link to the centre and off it */
    for (var memberNode : intersection.getMemberNodes()) {
      var link = memberNode == centre ? null : findLinkToCentreNode(centre, memberNode, mode);
      if (link == null) {
        continue;
      }
      var towardsCentre = (LinkSegment) link.getSegmentDownstreamFrom(memberNode);
      var fromCentre = (LinkSegment) link.getSegmentDownstreamFrom(centre);
      NodeUtils.<LinkSegment>findEntryLinkSegments(memberNode, allowsMode)
          .forEach(arriving -> newTurns.add(Pair.of(arriving, towardsCentre)));
      NodeUtils.<LinkSegment>findExitLinkSegments(memberNode, allowsMode).stream()
          .filter(leaving -> leaving != towardsCentre).forEach(leaving -> newTurns.add(Pair.of(fromCentre, leaving)));
    }
    /* banned where nothing the mode could do passes */
    newTurns.stream().filter(turn -> !turnsPassed.contains(turn)).forEach(banForMode);
  }

  /**
   * Take the collapsed modes off the banned turns on the internal segments a spatial intersection has left after
   * collapsing, since those modes no longer use them (C8 step 6). A banned turn left with no modes is removed through
   * the modifier, with its removal event
   *
   * @param intersection collapsed
   */
  private void removeCollapsedModesFromBannedTurnsOnInternalSegments(SpatialIntersection intersection) {
    Set<BannedTurn> seen = new LinkedHashSet<>();
    for (var internalSegment : intersection.getInternalSegments()) {
      for (var bannedTurn : List.copyOf(layer.getBannedTurns().getBySegment(internalSegment))) {
        if (!seen.add(bannedTurn)) {
          continue;
        }
        /* the modes it bans other than the collapsed ones; untouched when it bans none of those */
        var macroscopicBannedTurn = (MacroscopicBannedTurn) bannedTurn;
        var remainingModes = new ArrayList<>(macroscopicBannedTurn.hasExplicitlyBannedModes() ?
            macroscopicBannedTurn.getExplicitlyBannedModes() : layer.getSupportedModes());
        if (!remainingModes.removeAll(modes)) {
          continue;
        }
        /* removed when none are left, otherwise it names only the modes left */
        if (remainingModes.isEmpty()) {
          layer.getLayerModifier().removeBannedTurn(macroscopicBannedTurn);
          result.addRemovedBannedTurn(macroscopicBannedTurn);
        } else if (macroscopicBannedTurn.hasExplicitlyBannedModes()) {
          modes.forEach(macroscopicBannedTurn::removeExplicitlyBannedMode);
          result.addBannedTurnThatLostCollapsedModes(macroscopicBannedTurn);
        } else {
          remainingModes.forEach(macroscopicBannedTurn::addExplicitlyBannedMode);
          result.addBannedTurnThatLostCollapsedModes(macroscopicBannedTurn);
        }
      }
    }
  }

  /**
   * The given spatial intersections less those holding a node the settings exclude from collapse, which stay as they
   * are. For example, with junction node A excluded, the junction holding A is not collapsed, whatever its other member
   * nodes. An excluded id no node has is recorded as not found
   *
   * @param collapsing the spatial intersections to collapse
   * @return the spatial intersections to collapse, in order, without those holding a node excluded from collapse
   */
  private List<SpatialIntersection> leaveOutIntersectionsHoldingNodesExcludedFromCollapse(
      List<SpatialIntersection> collapsing) {
    var excludedNodes = IdMappingUtils.findEntitiesOfEachIdList(layer.getNodes(),
        settings.getNodeIdsExcludedFromCollapse(), result::addExcludedNodeIdNotFound).stream()
        .flatMap(List::stream).collect(Collectors.toSet());
    return collapsing.stream()
        .filter(intersection -> intersection.getMemberNodes().stream().noneMatch(excludedNodes::contains))
        .collect(Collectors.toList());
  }

  /**
   * Constructor
   *
   * @param layer whose spatial intersections to collapse, through its own modifier
   * @param settings the modes to collapse onto the centre nodes, and the nodes excluded from collapse
   * @param crs of the layer's geometry
   */
  public IntersectionModeCollapser(MacroscopicNetworkLayerImpl layer, IntersectionCollapserSettings settings,
      CoordinateReferenceSystem crs) {
    this.layer = layer;
    this.settings = new IntersectionCollapserSettings(settings);
    this.modes = List.copyOf(this.settings.getCollapsedModes());
    this.crsUtils = new PlanitJtsCrsUtils(crs);
  }

  /**
   * Collapse the given spatial intersections, each onto one node for the modes. Nothing is done when no intersection
   * or no mode is given. A spatial intersection with a member node the settings exclude from collapse stays as it is,
   * see {@link #leaveOutIntersectionsHoldingNodesExcludedFromCollapse}. What is left without access is removed once,
   * after all of them; a spatial intersection left with one member node becomes a non-spatial one through the
   * modifier. Then the banned turns on the internal segments left lose the collapsed modes. Movements stay as they are
   *
   * @param intersections to collapse, each registered on the layer
   * @return what was done
   */
  public IntersectionCollapseModifierResult collapse(Collection<? extends SpatialIntersection> intersections) {
    /* each given once, and each registered on the layer, since collapse changes them through its lookups */
    var collapsing = List.<SpatialIntersection>copyOf(new LinkedHashSet<>(intersections));
    if (collapsing.isEmpty() || modes.isEmpty()) {
      return result;
    }
    for (var intersection : collapsing) {
      PlanItRunTimeException.throwIf(!layer.getIntersections().contains(intersection),
          "Spatial intersection %s to collapse is not registered on the layer", intersection.getIdsAsString());
    }
    collapsing = leaveOutIntersectionsHoldingNodesExcludedFromCollapse(collapsing);

    /* collapse each, reusing the modified copies of types across them */
    var modifiedTypes = new ModifiedLinkSegmentTypes();
    collapsing.forEach(intersection -> collapse(intersection, modifiedTypes));

    /* remove what no mode can use any more, then take the collapsed modes off the bans on internal segments left */
    result.setModeAccessCleanup(layer.getLayerModifier().removeInfrastructureWithoutModeAccess());
    for (var intersection : collapsing) {
      if (layer.getIntersections().contains(intersection)) {
        removeCollapsedModesFromBannedTurnsOnInternalSegments(intersection);
      }
    }
    return result;
  }
}
