package org.goplanit.network.layer.modifier.consolidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkUtils;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionCollapseModifierResult;
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

  /** orders link segments by id, to break ties the same way every time */
  private static final Comparator<MacroscopicLinkSegment> SEGMENTS_BY_ID =
      Comparator.comparingLong(MacroscopicLinkSegment::getId);

  /** the layer whose spatial intersections are collapsed */
  private final MacroscopicNetworkLayerImpl layer;

  /** the modes to collapse onto the centre nodes, a copy of those given */
  private final List<Mode> modes;

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
      layer.getIntersections().update(intersection, i -> i.addInternalSegment(segment));
    }
    result.addLinkAddedToCentreNode(link);
  }

  /**
   * The link added for a collapsed mode from the centre node to a member node, see {@link #addLinkToCentreNode}. It is
   * the one link between the two that allows the mode, since the links moved to the centre end outside the intersection
   *
   * @param centre node of the intersection
   * @param memberNode the link joins the centre to
   * @param mode collapsed, the only mode the link allows
   * @return the link, null when none was added
   */
  private static MacroscopicLink findLinkToCentreNode(Node centre, Node memberNode, Mode mode) {
    return centre.getEdges(memberNode).stream().map(MacroscopicLink.class::cast)
        .filter(link -> MacroscopicLinkUtils.hasSegmentMatching(link, segment -> segment.isModeAllowed(mode)))
        .findFirst().orElse(null);
  }

  /**
   * Collapse one spatial intersection: a centre node is added, the internal segments lose access for the collapsed
   * modes, links used only by them move their member node end to the centre, and a link for each of them joins the
   * centre to each member node where a kept road still allows it. The bans of the collapsed modes move onto the new
   * turns where they hold exactly (C8 step 6)
   *
   * @param intersection to collapse
   * @param modifiedTypes the modified copies of types made so far, reused when the same copy is needed again
   */
  private void collapse(SpatialIntersection intersection, ModifiedLinkSegmentTypes modifiedTypes) {
    /* the bans of the collapsed modes, worked out before anything changes, and the centre node */
    var memberNodes = List.copyOf(intersection.getMemberNodes());
    var collapsedModesBannedOfMovement = findCollapsedModesBannedOfMovements(intersection);
    var centre = addCentreNode(intersection);
    /* the internal segments lose access for the collapsed modes */
    for (var segment : List.copyOf(intersection.getInternalSegments())) {
      modes.stream().filter(segment::isModeAllowed).forEach(
          mode -> MacroscopicNetworkLayerUtils.withdrawModeAccess(layer, modifiedTypes, segment, mode));
    }
    /* links leaving the intersection that only collapsed modes use move their member node end to the centre */
    for (var memberNode : memberNodes) {
      for (var link : List.copyOf(memberNode.<MacroscopicLink>getLinks())) {
        if (!intersection.hasMemberNode(link.getOtherNode(memberNode)) &&
            MacroscopicLinkUtils.isUsedOnlyByModes(link, modes)) {
          layer.getLayerModifier().moveLinkEndNode(link, memberNode, centre, crsUtils.getCoordinateReferenceSystem())
              .forEach(result::addRemovedBannedTurn);
          result.addLinkMovedToCentreNode(link);
        }
      }
    }
    /* a link per collapsed mode joins the centre to each member node where a kept road still allows that mode */
    for (var mode : modes) {
      for (var memberNode : memberNodes) {
        var keptRoadSegment = memberNode.<MacroscopicLink>getLinks().stream()
            .filter(link -> !intersection.hasMemberNode(link.getOtherNode(memberNode)))
            .flatMap(link -> link.getLinkSegments().stream()).map(MacroscopicLinkSegment.class::cast)
            .filter(segment -> segment.isModeAllowed(mode)).min(SEGMENTS_BY_ID).orElse(null);
        if (keptRoadSegment != null) {
          addLinkToCentreNode(intersection, centre, memberNode, mode, keptRoadSegment, modifiedTypes);
        }
      }
    }
    /* the collapsed modes keep only the movements they could make while the intersection was spatial */
    restrictNewTurnsToMovementsPossibleWhileSpatial(intersection, centre, collapsedModesBannedOfMovement);
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
   * The turns a collapsed mode now makes for a movement, once its arms used only by collapsed modes end at the centre
   * node and links join the centre to the member nodes: at the centre, and at a member node between a kept arm and the
   * link to the centre. Empty when the movement's from and to segments still meet at one member node, or when the mode
   * has no connection for it any more
   *
   * @param movement to follow
   * @param centre node of its intersection
   * @param mode collapsed
   * @return the turns as pairs of from and to segment, in order
   */
  private List<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>> findTurnsOfCollapsedMode(
      SpatialMovement movement, Node centre, Mode mode) {
    var from = movement.getLinkSegmentFrom();
    var to = movement.getLinkSegmentTo();
    var fromNode = from.getDownstreamNode();
    var toNode = to.getUpstreamNode();
    if (fromNode == toNode && fromNode != centre) {
      return List.of();
    }
    /* from a member node onto the link to the centre */
    var turns = new ArrayList<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>>(3);
    var arriving = from;
    if (fromNode != centre) {
      var link = findLinkToCentreNode(centre, fromNode, mode);
      if (link == null) {
        return List.of();
      }
      var towardsCentre = (MacroscopicLinkSegment) link.getSegmentDownstreamFrom(fromNode);
      turns.add(Pair.of(arriving, towardsCentre));
      arriving = towardsCentre;
    }
    /* at the centre onto the link to the exit's member node and on to the exit, or straight onto the exit */
    if (toNode != centre) {
      var link = findLinkToCentreNode(centre, toNode, mode);
      if (link == null) {
        return List.of();
      }
      var fromCentre = (MacroscopicLinkSegment) link.getSegmentDownstreamFrom(centre);
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
   * A turn banned already gains the modes, rather than a second banned turn being registered
   *
   * @param intersection collapsed
   * @param centre its centre node
   * @param collapsedModesBannedOfMovement the collapsed modes each movement could not be made by, worked out while the
   *                                       intersection was spatial
   */
  private void restrictNewTurnsToMovementsPossibleWhileSpatial(SpatialIntersection intersection, Node centre,
      Map<SpatialMovement, List<Mode>> collapsedModesBannedOfMovement) {
    var bannedModesOfTurn = new LinkedHashMap<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>, List<Mode>>();
    for (var mode : modes) {
      /* the new turns of each movement the mode has access to, and the movements passing each turn */
      var turnsOfMovement = new LinkedHashMap<SpatialMovement, List<Pair<MacroscopicLinkSegment,
          MacroscopicLinkSegment>>>();
      var movementsOfTurn = new LinkedHashMap<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>,
          List<SpatialMovement>>();
      for (var movement : intersection.getMovements()) {
        if (!movement.getLinkSegmentFrom().isModeAllowed(mode) || !movement.getLinkSegmentTo().isModeAllowed(mode)) {
          continue;
        }
        var turns = findTurnsOfCollapsedMode(movement, centre, mode);
        turnsOfMovement.put(movement, turns);
        turns.forEach(turn -> movementsOfTurn.computeIfAbsent(turn, key -> new ArrayList<>()).add(movement));
      }
      /* ban the mode on the new turns where it could not move while the intersection was spatial */
      Consumer<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>> banForMode =
          turn -> bannedModesOfTurn.computeIfAbsent(turn, key -> new ArrayList<>()).add(mode);
      banNewTurnsOfMovementsTheModeCouldNotMake(
          mode, turnsOfMovement, movementsOfTurn, collapsedModesBannedOfMovement, banForMode);
      banNewTurnsNoMovementPasses(intersection, centre, mode, movementsOfTurn.keySet(), banForMode);
    }
    /* register the bans, extending a turn banned already */
    bannedModesOfTurn.forEach((turn, bannedModes) -> result.addBannedTurnAddedOrExtendedForCollapsedModes(
        MacroscopicNetworkLayerUtils.banTurnForModes(layer, turn.first(), turn.second(), bannedModes)));
  }

  /**
   * Ban a collapsed mode on the new turns of the movements it has access to but could not make while the intersection
   * was spatial, see {@link #findCollapsedModesBannedOfMovements}: banned directly, by a turn banned at a member node,
   * or by an inner link the mode may not use. Collapse joins such a movement's arms through the centre node, so without
   * a ban the mode could now make it. A turn is banned only when every movement passing it is one the mode could not
   * make, so no movement the mode could make becomes banned. A movement whose new turns are all shared with a movement
   * the mode could make keeps no ban, and is listed as not held; this only happens for a movement with an arm that
   * stays on its member node, since the link from that node to the centre is shared by every arm there
   *
   * @param mode collapsed
   * @param turnsOfMovement the new turns of each movement the mode has access to
   * @param movementsOfTurn the movements the mode has access to passing each new turn
   * @param collapsedModesBannedOfMovement the collapsed modes each movement could not be made by
   * @param banForMode bans the mode on a turn
   */
  private void banNewTurnsOfMovementsTheModeCouldNotMake(Mode mode,
      Map<SpatialMovement, List<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>>> turnsOfMovement,
      Map<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>, List<SpatialMovement>> movementsOfTurn,
      Map<SpatialMovement, List<Mode>> collapsedModesBannedOfMovement,
      Consumer<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>> banForMode) {
    /* a turn every movement passing it is one the mode could not make */
    Predicate<SpatialMovement> couldNotMake = movement ->
        collapsedModesBannedOfMovement.getOrDefault(movement, List.of()).contains(mode);
    var turnsToBan = movementsOfTurn.entrySet().stream()
        .filter(entry -> entry.getValue().stream().allMatch(couldNotMake))
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
   * Ban a collapsed mode on each new turn that no movement the mode has access to passes. Such a turn joins two arms
   * between which no mode could go while the intersection was spatial, so no movement was stored for them, for example
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
   * @param turnsPassed the new turns passed by a movement the mode has access to
   * @param banForMode bans the mode on a turn
   */
  private void banNewTurnsNoMovementPasses(SpatialIntersection intersection, Node centre, Mode mode,
      Set<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>> turnsPassed,
      Consumer<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>> banForMode) {
    Predicate<MacroscopicLinkSegment> allowsMode = segment -> segment.isModeAllowed(mode);
    var newTurns = new ArrayList<Pair<MacroscopicLinkSegment, MacroscopicLinkSegment>>();
    /* at the centre, between every arriving and leaving segment, other than back onto the arm it arrived on */
    for (var arriving : NodeUtils.<MacroscopicLinkSegment>findEntryLinkSegments(centre, allowsMode)) {
      for (var leaving : NodeUtils.<MacroscopicLinkSegment>findExitLinkSegments(centre, allowsMode)) {
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
      var towardsCentre = (MacroscopicLinkSegment) link.getSegmentDownstreamFrom(memberNode);
      var fromCentre = (MacroscopicLinkSegment) link.getSegmentDownstreamFrom(centre);
      NodeUtils.<MacroscopicLinkSegment>findEntryLinkSegments(memberNode, allowsMode)
          .forEach(arriving -> newTurns.add(Pair.of(arriving, towardsCentre)));
      NodeUtils.<MacroscopicLinkSegment>findExitLinkSegments(memberNode, allowsMode).stream()
          .filter(leaving -> leaving != towardsCentre).forEach(leaving -> newTurns.add(Pair.of(fromCentre, leaving)));
    }
    /* banned where no movement passes */
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
   * Constructor
   *
   * @param layer whose spatial intersections to collapse, through its own modifier
   * @param modes to collapse onto the centre nodes
   * @param crs of the layer's geometry
   */
  public IntersectionModeCollapser(MacroscopicNetworkLayerImpl layer, Collection<Mode> modes,
      CoordinateReferenceSystem crs) {
    this.layer = layer;
    this.modes = List.copyOf(new LinkedHashSet<>(modes));
    this.crsUtils = new PlanitJtsCrsUtils(crs);
  }

  /**
   * Collapse the given spatial intersections, each onto one node for the modes. Nothing is done when no intersection
   * or no mode is given. What is left without access is removed once, after all of them; a spatial intersection left
   * with one member node becomes a non-spatial one through the modifier. Then the banned turns on the internal segments
   * left lose the collapsed modes. Movements stay as they are
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
      PlanItRunTimeException.throwIf(layer.getIntersections().get(intersection.getId()) != intersection,
          "Spatial intersection %s to collapse is not registered on the layer", intersection.getIdsAsString());
    }

    /* collapse each, reusing the modified copies of types across them */
    var modifiedTypes = new ModifiedLinkSegmentTypes();
    collapsing.forEach(intersection -> collapse(intersection, modifiedTypes));

    /* remove what no mode can use any more, then take the collapsed modes off the bans on internal segments left */
    result.setModeAccessCleanup(layer.getLayerModifier().removeInfrastructureWithoutModeAccess());
    for (var intersection : collapsing) {
      if (layer.getIntersections().get(intersection.getId()) == intersection) {
        removeCollapsedModesFromBannedTurnsOnInternalSegments(intersection);
      }
    }
    return result;
  }
}
