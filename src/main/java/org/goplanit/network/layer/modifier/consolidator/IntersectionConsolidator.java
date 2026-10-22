package org.goplanit.network.layer.modifier.consolidator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.algorithms.shortest.ShortestPathBreadthFirst;
import org.goplanit.algorithms.shortest.ShortestPathBreadthFirstResult;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.network.layer.macroscopic.SpatialMovementUtils;
import org.goplanit.utils.containers.ContainerUtils;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.geo.PlanitGraphGeoUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentUtils;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.ApproachOutcome;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.IntersectionOutcome;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.LinkReachedOnlyThroughIntersection;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.NodeUtils;
import org.locationtech.jts.geom.Point;

/**
 * Consolidates the intersections of a layer. It decides which parsed intersections form one junction, see
 * {@link IntersectionCluster}, and builds the consolidated intersections. It then pulls in the junctions within their
 * radius, joins the nodes they enclose, and pulls in the junctions that the roads of would-be approaches without a
 * signal lead back to. Each intersection that grew by pulling in a junction then makes internal the links now reached
 * only through it, and joins the nodes it now encloses. Once the junctions are grouped, crossings join them, and each
 * intersection a crossing joined joins the nodes it now encloses. The roads still entering a signalised junction
 * without an approach become approaches, with the signal tagged on the node ahead when they carry on its road and
 * without a signal otherwise, and the movements are stored. Junctions never join when the
 * group would lose every approach with a signal, see {@link #wouldLoseEveryApproachWithSignal}. Intersections are
 * removed through the layer modifier, so each removal fires its event. Used once per consolidation, run through
 * {@link org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier#consolidateIntersections}, not on
 * its own.
 *
 * @author markr
 */
public class IntersectionConsolidator {

  /** A junction pulled in along a connection that crosses a tram or light rail track may lie this much further beyond
   * the radius than the margin, in metres: about the width of a tram or light rail corridor in the middle of a road,
   * double track with platforms. The corridor between two junctions makes for a larger junction layout */
  private static final double PULL_IN_EXTRA_MARGIN_WHERE_CONNECTION_CROSSES_TRACK_METERS = 15;

  /** further distance a junction may lie beyond the pull-in reach when the road of an approach without a signal leads
   * back to it, in metres. Such an approach is unusual at a signalised junction, so its signal most likely belongs to
   * the junction it leads back to, just beyond the reach */
  private static final double PULL_IN_EXTRA_MARGIN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL_METERS = 15;

  /** longest connection between two member nodes, through nodes no motorised road or rail mode uses, along which such
   * nodes are enclosed, in metres, see {@link #findNodesNotOnRoadOrRailOnShortConnectionsBetweenMemberNodes}: a
   * crossing's footway side, the pavement round the corner and the next crossing's footway side */
  private static final double MAXIMUM_CONNECTION_THROUGH_NODES_NOT_ON_ROAD_OR_RAIL_METERS = 50;

  /**
   * The centre of an intersection and its radius, both as the member nodes are when it is made. Only the member nodes a
   * motorised road or rail mode uses count, so the nodes of footways and cycleways that join never widen it. The centre
   * is their centroid, the radius the distance from the centre to the furthest of them. An intersection without such
   * member nodes takes all its member nodes instead, see {@link #computeCentreAndRadius}
   */
  private static final class CentreAndRadius {

    /** the centroid of the member nodes that count */
    private final Point centre;

    /** distance from the centre to the furthest member node that counts, in metres */
    private final double radiusMeters;

    /**
     * Constructor
     *
     * @param centre the centroid of the member nodes that count
     * @param radiusMeters distance from the centre to the furthest member node that counts, in metres
     */
    private CentreAndRadius(Point centre, double radiusMeters) {
      this.centre = centre;
      this.radiusMeters = radiusMeters;
    }
  }

  /** the layer to consolidate */
  private final MacroscopicNetworkLayerImpl layer;

  /** to measure directions and distances with, in the layer's coordinate reference system */
  private final PlanitJtsCrsUtils crsUtils;

  /** the reach, scoring values and warning lengths, a copy of those given */
  private final IntersectionConsolidatorSettings settings;

  /** true for a link segment with a signal of its own, which the layer does not record: a vehicle signal standing on
   * it, or a light for the footway or cycleway it is on where no road or rail passes. A segment that becomes internal is
   * added as an internal segment with a signal when it has one, and a link reached only through the intersection is
   * listed as having a signal when one of its segments has one. Asked of road segments into a junction too, to tell
   * whether a node lies in front of it, see {@link #isEnteredFromOutsideBeforeVehicleSignalIntoJunction} */
  private final Predicate<MacroscopicLinkSegment> hasSignalOfItsOwn;

  /** the member nodes of the signalised crossing-only intersections before consolidation, so a segment into a crossing
   * is still known once the crossing has joined a junction, see {@link #keepsCrossingSignalOnceInternal} */
  private final Set<Node> memberNodesOfSignalisedCrossingOnlyIntersections = new HashSet<>();

  /** the approaches with a signal of their own of the layer's intersections before consolidation, so such an approach
   * is still known once its start node has joined, see {@link #continuesRoadOfApproachWithSignalTaggedOnNode} */
  private final Set<MacroscopicLinkSegment> approachesWithSignalBeforeConsolidation = new HashSet<>();

  /** per member node of a parsed intersection, the index of the intersection cluster it was weighed in, see
   * {@link #wereWeighedInOneCluster} */
  private final Map<Node, Integer> clusterIndexOfParsedMemberNode = new HashMap<>();

  /** the spatial intersections consolidation made, as opposed to those the layer had. One merged away again is not
   * listed as replaced itself, see {@link #joinIntoSpatialIntersection} */
  private final Set<Intersection> madeByConsolidation = Collections.newSetFromMap(new IdentityHashMap<>());

  /** what was done, filled where each change is made */
  private final IntersectionConsolidationModifierResult result = new IntersectionConsolidationModifierResult();

  /** true for a link a motorised road mode may use in at least one direction */
  static final Predicate<Edge> IS_ROAD_LINK = MacroscopicLinkUtils.permitsMotorisedRoadModeOnAnySegment();

  /** true for a segment a motorised road mode may use, false for none */
  static final Predicate<MacroscopicLinkSegment> IS_ROAD_SEGMENT =
      MacroscopicLinkSegmentUtils.permitsMotorisedRoadMode()::test;

  /**
   * The centre and radius of an intersection with the given member nodes, see {@link CentreAndRadius}
   *
   * @param memberNodes of the intersection
   * @return its centre and radius
   */
  private CentreAndRadius computeCentreAndRadius(Collection<Node> memberNodes) {
    var onRoadOrRail = memberNodes.stream()
        .filter(node -> !MacroscopicLinkUtils.isUsedByNoMotorisedRoadOrRailMode(node)).collect(Collectors.toList());
    var counted = onRoadOrRail.isEmpty() ? memberNodes : onRoadOrRail;
    var centre = PlanitGraphGeoUtils.createCentroid(counted);
    return new CentreAndRadius(
        centre, PlanitGraphGeoUtils.getDistanceToFurthestVertexInMetres(centre, counted, crsUtils));
  }

  /**
   * Verify if a node lies within a distance of an intersection's centre
   *
   * @param centreAndRadius of the intersection
   * @param node to verify
   * @param meters the distance
   * @return true when within it, false otherwise
   */
  private boolean isWithinDistanceOfCentre(CentreAndRadius centreAndRadius, Node node, double meters) {
    return crsUtils.getDistanceInMetres(centreAndRadius.centre, node.getPosition()) <= meters + Precision.EPSILON_9;
  }

  /**
   * Verify if a node lies within an intersection's radius of its centre
   *
   * @param centreAndRadius of the intersection
   * @param node to verify
   * @return true when within it, false otherwise
   */
  private boolean isWithinRadius(CentreAndRadius centreAndRadius, Node node) {
    return isWithinDistanceOfCentre(centreAndRadius, node, centreAndRadius.radiusMeters);
  }

  /**
   * Find the intersection clusters (C2 step 1). Two parsed intersections are in one cluster when a member node of one
   * lies within the joining reach of a member node of the other, along the road in either direction.
   * Clusters chain. A cluster's nodes are its member nodes and the nodes on the shortest road connections of at most
   * that length between them. A node with links of at most that length to member nodes of two or more of its parsed
   * intersections joins too, when road traffic reaches it from one of those member nodes. There is one exception, for
   * both kinds of node: a node in front of the junction stays outside, see
   * {@link #leaveOutJoiningNodesInFrontOfJunction}. So a fork before the cluster, which road traffic only reaches from
   * outside, stays outside, and the segments from it stay approaches. So does a node where a road splits into the
   * carriageways of the junction, with the signal on the inbound one. So do the nodes on the road between two junctions
   * from where a side road comes in to the stop line of one of them: the junctions are then not connected through the
   * cluster's nodes
   *
   * @param parsedIntersections of the layer, by id
   * @param parsedIntersectionOfMemberNode each member node with its parsed intersection
   * @return the intersection clusters, by the lowest id of their parsed intersections
   */
  private List<IntersectionCluster> findJunctionClusters(
      List<Intersection> parsedIntersections, Map<Node, Intersection> parsedIntersectionOfMemberNode) {
    /* join parsed intersections that reach each other's member nodes along the road within the reach */
    var reachingPairs = new ArrayList<Pair<Intersection, Intersection>>();
    var connectionsFrom = new HashMap<Node, ShortestPathBreadthFirstResult<Node>>();
    for (var entry : parsedIntersectionOfMemberNode.entrySet()) {
      var connections = MacroscopicNetworkLayerUtils.findShortestConnections(
          entry.getKey(), IS_ROAD_LINK, node -> true, settings.getMaximumJoiningReachMeters());
      connectionsFrom.put(entry.getKey(), connections);
      for (var reached : connections.getReached()) {
        var other = parsedIntersectionOfMemberNode.get(reached);
        if (other != null && other != entry.getValue()) {
          reachingPairs.add(Pair.of(other, entry.getValue()));
        }
      }
    }

    /* each disjoint set of joined parsed intersections is a cluster, starting with their member nodes */
    var clusters = new ArrayList<Pair<List<Intersection>, Set<Node>>>();
    var clusterIndexOf = new HashMap<Intersection, Integer>();
    for (var joined : ListUtils.extractDisjointSets(parsedIntersections, reachingPairs)) {
      Set<Node> clusterNodes = new LinkedHashSet<>();
      for (var intersection : joined) {
        clusterNodes.addAll(intersection.getMemberNodes());
        clusterIndexOf.put(intersection, clusters.size());
      }
      clusters.add(Pair.of(new ArrayList<>(joined), clusterNodes));
    }

    /* add the nodes on the road connections between member nodes of a cluster */
    for (var cluster : clusters) {
      var clusterNodes = cluster.second();
      for (var memberNode : List.copyOf(clusterNodes)) {
        var connections = connectionsFrom.get(memberNode);
        for (var reached : connections.getReached()) {
          if (reached != memberNode && parsedIntersectionOfMemberNode.containsKey(reached)) {
            clusterNodes.addAll(connections.getEntitiesOnConnectionTo(reached));
          }
        }
      }
    }

    /* nodes with short links to member nodes of two or more parsed intersections of one cluster, and over those links
       the parsed intersections road traffic arrives from */
    var linkedParsedIntersections = new HashMap<Node, Set<Intersection>>();
    var arrivingFromParsedIntersections = new HashMap<Node, Set<Intersection>>();
    for (var entry : parsedIntersectionOfMemberNode.entrySet()) {
      var memberNode = entry.getKey();
      for (var link : memberNode.<MacroscopicLink>getLinks()) {
        Node neighbour = link.getOtherNode(memberNode);
        if (neighbour != memberNode && IS_ROAD_LINK.test(link) &&
            link.getLengthMeters() <= settings.getMaximumJoiningReachMeters() + Precision.EPSILON_9 &&
            !parsedIntersectionOfMemberNode.containsKey(neighbour)) {
          linkedParsedIntersections.computeIfAbsent(neighbour, node -> new HashSet<>()).add(entry.getValue());
          if (MacroscopicLinkUtils.hasSegmentMatching(
              link, segment -> segment.getUpstreamVertex() == memberNode && IS_ROAD_SEGMENT.test(segment))) {
            arrivingFromParsedIntersections.computeIfAbsent(neighbour, node -> new HashSet<>()).add(entry.getValue());
          }
        }
      }
    }
    /* add such a node to the cluster holding two or more of the parsed intersections it links to, when road traffic
       reaches it from a member node of the cluster */
    for (var linked : linkedParsedIntersections.entrySet()) {
      var node = linked.getKey();
      var arrivingFromClusters = arrivingFromParsedIntersections.getOrDefault(node, Set.of()).stream()
          .map(clusterIndexOf::get).collect(Collectors.toSet());
      var countByCluster = linked.getValue().stream()
          .collect(Collectors.groupingBy(clusterIndexOf::get, Collectors.counting()));
      countByCluster.forEach((clusterIndex, count) -> {
        if (count >= 2 && arrivingFromClusters.contains(clusterIndex)) {
          clusters.get(clusterIndex).second().add(node);
        }
      });
    }
    /* leave out the nodes in front of the junction, those on the connections and the linked ones alike */
    for (var cluster : clusters) {
      var memberNodes = IntersectionUtils.collectMemberNodes(cluster.first());
      var joiningNodes = cluster.second().stream().filter(node -> !memberNodes.contains(node))
          .collect(Collectors.toCollection(LinkedHashSet::new));
      leaveOutJoiningNodesInFrontOfJunction(memberNodes, joiningNodes, Set.of());
      cluster.second().removeIf(node -> !memberNodes.contains(node) && !joiningNodes.contains(node));
    }
    /* create the intersection clusters from what was gathered */
    return clusters.stream().map(cluster -> new IntersectionCluster(
        cluster.first(), cluster.second(), parsedIntersectionOfMemberNode, crsUtils, settings))
        .collect(Collectors.toList());
  }

  /**
   * True when road traffic enters a node from outside a junction before a vehicle signal into the junction. A road
   * segment enters the node from a node that is neither a member node nor about to join. And from the node, road
   * traffic meets a vehicle signal into the junction before it reaches a member node: on a road segment from the node
   * itself, or further on, past other nodes about to join. Traffic from outside then reaches the node before the
   * signal, so the node lies in front of the junction. One example is a node where a road splits into the carriageways
   * of the junction, with the signal on the inbound one: outbound traffic reaches it from the junction, yet it is no
   * part of it. Another is the node where a side road comes into the road between two junctions, some way before the
   * stop line of one of them. Only a vehicle signal standing on a road says where traffic stops; a signal tagged on a
   * junction node does not. A vehicle signal into a node of a crossing about to join is that crossing's own stop line,
   * not a signal into the junction
   *
   * @param node to check
   * @param memberNodes of the junction so far
   * @param joiningNodes the other nodes about to join it, the node checked among them
   * @param crossingNodes the nodes of a crossing about to join, among the member nodes, empty when none
   * @return true when entered from outside before a vehicle signal into the junction, false otherwise
   */
  private boolean isEnteredFromOutsideBeforeVehicleSignalIntoJunction(Node node, Set<Node> memberNodes,
      Set<Node> joiningNodes, Set<Node> crossingNodes) {
    Predicate<Object> isOfJunction = vertex -> memberNodes.contains(vertex) || joiningNodes.contains(vertex);
    if (NodeUtils.<MacroscopicLinkSegment>findEntryLinkSegments(node, segment ->
        IS_ROAD_SEGMENT.test(segment) && !isOfJunction.test(segment.getUpstreamVertex())).isEmpty()) {
      return false;
    }
    /* follow the road on from the node, past the other nodes about to join, until a vehicle signal into the junction */
    var passed = new HashSet<Node>(List.of(node));
    var toFollow = new ArrayDeque<Node>(List.of(node));
    while (!toFollow.isEmpty()) {
      for (var segment : NodeUtils.<MacroscopicLinkSegment>findExitLinkSegments(toFollow.poll(), IS_ROAD_SEGMENT)) {
        var next = (Node) segment.getDownstreamVertex();
        if (!isOfJunction.test(next)) {
          continue;
        }
        if (!crossingNodes.contains(next) && hasSignalOfItsOwn.test(segment)) {
          return true;
        }
        if (!memberNodes.contains(next) && passed.add(next)) {
          toFollow.add(next);
        }
      }
    }
    return false;
  }

  /**
   * True when a connection between two junctions crosses a tram or light rail track. A node on it that is a member node
   * of neither junction is where such a track meets a road, see
   * {@link MacroscopicLinkUtils#isWhereTramOrLightRailTrackMeetsRoad}
   *
   * @param connection the nodes on the connection, from one junction to the other
   * @param from the junction the connection starts at
   * @param to the junction the connection ends at
   * @return true when it crosses a tram or light rail track between them, false otherwise
   */
  private static boolean connectionCrossesTramOrLightRailTrackBetween(
      List<Node> connection, Intersection from, Intersection to) {
    return connection.stream().anyMatch(node -> !from.hasMemberNode(node) && !to.hasMemberNode(node) &&
        MacroscopicLinkUtils.isWhereTramOrLightRailTrackMeetsRoad(node));
  }

  /**
   * The distance from the centre of a junction within which another junction it connects to is pulled in: its radius
   * plus {@link IntersectionConsolidatorSettings#getPullInMarginBeyondRadiusMeters()}, plus
   * {@link #PULL_IN_EXTRA_MARGIN_WHERE_CONNECTION_CROSSES_TRACK_METERS} when the connection crosses a tram or light
   * rail track between them
   *
   * @param radius of the junction pulling in: the distance from its centre to the furthest member node, in metres
   * @param connection the nodes on the connection, from one junction to the other
   * @param into the junction pulling in
   * @param other the junction connected to
   * @return the reach, in metres
   */
  private double computePullInReachMeters(double radius, List<Node> connection, Intersection into, Intersection other) {
    return radius + settings.getPullInMarginBeyondRadiusMeters() +
        (connectionCrossesTramOrLightRailTrackBetween(connection, into, other) ?
            PULL_IN_EXTRA_MARGIN_WHERE_CONNECTION_CROSSES_TRACK_METERS : 0);
  }

  /**
   * Leave out the nodes about to join a junction that lie in front of it, so the roads from them stay approaches: the
   * nodes of an intersection cluster other than its member nodes (C2 step 1), those on the connections of junctions
   * pulled in (C2 step 9a), the nodes it encloses (C2 step 12), and those on the roads of junctions pulled in to
   * salvage an approach without a signal (C2 step 13). The connection of a crossing that joins is checked the same
   * way, see {@link #passesNodeInFrontOfJunction}. Road traffic reaches such a node only from outside, like a fork
   * before the junction. Or road traffic enters it from outside before a vehicle signal into the junction, see
   * {@link #isEnteredFromOutsideBeforeVehicleSignalIntoJunction}. A node no road traffic uses, such as one on a
   * footway, stays. Leaving one node out can leave the next one reached only from outside, so this repeats until no
   * more nodes are left out
   *
   * @param memberNodes of the junction, and of the junctions it pulls in
   * @param joiningNodes the other nodes about to join, from which the nodes left out are removed
   * @param crossingNodes the nodes of a crossing about to join, among the member nodes, empty when none: a vehicle
   *                      signal into them is the crossing's own stop line
   */
  private void leaveOutJoiningNodesInFrontOfJunction(Set<Node> memberNodes, Set<Node> joiningNodes,
      Set<Node> crossingNodes) {
    ContainerUtils.removeUntilNoneMatches(joiningNodes, () -> {
      var stillJoining = new HashSet<Node>(joiningNodes);
      return node -> {
        boolean usedByRoadTraffic = !NodeUtils.findEntryLinkSegments(node, IS_ROAD_SEGMENT).isEmpty();
        boolean reachedFromJunction = !NodeUtils.<MacroscopicLinkSegment>findEntryLinkSegments(node, segment ->
            IS_ROAD_SEGMENT.test(segment) && (memberNodes.contains(segment.getUpstreamVertex()) ||
                stillJoining.contains(segment.getUpstreamVertex()))).isEmpty();
        return (usedByRoadTraffic && !reachedFromJunction) ||
            isEnteredFromOutsideBeforeVehicleSignalIntoJunction(node, memberNodes, stillJoining, crossingNodes);
      };
    });
  }

  /**
   * How far back from the given nodes the links reached only through them are looked for (C2 step 9): the maximum
   * distance, see {@link IntersectionConsolidatorSettings#getMaximumDistanceReachedOnlyThroughIntersectionMeters()}, or
   * the radius of the nodes times
   * {@link IntersectionConsolidatorSettings#getMaximumDistanceReachedOnlyThroughIntersectionRadiusFactor()} when that
   * is further. A large intersection has longer roads between its junctions, so it looks further back. The radius is
   * taken as for an intersection's, see {@link CentreAndRadius}
   *
   * @param nodes of the consolidated intersection
   * @return the reach, in metres
   */
  private double computeReachedOnlyThroughIntersectionMeters(Set<Node> nodes) {
    return Math.max(settings.getMaximumDistanceReachedOnlyThroughIntersectionMeters(),
        computeCentreAndRadius(nodes).radiusMeters *
            settings.getMaximumDistanceReachedOnlyThroughIntersectionRadiusFactor());
  }

  /**
   * Find the links entering the given nodes that road traffic can only reach by driving through them (C2 step 9). From
   * each road segment entering the nodes from outside, trace back against the traffic over road segments, never into
   * the nodes, adding up the distance from the node it enters, the segment itself included. A way back ends at one of
   * the nodes. When a way back goes further than the reach, see {@link #computeReachedOnlyThroughIntersectionMeters},
   * or reaches a node no road traffic arrives at, traffic comes from outside. Turning back onto the link just traced
   * does not count as a way in, so a dead-end road stays an approach. Each case found is listed in the result
   *
   * @param nodes of the consolidated intersection
   * @return the nodes passed by the links found, empty when none
   */
  private Set<Node> findNodesOfLinksReachedOnlyThroughIntersection(Set<Node> nodes) {
    double maximumMeters = computeReachedOnlyThroughIntersectionMeters(nodes);
    var nodesPassed = new LinkedHashSet<Node>();
    /* each road segment entering the nodes from outside, itself within the reach, is a candidate */
    for (var node : nodes.stream().sorted(IdAbleUtils.byId()).collect(Collectors.toList())) {
      for (var entering : NodeUtils.<MacroscopicLinkSegment>findEntryLinkSegments(node, IS_ROAD_SEGMENT)) {
        if (nodes.contains(entering.getUpstreamNode()) ||
            entering.getParent().getLengthMeters() > maximumMeters + Precision.EPSILON_9) {
          continue;
        }

        /* trace back against the traffic, never back beyond the nodes. Traffic comes from outside when a way back
           reaches a node no road traffic arrives at, or goes further than the maximum distance */
        var tracedBack = traceRoadBack(entering, segment -> !nodes.contains(segment.getUpstreamNode()), maximumMeters);
        var feedingNodes = new TreeSet<Node>(IdAbleUtils.byId());
        double longest = 0;
        boolean fromOutside = false;
        for (var segment : tracedBack.getReached()) {
          double meters = findMetersBackToStartOf(segment, entering, tracedBack);
          if (nodes.contains(segment.getUpstreamNode())) {
            feedingNodes.add(segment.getUpstreamNode());
            longest = Math.max(longest, meters);
            continue;
          }
          var predecessors = MacroscopicLinkSegmentUtils.findPredecessors(segment, IS_ROAD_SEGMENT);
          if (predecessors.isEmpty() || predecessors.stream().anyMatch(predecessor ->
              meters + predecessor.getParent().getLengthMeters() > maximumMeters + Precision.EPSILON_9)) {
            fromOutside = true;
            break;
          }
        }
        if (fromOutside) {
          continue;
        }

        /* traffic reaches it only through the nodes: list the case and keep the nodes passed */
        var segmentsTracedBack = new ArrayList<>(tracedBack.getReached());
        result.addLinkReachedOnlyThroughIntersection(new LinkReachedOnlyThroughIntersection(entering,
            new ArrayList<>(feedingNodes), longest, segmentsTracedBack,
            segmentsTracedBack.stream().anyMatch(hasSignalOfItsOwn)));
        segmentsTracedBack.stream().map(MacroscopicLinkSegment::getUpstreamNode)
            .filter(upstream -> !nodes.contains(upstream)).forEach(nodesPassed::add);
      }
    }
    return nodesPassed;
  }

  /**
   * Trace a road back from a road segment against the traffic, shortest way first: the road segments traffic can come
   * from to reach it, never back along the link it leaves, so turning back does not count as a way in
   *
   * @param entering the road segment to trace back from
   * @param canTraceBackBeyond true for a road segment reached that the trace may go on back beyond
   * @param maximumMeters furthest the start of a road segment reached may lie from where the entering segment ends,
   *                      along the road; road segments starting beyond it may still be reached, but are not gone beyond
   * @return the road segments reached, the entering one included, see {@link #findMetersBackToStartOf}
   */
  private static ShortestPathBreadthFirstResult<MacroscopicLinkSegment> traceRoadBack(
      MacroscopicLinkSegment entering, Predicate<MacroscopicLinkSegment> canTraceBackBeyond, double maximumMeters) {
    return ShortestPathBreadthFirst.executeAlongEdgeSegments(entering, false,
        (from, onto) -> from.getParent() != onto.getParent() && IS_ROAD_SEGMENT.test(from), canTraceBackBeyond,
        segment -> segment.getParent().getLengthMeters(), maximumMeters - entering.getParent().getLengthMeters());
  }

  /**
   * The distance along the road from where a traced back road segment starts to where the segment it was traced back
   * from ends, both segments included, see {@link #traceRoadBack}
   *
   * @param segment reached by the trace
   * @param entering the road segment traced back from
   * @param tracedBack the result of the trace
   * @return the distance, in metres
   */
  private static double findMetersBackToStartOf(MacroscopicLinkSegment segment, MacroscopicLinkSegment entering,
      ShortestPathBreadthFirstResult<MacroscopicLinkSegment> tracedBack) {
    return entering.getParent().getLengthMeters() + tracedBack.getCostToReach(segment) +
        (segment == entering ? 0 : segment.getParent().getLengthMeters());
  }

  /**
   * True when an internal segment keeps a crossing's signal, so it becomes an internal segment with a signal: it enters
   * the node of a signalised crossing-only intersection, and lies on a link no motorised road mode uses in either
   * direction, such as a footway or cycleway. Such a side of a crossing has a light of its own, even though it is no
   * approach: approaches are for motorised road and rail traffic. Only a crossing's signal is kept: a crossing has one
   * node. A junction's signal standing on its node is not kept: a junction often has such a signal on every one of its
   * nodes, so keeping it would count one real signal several times. Nor is a crossing's signal kept on a road link into
   * the crossing, whoever uses that direction. The signal does not say which direction it applies to, and road traffic
   * stops for the crossing on the road from outside, which stays an approach. The road into it from inside the
   * junction is run by the junction's own signals, and walking along a road to the crossing passes no light
   *
   * @param segment the internal segment
   * @return true when it keeps the crossing's signal, false otherwise
   */
  private boolean keepsCrossingSignalOnceInternal(MacroscopicLinkSegment segment) {
    return !IS_ROAD_LINK.test(segment.getParent()) &&
        memberNodesOfSignalisedCrossingOnlyIntersections.contains(segment.getDownstreamNode());
  }

  /**
   * Make every link segment between two member nodes of an intersection an internal segment (C2 step 8). One with a
   * signal of its own, see {@link #hasSignalOfItsOwn}, or one that keeps a crossing's signal, see
   * {@link #keepsCrossingSignalOnceInternal}, becomes an internal segment with a signal. A raw edit of the
   * intersection
   *
   * @param intersection to add the internal segments to
   */
  private void addInternalSegments(SpatialIntersection intersection) {
    for (var memberNode : intersection.getMemberNodes()) {
      for (var segment : memberNode.<MacroscopicLinkSegment>getExitLinkSegments()) {
        if (intersection.isExitSegment(segment)) {
          continue;
        }
        if (hasSignalOfItsOwn.test(segment) || keepsCrossingSignalOnceInternal(segment)) {
          intersection.addInternalSegmentWithSignal(segment);
        } else {
          intersection.addInternalSegment(segment);
        }
        if (segment.getParent().getLengthMeters() > settings.getInternalLinkLengthWarningMeters()) {
          result.addInternalLinkLongerThanWarningLength(segment.getParent());
        }
      }
    }
  }

  /**
   * Merge intersections into a spatial intersection (C2 steps 7 and 8). The merged intersections are removed through
   * the modifier, each with its removal event. The spatial intersection gains their member nodes and the given nodes,
   * the union of their kinds, the control type signalised if any of them was, their external ids, each once, and their
   * approaches, each keeping whether it has a signal of its own. An approach that starts at one of its member nodes is
   * no longer an approach, and the segments between its member nodes become internal, see
   * {@link #addInternalSegments(SpatialIntersection)}
   *
   * @param kept spatial intersection to merge into
   * @param merged intersections to merge away
   * @param nodesToAdd further nodes to make member nodes
   * @return the nodes that became member nodes and were a member node of none of the intersections before, by id
   */
  private List<Node> mergeInto(SpatialIntersection kept, List<Intersection> merged, Collection<Node> nodesToAdd) {
    /* gather what the merged intersections bring, removing each through the modifier */
    var memberNodesBefore = new LinkedHashSet<>(kept.getMemberNodes());
    var hasOwnSignalOfMergedApproach = new LinkedHashMap<MacroscopicLinkSegment, Boolean>();
    var types = EnumSet.noneOf(IntersectionType.class);
    types.addAll(kept.getTypes());
    boolean signalised = kept.isSignalised();
    var externalIds = new ArrayList<String>();
    for (var other : merged) {
      if (other.hasExternalId()) {
        externalIds.addAll(List.of(other.getSplitExternalId()));
      }
      memberNodesBefore.addAll(other.getMemberNodes());
      other.getApproachSegments().forEach(
          approach -> hasOwnSignalOfMergedApproach.put(approach, other.hasOwnSignal(approach)));
      types.addAll(other.getTypes());
      signalised |= other.isSignalised();
      layer.getLayerModifier().removeIntersection(other);
    }
    /* the given nodes that were a member node of none of them */
    var joined = nodesToAdd.stream().filter(node -> !memberNodesBefore.contains(node)).distinct()
        .sorted(IdAbleUtils.byId()).collect(Collectors.toList());

    var memberNodes = new LinkedHashSet<>(memberNodesBefore);
    memberNodes.addAll(joined);

    /* apply it all to the kept intersection in one update, so the lookups of the intersections stay current */
    boolean keptSignalised = signalised;
    layer.getIntersections().update(kept, intersection -> {
      if (keptSignalised) {
        intersection.setControlType(IntersectionControlType.SIGNALISED);
      }
      types.forEach(intersection::addType);
      externalIds.stream().filter(externalId -> !intersection.containsExternalId(externalId))
          .forEach(intersection::appendExternalId);
      /* an approach may not start at a member node, so those go before the member nodes are added */
      for (var approach : List.copyOf(intersection.getApproachSegments())) {
        if (memberNodes.contains(approach.getUpstreamNode())) {
          intersection.removeApproachSegment(approach);
          result.addApproach(ApproachOutcome.MADE_INTERNAL, approach);
        }
      }
      memberNodes.forEach(intersection::addMemberNode);
      hasOwnSignalOfMergedApproach.forEach((approach, hasOwnSignal) -> {
        if (memberNodes.contains(approach.getUpstreamNode())) {
          result.addApproach(ApproachOutcome.MADE_INTERNAL, approach);
        } else if (intersection.addApproachSegment(approach)) {
          intersection.setApproachSegmentHasOwnSignal(approach, hasOwnSignal);
        }
      });
      addInternalSegments(intersection);
    });
    return joined;
  }

  /**
   * Join intersections into one spatial intersection, with the given nodes as member nodes too (C2 steps 7, 9a and 10
   * to 13). When one of them is spatial already, the others are merged into it. Otherwise a new spatial intersection
   * replaces all of them. Each of the layer's intersections merged away is listed as replaced, with the spatial
   * intersection that replaced it. A spatial intersection consolidation made and merged away again is not listed
   * itself: the intersections it replaced are listed as replaced by the one it is merged into, so each is listed once.
   * A single non-spatial intersection that would gain no node is left as it is
   *
   * @param joining the intersections to join, by id
   * @param nodesToAdd further nodes to make member nodes
   * @return the intersection they are joined into
   */
  private Intersection joinIntoSpatialIntersection(List<Intersection> joining, Collection<Node> nodesToAdd) {
    /* the spatial intersection to join into: one of them, or a new one that replaces them all */
    var spatial = joining.stream().filter(SpatialIntersection.class::isInstance).map(SpatialIntersection.class::cast)
        .findFirst().orElse(null);
    if (spatial == null) {
      var first = joining.get(0);
      if (joining.size() == 1 && nodesToAdd.stream().allMatch(first::hasMemberNode)) {
        return first;
      }
      spatial = layer.getIntersections().getFactory().createSpatial(first.getMemberNodes(), first.getControlType(),
          first.getTypes());
      spatial.setXmlId(String.valueOf(spatial.getId()));
      layer.getIntersections().getFactory().register(spatial);
      madeByConsolidation.add(spatial);
    }
    /* merge the others into it, each listed as replaced. When consolidation made one, the intersections it replaced are
       listed as replaced by the target instead */
    var target = spatial;
    var merged = joining.stream().filter(intersection -> intersection != target).collect(Collectors.toList());
    for (var other : merged) {
      if (madeByConsolidation.contains(other)) {
        result.reassignReplacedIntersections((SpatialIntersection) other, target);
      } else {
        result.addReplacedIntersection(other, target);
      }
    }
    mergeInto(target, merged, nodesToAdd).forEach(result::addJoinedNode);
    return target;
  }

  /**
   * Find the nodes a spatial intersection encloses, whatever modes their links carry. Each is a member node of no
   * intersection that lies within the radius of the intersection and connects two or more of its member nodes without
   * leaving the radius, over links each no longer than the joining reach. A node no motorised road or
   * rail mode uses, such as one on a footway or cycleway, is enclosed too when it lies on a short connection between
   * two member nodes, see {@link #findNodesNotOnRoadOrRailOnShortConnectionsBetweenMemberNodes}, however far from the
   * centre. A link from an enclosed node to beyond the radius does not stop it from joining: it becomes an arm. Nodes
   * in front of the junction stay out, see {@link #leaveOutJoiningNodesInFrontOfJunction}, and so do footway and
   * cycleway nodes that would hang off the intersection, see
   * {@link #leaveOutJoiningNodesNotOnRoadOrRailHangingOffIntersection}. Examples are the nodes of a
   * tram crossover or turnout between member nodes, a merge inside the junction, and a footway corner between two of
   * its crossings
   *
   * @param intersection that may enclose them
   * @return the nodes enclosed, by id, empty when there are none
   */
  private Set<Node> findEnclosedNodes(SpatialIntersection intersection) {
    /* per node within the radius, the member nodes it connects to without leaving the radius */
    var centreAndRadius = computeCentreAndRadius(intersection.getMemberNodes());
    double maximumLinkMeters = settings.getMaximumJoiningReachMeters();
    var memberNodesConnected = new HashMap<Node, Set<Node>>();
    for (var memberNode : intersection.getMemberNodes()) {
      MacroscopicNetworkLayerUtils.findShortestConnections(memberNode,
          edge -> edge.getLengthMeters() <= maximumLinkMeters + Precision.EPSILON_9,
          node -> layer.getIntersections().getByMemberNode(node) == null && isWithinRadius(centreAndRadius, node),
          Double.POSITIVE_INFINITY).getReached().stream().filter(node -> node != memberNode)
          .forEach(node -> memberNodesConnected.computeIfAbsent(node, key -> new HashSet<>()).add(memberNode));
    }
    var enclosed = memberNodesConnected.entrySet().stream().filter(entry -> entry.getValue().size() >= 2)
        .map(Map.Entry::getKey).collect(Collectors.toSet());
    enclosed.addAll(findNodesNotOnRoadOrRailOnShortConnectionsBetweenMemberNodes(intersection));
    var enclosedById = enclosed.stream().sorted(IdAbleUtils.byId())
        .collect(Collectors.toCollection(LinkedHashSet::new));
    var memberNodes = new HashSet<>(intersection.getMemberNodes());
    leaveOutJoiningNodesInFrontOfJunction(memberNodes, enclosedById, Set.of());
    leaveOutJoiningNodesNotOnRoadOrRailHangingOffIntersection(memberNodes, enclosedById);
    return enclosedById;
  }

  /**
   * Leave out the nodes about to join an intersection that no motorised road or rail mode uses and that would hang off
   * it (C2 step 12). Such a node hangs off when it links to fewer than two of the intersection's nodes, its member nodes
   * and the other nodes about to join: a node on a pavement leading away from a footway corner, for example. It also
   * hangs off when it is not connected to the member nodes through the other nodes about to join: footway nodes beyond
   * a node in front of the junction that stays outside, for example. Leaving one node out can leave the next one
   * hanging, so this repeats until no more nodes are left out. A node a motorised road or rail mode uses is not left
   * out this way
   *
   * @param memberNodes of the intersection
   * @param joiningNodes the nodes about to join, from which the nodes left out are removed
   */
  private static void leaveOutJoiningNodesNotOnRoadOrRailHangingOffIntersection(
      Set<Node> memberNodes, Set<Node> joiningNodes) {
    ContainerUtils.removeUntilNoneMatches(joiningNodes, () -> {
      /* the nodes about to join that the member nodes reach through them */
      var connected = new HashSet<Node>();
      for (var memberNode : memberNodes) {
        connected.addAll(MacroscopicNetworkLayerUtils.findShortestConnections(
            memberNode, edge -> true, joiningNodes::contains, Double.POSITIVE_INFINITY).getReached());
      }
      return node -> MacroscopicLinkUtils.isUsedByNoMotorisedRoadOrRailMode(node) && (!connected.contains(node) ||
          node.getEdges().stream().map(edge -> edge.getOtherVertex(node))
              .filter(other -> memberNodes.contains(other) || joiningNodes.contains(other)).distinct().count() < 2);
    });
  }

  /**
   * Find the nodes no motorised road or rail mode uses, such as those of footways and cycleways, that lie on a
   * connection between two member nodes of an intersection, through such nodes only, at most
   * {@value #MAXIMUM_CONNECTION_THROUGH_NODES_NOT_ON_ROAD_OR_RAIL_METERS} m long. A footway corner between two crossings
   * is an example: such corners can lie well beyond the radius. A node on a pavement leading away from the
   * intersection can be found too, by going out to it and back; it would hang off the intersection, so it is left out
   * afterwards, see {@link #leaveOutJoiningNodesNotOnRoadOrRailHangingOffIntersection}
   *
   * @param intersection whose member nodes the connections are between
   * @return the nodes found, empty when there are none
   */
  private Set<Node> findNodesNotOnRoadOrRailOnShortConnectionsBetweenMemberNodes(Intersection intersection) {
    /* per node reached, its distance from each member node it is reached from */
    var metersFromMemberNodes = new HashMap<Node, List<Double>>();
    for (var memberNode : intersection.getMemberNodes()) {
      var connections = MacroscopicNetworkLayerUtils.findShortestConnections(memberNode, edge -> true,
          node -> layer.getIntersections().getByMemberNode(node) == null &&
              MacroscopicLinkUtils.isUsedByNoMotorisedRoadOrRailMode(node),
          MAXIMUM_CONNECTION_THROUGH_NODES_NOT_ON_ROAD_OR_RAIL_METERS);
      connections.getReached().stream().filter(node -> node != memberNode).forEach(node -> metersFromMemberNodes
          .computeIfAbsent(node, key -> new ArrayList<>()).add(connections.getCostToReach(node)));
    }
    /* a node joins when its distances to its two nearest member nodes add up to at most the longest connection */
    return metersFromMemberNodes.entrySet().stream().filter(entry -> entry.getValue().size() >= 2 &&
        entry.getValue().stream().sorted().limit(2).mapToDouble(Double::doubleValue).sum() <=
            MAXIMUM_CONNECTION_THROUGH_NODES_NOT_ON_ROAD_OR_RAIL_METERS + Precision.EPSILON_9)
        .map(Map.Entry::getKey).collect(Collectors.toSet());
  }

  /**
   * Join to each of the given spatial intersections the nodes it encloses (C2 step 12), see {@link #findEnclosedNodes}.
   * The other steps follow the road, so they leave nodes such as those of a tram track between two member nodes out.
   * The links on either side of them would then be arms of the intersection instead of internal links. The nodes that
   * join can widen the radius and give further connections between member nodes, so the nodes an intersection
   * encloses are looked for again until none join. Each node is listed as joined
   *
   * @param intersections the spatial intersections to join the nodes they enclose to
   */
  private void joinEnclosedNodes(Collection<SpatialIntersection> intersections) {
    joinNodesFound(intersections, this::findEnclosedNodes, true);
  }

  /**
   * Join to each of the given spatial intersections the nodes of the links reached only through it (C2 step 9 again),
   * see {@link #findNodesOfLinksReachedOnlyThroughIntersection(Set)}. Meant for intersections that grew by pulling in a
   * junction after that step first ran: a road leaving one of their junctions and coming back into another, such as
   * one across a tram corridor, is reached only through the intersection once both junctions are in it
   *
   * @param intersections the spatial intersections to join the nodes to
   */
  private void joinNodesOfLinksReachedOnlyThroughIntersection(Collection<SpatialIntersection> intersections) {
    joinNodesFound(intersections,
        intersection -> findNodesOfLinksReachedOnlyThroughIntersection(new LinkedHashSet<>(intersection.getMemberNodes())),
        false);
  }

  /**
   * Join to each of the given spatial intersections the nodes found for it, see
   * {@link #joinIntoSpatialIntersection(List, Collection)}, once or until none are found
   *
   * @param intersections the spatial intersections to join the nodes to
   * @param findNodes finds the nodes to join to an intersection, empty when there are none
   * @param untilNoneFound true to look for nodes again once they have joined, until none are found, false to look once
   */
  private void joinNodesFound(Collection<SpatialIntersection> intersections,
      Function<SpatialIntersection, Set<Node>> findNodes, boolean untilNoneFound) {
    for (var intersection : intersections) {
      for (var found = findNodes.apply(intersection); !found.isEmpty();
          found = untilNoneFound ? findNodes.apply(intersection) : Set.of()) {
        joinIntoSpatialIntersection(List.<Intersection>of(intersection), found);
      }
    }
  }

  /**
   * Build the consolidated intersection of a cluster subset: the links reached only through it are found first, then
   * its parsed intersections are joined into one spatial intersection, with the subset's other nodes and the nodes
   * those links pass as member nodes
   *
   * @param subset the cluster subset
   */
  private void buildConsolidatedIntersection(IntersectionCluster.ClusterSubset subset) {
    var nodes = new LinkedHashSet<>(subset.nodes);
    nodes.addAll(findNodesOfLinksReachedOnlyThroughIntersection(subset.nodes));
    joinIntoSpatialIntersection(subset.parsedIntersections, nodes);
  }

  /**
   * Pull junctions into the junctions whose radius, plus a margin, they lie within (C2 step 9a). The radius is
   * the distance from the centre of the member nodes to the furthest of them, counting only the member nodes a
   * motorised road or rail mode uses, see {@link CentreAndRadius}. A junction on one node pulls in too, its radius
   * being nil, so two junctions a few metres apart that only footways link become one. It passes over the junctions it
   * was weighed with in an intersection cluster, see {@link #wereWeighedInOneCluster}: the subdivision chosen there
   * decided they stay apart. Another junction with a node within the
   * radius plus {@link IntersectionConsolidatorSettings#getPullInMarginBeyondRadiusMeters()} of the centre, reached
   * from a member node within {@link IntersectionConsolidatorSettings#getMaximumPullInConnectionLengthMeters()} over
   * any links, is part of the same junction: a signal inside a junction belongs to it, even when the scoring kept it
   * apart or no road brought it into the intersection cluster. It joins with the nodes on that connection, see
   * {@link #joinIntoSpatialIntersection(List, Collection)}, and is listed. A node on the connection that lies in front
   * of the junction stays outside, see {@link #leaveOutJoiningNodesInFrontOfJunction}. A junction whose connection
   * crosses a tram or light rail track may lie
   * {@link #PULL_IN_EXTRA_MARGIN_WHERE_CONNECTION_CROSSES_TRACK_METERS} further, see
   * {@link #connectionCrossesTramOrLightRailTrackBetween}, since the corridor between them makes for a larger junction
   * layout, see {@link #computePullInReachMeters}. The centre and radius move as junctions join, so this repeats until
   * a round pulls nothing in
   *
   * @return the intersections junctions were pulled into, empty when none
   */
  private List<Intersection> pullInJunctionsWithinRadius() {
    var grown = new ArrayList<Intersection>();
    List<Intersection> round;
    do {
      round = pullInRound(Intersection::isJunction, this::findJunctionsWithinRadius,
          junction -> result.addIntersection(IntersectionOutcome.JUNCTION_PULLED_IN_WITHIN_RADIUS, junction));
      grown.addAll(round);
    } while (!round.isEmpty());
    return grown;
  }

  /**
   * Find the other junctions with a node within the radius plus the margin that a member node of an intersection
   * connects to, see {@link #pullInJunctionsWithinRadius()}, each with the nodes on its shortest connection
   *
   * @param into the intersection to pull them into
   * @return the junctions with the nodes on their connections, empty when none
   */
  private Map<Intersection, List<Node>> findJunctionsWithinRadius(Intersection into) {
    var centreAndRadius = computeCentreAndRadius(into.getMemberNodes());
    var connectionOf = new LinkedHashMap<Intersection, List<Node>>();
    var lengthOf = new HashMap<Intersection, Double>();
    for (var memberNode : into.getMemberNodes()) {
      var connections = MacroscopicNetworkLayerUtils.findShortestConnections(
          memberNode, edge -> true, node -> true, settings.getMaximumPullInConnectionLengthMeters());
      for (var reached : connections.getReached()) {
        var other = layer.getIntersections().getByMemberNode(reached);
        if (other == null || other == into || !other.isJunction() ||
            (!(into instanceof SpatialIntersection) && wereWeighedInOneCluster(into, other))) {
          continue;
        }
        var connection = connections.getEntitiesOnConnectionTo(reached);
        if (!isWithinDistanceOfCentre(centreAndRadius,
            reached, computePullInReachMeters(centreAndRadius.radiusMeters, connection, into, other))) {
          continue;
        }
        double length = connections.getCostToReach(reached);
        if (length < lengthOf.getOrDefault(other, Double.POSITIVE_INFINITY) - Precision.EPSILON_9) {
          lengthOf.put(other, length);
          connectionOf.put(other, connection);
        }
      }
    }
    return connectionOf;
  }

  /**
   * Verify if two junctions hold parsed intersections that were weighed in one intersection cluster, see
   * {@link #findJunctionClusters}. The subdivision chosen for the cluster then decided whether they form one
   * junction. Junctions that no road within the joining reach links were never weighed together
   *
   * @param one junction
   * @param other junction
   * @return true when weighed in one cluster, false otherwise
   */
  private boolean wereWeighedInOneCluster(Intersection one, Intersection other) {
    var clustersOfOne = one.getMemberNodes().stream().map(clusterIndexOfParsedMemberNode::get)
        .filter(Objects::nonNull).collect(Collectors.toSet());
    return other.getMemberNodes().stream().map(clusterIndexOfParsedMemberNode::get).anyMatch(clustersOfOne::contains);
  }

  /**
   * One round of pulling junctions into intersections. Each candidate, taken as the intersections are at the start
   * of the round and still there, pulls in the junctions found for it, with the nodes found with them, see
   * {@link #pullIn}
   *
   * @param isCandidate true for an intersection junctions may be pulled into
   * @param findJunctionsToPullIn finds, for an intersection, the junctions to pull in, each with the nodes that come
   *                              along, empty when none
   * @param record lists each junction pulled in
   * @return the intersections junctions were pulled into, empty when none
   */
  private List<Intersection> pullInRound(Predicate<Intersection> isCandidate,
      Function<Intersection, Map<Intersection, List<Node>>> findJunctionsToPullIn, Consumer<Intersection> record) {
    var grown = new ArrayList<Intersection>();
    /* collected first, since pulling junctions in updates the intersections */
    for (var into : layer.getIntersections().stream(isCandidate).collect(Collectors.toList())) {
      if (!layer.getIntersections().contains(into)) {
        continue;
      }
      var junctionsToPullIn = findJunctionsToPullIn.apply(into);
      if (junctionsToPullIn.isEmpty()) {
        continue;
      }
      var joinedInto = pullIn(into, junctionsToPullIn.keySet(),
          junctionsToPullIn.values().stream().flatMap(List::stream).collect(Collectors.toList()), record);
      if (joinedInto != null) {
        grown.add(joinedInto);
      }
    }
    return grown;
  }

  /**
   * Pull junctions into a junction, with the nodes on their connections to it that do not lie in front of it, see
   * {@link #leaveOutJoiningNodesInFrontOfJunction} and {@link #joinIntoSpatialIntersection(List, Collection)}. Each
   * junction pulled in is recorded. None is pulled in when the group would lose every approach with a signal, see
   * {@link #wouldLoseEveryApproachWithSignal}
   *
   * @param into the junction pulling them in
   * @param pulledIn the junctions pulled in
   * @param connectionNodes the nodes on their connections to it, member nodes included
   * @param record records each junction pulled in in the result
   * @return the intersection they are joined into, null when none is pulled in
   */
  private Intersection pullIn(Intersection into, Collection<Intersection> pulledIn, Collection<Node> connectionNodes,
      Consumer<Intersection> record) {
    var memberNodes = new HashSet<Node>(into.getMemberNodes());
    pulledIn.forEach(other -> memberNodes.addAll(other.getMemberNodes()));
    var joiningNodes = findJoiningNodesNotInFrontOfJunction(memberNodes, connectionNodes, Set.of());
    var joining = new ArrayList<Intersection>(List.of(into));
    pulledIn.stream().sorted(IdAbleUtils.byId()).forEach(joining::add);
    memberNodes.addAll(joiningNodes);
    if (wouldLoseEveryApproachWithSignal(joining, memberNodes)) {
      return null;
    }
    pulledIn.forEach(record);
    return joinIntoSpatialIntersection(joining, joiningNodes);
  }

  /**
   * The nodes on connections to a junction that join it: those not a member node yet, less those that lie in front of
   * the junction, see {@link #leaveOutJoiningNodesInFrontOfJunction}
   *
   * @param memberNodes of the junction, and of the intersections joining it
   * @param connectionNodes the nodes on the connections to it, member nodes included
   * @param crossingNodes the nodes of a crossing joining, among the member nodes, empty when none
   * @return the joining nodes, in the order of the connections
   */
  private Set<Node> findJoiningNodesNotInFrontOfJunction(Set<Node> memberNodes, Collection<Node> connectionNodes,
      Set<Node> crossingNodes) {
    var joiningNodes = connectionNodes.stream().filter(node -> !memberNodes.contains(node))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    leaveOutJoiningNodesInFrontOfJunction(memberNodes, joiningNodes, crossingNodes);
    return joiningNodes;
  }

  /**
   * Verify if the connection of an intersection to a junction passes a node in front of that junction, see
   * {@link #leaveOutJoiningNodesInFrontOfJunction}: road traffic comes into the node from outside and then passes a
   * vehicle signal into the junction, or reaches it only from outside. The connection then cannot become internal.
   * When the intersection is a crossing-only one, its node counts as part of the junction, so a node traffic reaches
   * only from the crossing is no node in front of it, and the crossing's own stop line is no signal into the junction.
   * An example is a crossing on a side street that leaves the junction's approach before its stop line
   *
   * @param connectionNodes the nodes on the connection, from the intersection's member node to the junction's
   * @param junction the junction reached
   * @param other the intersection connected to it, a crossing-only intersection or another junction
   * @return true when a node on the connection lies in front of the junction, false otherwise
   */
  private boolean passesNodeInFrontOfJunction(List<Node> connectionNodes, Intersection junction, Intersection other) {
    var memberNodes = new HashSet<Node>(junction.getMemberNodes());
    memberNodes.addAll(other.getMemberNodes());
    var crossingNodes = other.isCrossingOnly() ? new HashSet<Node>(other.getMemberNodes()) : Set.<Node>of();
    long onConnectionOnly = connectionNodes.stream().filter(node -> !memberNodes.contains(node)).count();
    return findJoiningNodesNotInFrontOfJunction(memberNodes, connectionNodes, crossingNodes).size() < onConnectionOnly;
  }

  /**
   * Let each crossing-only intersection join the intersection with a parsed member node within the maximum internal
   * link length of the crossing's node along the road, the nearest when it reaches several (C2 step 10). A crossing
   * whose node no road uses, such as where a footway crosses a tram track, looks along any links instead. A junction
   * whose connection passes a node in front of it is passed over, see {@link #passesNodeInFrontOfJunction}: a crossing
   * on a side street leaving its approach before the stop line is no part of it. The crossing's node and the nodes on
   * the connection between them become member nodes, see {@link #joinIntoSpatialIntersection(List, Collection)}. A
   * crossing that reaches no junction stays as it is. Crossings join once the junctions are grouped, so a crossing
   * never brings a junction within reach of another
   *
   * @param crossings the crossing-only intersections, collected before any joins, since a crossing joining a junction
   *                  changes the intersections
   * @param parsedMemberNodes the member nodes of the parsed intersections, found in the intersection they are in now
   * @return the crossings left as they are
   */
  private List<Intersection> joinCrossings(List<Intersection> crossings, Set<Node> parsedMemberNodes) {
    var left = new ArrayList<Intersection>();
    for (var crossing : crossings) {
      /* find the nearest intersection along the road from the crossing's node, by its parsed member nodes, or along
         any links when no road reaches the crossing's node */
      Intersection nearest = null;
      List<Node> connectionNodes = null;
      double nearestMeters = Double.POSITIVE_INFINITY;
      for (var crossingNode : crossing.getMemberNodes()) {
        boolean onRoad = MacroscopicLinkUtils.isUsedByModeMatching(crossingNode, Mode::isMotorisedRoadMode);
        var connections = MacroscopicNetworkLayerUtils.findShortestConnections(crossingNode,
            onRoad ? IS_ROAD_LINK : edge -> true, node -> true, settings.getMaximumJoiningReachMeters());
        for (var reached : connections.getReached()) {
          var junction = parsedMemberNodes.contains(reached) ? layer.getIntersections().getByMemberNode(reached) : null;
          if (junction == null) {
            continue;
          }
          double meters = connections.getCostToReach(reached);
          if (meters < nearestMeters - Precision.EPSILON_9 || (meters <= nearestMeters +
              Precision.EPSILON_9 && junction.getId() < nearest.getId())) {
            var onConnection = connections.getEntitiesOnConnectionTo(reached);
            if (passesNodeInFrontOfJunction(onConnection, junction, crossing)) {
              continue;
            }
            nearest = junction;
            nearestMeters = meters;
            connectionNodes = onConnection;
          }
        }
      }
      /* join it, or leave the crossing as it is when none is within reach */
      if (nearest == null) {
        left.add(crossing);
        continue;
      }
      joinIntoSpatialIntersection(List.of(nearest, crossing), connectionNodes);
      result.addIntersection(IntersectionOutcome.CROSSING_JOINED, crossing);
    }
    return left;
  }

  /**
   * Find the junction a road leads into from a node, or comes out of to a node, through nodes where the road simply
   * continues, within a distance
   *
   * @param node to start from
   * @param skippedLink link not to start along, the slip lane itself
   * @param intoJunction true to follow the traffic into a junction, false to go back against it out of one
   * @param maximumDistanceMeters furthest to follow
   * @return the junction reached, null when none
   */
  private Intersection findJunctionAlongRoad(Node node, MacroscopicLink skippedLink, boolean intoJunction,
      double maximumDistanceMeters) {
    /* start along each road segment at the node, other than on the slip lane */
    for (var start : intoJunction ? NodeUtils.findExitLinkSegments(node, IS_ROAD_SEGMENT) :
        NodeUtils.findEntryLinkSegments(node, IS_ROAD_SEGMENT)) {
      if (start.getParent() == skippedLink) {
        continue;
      }
      /* follow the road while it simply continues, until it reaches a junction or goes beyond the distance */
      var junction = MacroscopicLinkSegmentUtils.<Intersection>followSegmentsUntilAnswered(start, intoJunction,
          start.getParent().getLengthMeters(), maximumDistanceMeters, (current, meters) -> {
            var reached = layer.getIntersections().getByMemberNode(
                intoJunction ? current.getDownstreamNode() : current.getUpstreamNode());
            return reached != null && reached.isJunction() ? reached : null;
          }, current -> MacroscopicLinkSegmentUtils.findOnlySegmentOnward(current, intoJunction, IS_ROAD_SEGMENT));
      if (junction != null) {
        return junction;
      }
    }
    return null;
  }

  /**
   * Find the junction whose road a slip lane leaves, for a road segment that may be on one: follow the segment back
   * against the traffic to the node where it branches off a road, then that road on into a junction, each within twice
   * the slip lane reach along the road. A slip lane is one-way for motorised modes, so a segment whose link lets a
   * motorised road mode go the other way is on no slip lane
   *
   * @param segment a road segment, on a slip lane or not
   * @return the junction the road the slip lane leaves leads into, null when none is reached or the segment is on no
   *         slip lane
   */
  private Intersection findJunctionWhoseRoadASlipLaneLeaves(MacroscopicLinkSegment segment) {
    var otherWay = (MacroscopicLinkSegment) segment.getOppositeDirectionSegment();
    if (otherWay != null && IS_ROAD_SEGMENT.test(otherWay)) {
      return null;
    }
    double roadSearchMeters = 2 * settings.getMaximumDistanceSlipLaneCrossingToJunctionMeters();
    var leavingRoad = MacroscopicLinkSegmentUtils.followUntilItBranches(
        segment, false, roadSearchMeters, IS_ROAD_SEGMENT);
    return leavingRoad == null ? null :
        findJunctionAlongRoad(leavingRoad.getUpstreamNode(), leavingRoad.getParent(), true, roadSearchMeters);
  }

  /**
   * Verify if a road segment entering a junction is on a slip lane of that junction: one that leaves a road leading
   * into it, see {@link #findJunctionWhoseRoadASlipLaneLeaves}
   *
   * @param segment the road segment
   * @param junction the junction it enters
   * @return true when on a slip lane of the junction, false otherwise
   */
  private boolean isOnSlipLaneOf(MacroscopicLinkSegment segment, Intersection junction) {
    return findJunctionWhoseRoadASlipLaneLeaves(segment) == junction;
  }

  /**
   * Let a crossing intersection on a slip lane join its junction (C2 step 11): the slip lane leaves a road leading into
   * the junction and rejoins a road coming out of it, each within twice the slip lane reach along the road, and the
   * crossing lies within the slip lane reach of a member node of the junction in a straight line. Along the road the
   * crossing is further away, round the end of the slip lane. The crossing's node becomes a member node, with the slip
   * lane segment into it an approach and the one out of it an exit; the slip lane's own ends stay outside
   *
   * @param crossings left by {@link #joinCrossings(List, Set)}
   */
  private void joinCrossingsOnSlipLanes(List<Intersection> crossings) {
    double roadSearchMeters = 2 * settings.getMaximumDistanceSlipLaneCrossingToJunctionMeters();
    for (var crossing : crossings) {
      /* a crossing on one node, with a road in and a road out on different links, as on a slip lane */
      if (crossing.getMemberNodes().size() != 1) {
        continue;
      }
      var crossingNode = crossing.getMemberNodes().get(0);
      var in = NodeUtils.findEntryLinkSegments(crossingNode, IS_ROAD_SEGMENT);
      var out = NodeUtils.findExitLinkSegments(crossingNode, IS_ROAD_SEGMENT);
      if (in.size() != 1 || out.size() != 1 || in.get(0).getParent() == out.get(0).getParent()) {
        continue;
      }
      /* the junction whose road the slip lane leaves, and where the slip lane rejoins a road */
      var junction = findJunctionWhoseRoadASlipLaneLeaves(in.get(0));
      var rejoiningRoad =
          MacroscopicLinkSegmentUtils.followUntilItBranches(
              out.get(0), true, roadSearchMeters, IS_ROAD_SEGMENT);
      if (junction == null || rejoiningRoad == null) {
        continue;
      }
      /* both roads must lead to the same junction, with the crossing within reach of one of its member nodes */
      if (junction != findJunctionAlongRoad(
          rejoiningRoad.getDownstreamNode(), rejoiningRoad.getParent(), false, roadSearchMeters)) {
        continue;
      }
      if (junction.getMemberNodes().stream().noneMatch(memberNode -> crsUtils.getDistanceInMetres(
          crossingNode.getPosition(), memberNode.getPosition()) <=
          settings.getMaximumDistanceSlipLaneCrossingToJunctionMeters())) {
        continue;
      }
      joinIntoSpatialIntersection(List.of(junction, crossing), List.of());
      result.addIntersection(IntersectionOutcome.CROSSING_JOINED_FROM_SLIP_LANE, crossing);
    }
  }

  /**
   * Trace the road of a segment entering a junction back against the traffic, see {@link #traceRoadBack}, to the
   * nearest signalised junction along the road within the pull-in reach plus
   * {@link #PULL_IN_EXTRA_MARGIN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL_METERS} of the junction's centre, see
   * {@link #computePullInReachMeters}. The trace goes no further than
   * {@link IntersectionConsolidatorSettings#getMaximumPullInConnectionLengthMeters()}, never through another
   * intersection and never back into the junction. Of junctions equally near, the one reached at the node of lowest id
   * is taken
   *
   * @param entering the road segment entering the junction
   * @param junction the junction it enters
   * @param centreAndRadius of the junction
   * @return the nodes on that road, from the member node it enters to the junction reached, both included, empty when
   * no signalised junction is reached within the reach
   */
  private List<Node> traceRoadBackToSignalisedJunctionWithinReach(
      MacroscopicLinkSegment entering, Intersection junction, CentreAndRadius centreAndRadius) {
    double maximumMeters = settings.getMaximumPullInConnectionLengthMeters();
    var tracedBack = traceRoadBack(entering,
        segment -> layer.getIntersections().getByMemberNode(segment.getUpstreamNode()) == null, maximumMeters);
    List<Node> nearestRoad = List.of();
    double nearestMeters = Double.POSITIVE_INFINITY;
    for (var segment : tracedBack.getReached()) {
      var reachedNode = segment.getUpstreamNode();
      var other = layer.getIntersections().getByMemberNode(reachedNode);
      double meters = findMetersBackToStartOf(segment, entering, tracedBack);
      if (other == null || other == junction || !other.isSignalisedJunction() ||
          meters > maximumMeters + Precision.EPSILON_9 || meters > nearestMeters + Precision.EPSILON_9 ||
          (meters >= nearestMeters - Precision.EPSILON_9 &&
              reachedNode.getId() > ListUtils.getLastValue(nearestRoad).getId())) {
        continue;
      }
      var road = new ArrayList<Node>(List.of(entering.getDownstreamNode()));
      tracedBack.getEntitiesOnConnectionTo(segment).forEach(passed -> road.add(passed.getUpstreamNode()));
      if (isWithinDistanceOfCentre(centreAndRadius, reachedNode,
          computePullInReachMeters(centreAndRadius.radiusMeters, road, junction, other) +
              PULL_IN_EXTRA_MARGIN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL_METERS)) {
        nearestRoad = road;
        nearestMeters = meters;
      }
    }
    return nearestRoad;
  }

  /**
   * One round of pulling junctions into the signalised junctions to salvage their approaches without a signal (C2 step
   * 13). A road segment entering a signalised junction from outside that is not an approach would become an approach
   * without a signal of its own (C3), unusual unless it is a slip lane. So its road is traced back against the traffic
   * to the first signalised junction within the pull-in reach plus
   * {@link #PULL_IN_EXTRA_MARGIN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL_METERS}, see
   * {@link #traceRoadBackToSignalisedJunctionWithinReach}. That junction is pulled in, with the nodes on that road
   * between them, so the road becomes internal. The further reach applies along that road only: the junction's other
   * arms keep the pull-in reach. A node on the road that lies in front of the junction stays outside, see
   * {@link #leaveOutJoiningNodesInFrontOfJunction}. Each junction pulled in is listed
   *
   * @return the intersections junctions were pulled into, empty when none
   */
  private List<Intersection> pullInJunctionsToSalvageApproachesWithoutSignal() {
    return pullInRound(Intersection::isSignalisedJunction, this::findJunctionsToSalvageApproachesWithoutSignal,
        junction -> result.addIntersection(IntersectionOutcome.JUNCTION_PULLED_IN_TO_SALVAGE_APPROACH_WITHOUT_SIGNAL,
            junction));
  }

  /**
   * Find, for each road that would become an approach without a signal of a signalised junction, the junction it
   * leads back to, see {@link #pullInJunctionsToSalvageApproachesWithoutSignal()}, each with the nodes on its roads. A
   * junction whose road passes a node in front of it is not found, see {@link #passesNodeInFrontOfJunction}: the road
   * would not become internal, so pulling the junction in would salvage nothing
   *
   * @param into the signalised junction to pull them into
   * @return the junctions with the nodes on the roads leading back to them, empty when none
   */
  private Map<Intersection, List<Node>> findJunctionsToSalvageApproachesWithoutSignal(Intersection into) {
    var withoutSignal = IntersectionUtils.findSegmentsEnteringFromOutsideThatAreNotApproaches(into, IS_ROAD_SEGMENT)
        .stream().filter(segment -> !isOnSlipLaneOf(segment, into)).collect(Collectors.toList());
    var roadsOf = new LinkedHashMap<Intersection, List<Node>>();
    if (withoutSignal.isEmpty()) {
      return roadsOf;
    }
    var centreAndRadius = computeCentreAndRadius(into.getMemberNodes());
    for (var segment : withoutSignal) {
      var road = traceRoadBackToSignalisedJunctionWithinReach(segment, into, centreAndRadius);
      if (road.isEmpty()) {
        continue;
      }
      var leadingBackTo = layer.getIntersections().getByMemberNode(ListUtils.getLastValue(road));
      if (!passesNodeInFrontOfJunction(road, leadingBackTo, into)) {
        roadsOf.computeIfAbsent(leadingBackTo, junction -> new ArrayList<>()).addAll(road);
      }
    }
    return roadsOf;
  }

  /**
   * Verify if a road segment entering a junction carries on the road of an approach whose signal was tagged on the
   * node it entered: following the same road on, past member nodes, see
   * {@link MacroscopicLinkUtils#isSameExternalIdOrName}, it reaches a segment that was an approach with a signal of
   * its own before consolidation, with no vehicle signal standing on it. Such a signal says that the node is
   * signalised, not where on the road traffic stops. Once that node has become a member node, the signal stands where
   * the road enters the junction
   *
   * @param entering the road segment entering the junction from outside
   * @param junction the junction it enters
   * @return true when it carries on the road of such an approach, false otherwise
   */
  private boolean continuesRoadOfApproachWithSignalTaggedOnNode(MacroscopicLinkSegment entering,
      Intersection junction) {
    UnaryOperator<MacroscopicLinkSegment> sameRoadOnInJunction = current ->
        MacroscopicLinkSegmentUtils.findContinuations(current, onward -> IS_ROAD_SEGMENT.test(onward) &&
            junction.hasMemberNode(onward.getDownstreamVertex()) &&
            MacroscopicLinkUtils.isSameExternalIdOrName(current.getParent(), onward.getParent()))
            .stream().findFirst().orElse(null);
    return Boolean.TRUE.equals(MacroscopicLinkSegmentUtils.followSegmentsUntilAnswered(entering, true,
        entering.getParent().getLengthMeters(), Double.POSITIVE_INFINITY, (current, meters) -> {
          var onward = sameRoadOnInJunction.apply(current);
          return onward != null && approachesWithSignalBeforeConsolidation.contains(onward) &&
              !hasSignalOfItsOwn.test(onward) ? Boolean.TRUE : null;
        }, sameRoadOnInJunction));
  }

  /**
   * Make, at each signalised junction, every road segment entering it from outside that is not an approach an
   * approach, and list the junctions where fewer than half of the road approaches have a signal of their own (C3).
   * Such a segment is an arm of the junction all the same, so it becomes an approach, and the movements from it are
   * stored like those of any approach. It has a signal of its own when it carries on the road of an approach whose
   * signal was tagged on the node it entered, see {@link #continuesRoadOfApproachWithSignalTaggedOnNode}, and is listed
   * as such. Otherwise it has none, and is listed as on a slip lane or not. A slip lane often has no signal of its own.
   * A road entering a signalised junction without a signal is otherwise unusual, so a signal may be missing from the
   * source
   */
  private void addRoadsEnteringAsApproachesAndFindJunctionsWithFewSignalisedArms() {
    /* collected first, since adding approaches updates the intersections */
    var junctions = layer.getIntersections().stream(Intersection::isSignalisedJunction).collect(Collectors.toList());
    for (var junction : junctions) {
      /* road segments entering a member node from outside that are not approaches become approaches, with a signal
         when they carry on the road of an approach whose signal was tagged on the node it entered */
      var entering = IntersectionUtils.findSegmentsEnteringFromOutsideThatAreNotApproaches(junction, IS_ROAD_SEGMENT);
      var withSignal = entering.stream().filter(segment -> continuesRoadOfApproachWithSignalTaggedOnNode(
          segment, junction)).collect(Collectors.toSet());
      layer.getIntersections().update(junction, intersection -> entering.forEach(segment -> {
        intersection.addApproachSegment(segment);
        intersection.setApproachSegmentHasOwnSignal(segment, withSignal.contains(segment));
      }));
      for (var segment : entering) {
        if (withSignal.contains(segment)) {
          result.addApproach(ApproachOutcome.ADDED_WITH_SIGNAL_TAGGED_ON_NODE_AHEAD, segment);
        } else {
          result.addApproach(isOnSlipLaneOf(segment, junction) ? ApproachOutcome.ADDED_WITHOUT_SIGNAL_ON_SLIP_LANE :
              ApproachOutcome.ADDED_WITHOUT_SIGNAL_NOT_ON_SLIP_LANE, segment);
        }
      }
      /* few lit arms: fewer than half of the road approaches with a signal of their own */
      var roadApproaches = junction.getApproachSegments().stream().filter(IS_ROAD_SEGMENT)
          .collect(Collectors.toList());
      if (2 * roadApproaches.stream().filter(junction::hasOwnSignal).count() < roadApproaches.size()) {
        result.addIntersection(IntersectionOutcome.JUNCTION_WITH_FEW_SIGNALISED_ARMS, junction);
      }
    }
  }

  /**
   * Store the movements of every spatial intersection, replacing those it had, each removed with its removal
   * event. From each approach and for each mode allowed on it, every exit its internal segments connect gives a
   * movement, banned turns ignored, so the list is complete; its length is the shortest connection over those modes,
   * see {@link SpatialMovementUtils#findShortestConnectionToEachExit}. The modes a movement is banned for implicitly
   * are not stored, see {@link SpatialMovementUtils#isImplicitlyBanned}
   */
  private void storeMovements() {
    layer.getIntersections().streamSpatial().forEach(intersection -> {
      /* remove the movements it had */
      List.copyOf(intersection.getMovements()).forEach(layer.getLayerModifier()::removeSpatialMovement);
      /* one movement for each exit an approach's internal segments connect, as long as the shortest connection */
      for (var approach : intersection.getApproachSegments()) {
        var shortestOfExit = SpatialMovementUtils.findShortestConnectionToEachExit(layer, intersection, approach);
        shortestOfExit.keySet().stream().sorted(IdAbleUtils.byId()).forEach(exit -> {
          var movement = layer.getSpatialMovements().getFactory().registerNew(
              approach, exit, shortestOfExit.get(exit).getCostToReach(exit) / 1000);
          movement.setXmlId(String.valueOf(movement.getId()));
          intersection.addMovement(movement);
          result.addStoredSpatialMovement(movement);
        });
      }
    });
  }

  /**
   * Verify if intersections joined into one, with the given member nodes, would lose every approach with a signal of
   * its own: at least one of them has such an approach, yet each starts at a member node, so it would become an
   * internal segment. Traffic entering such a group would meet no signal. For example, two junctions whose only
   * approaches with a signal are the two directions of the road between them
   *
   * @param joining the intersections to join
   * @param memberNodes of the intersection they would form
   * @return true when they have an approach with a signal of its own and none would stay an approach, false otherwise
   */
  static boolean wouldLoseEveryApproachWithSignal(Collection<? extends Intersection> joining, Set<Node> memberNodes) {
    var withSignal = joining.stream().flatMap(intersection -> intersection.getApproachSegments().stream()
        .filter(intersection::hasOwnSignal)).collect(Collectors.toList());
    return !withSignal.isEmpty() &&
        withSignal.stream().allMatch(approach -> memberNodes.contains(approach.getUpstreamNode()));
  }

  /**
   * Constructor
   *
   * @param layer to consolidate, through its own modifier
   * @param crs of the layer's geometry
   * @param settings the reach, scoring values and warning lengths to use
   * @param hasSignalOfItsOwn true for a link segment with a signal of its own, a vehicle signal standing on it or a
   *                          light for the footway or cycleway it is on, asked for segments that become internal and
   *                          for road segments into a junction
   */
  public IntersectionConsolidator(MacroscopicNetworkLayerImpl layer, CoordinateReferenceSystem crs,
      IntersectionConsolidatorSettings settings, Predicate<MacroscopicLinkSegment> hasSignalOfItsOwn) {
    this.layer = layer;
    this.crsUtils = new PlanitJtsCrsUtils(crs);
    this.settings = new IntersectionConsolidatorSettings(settings);
    this.hasSignalOfItsOwn = hasSignalOfItsOwn;
  }

  /**
   * Consolidate the intersections of the layer
   *
   * @return what was done
   */
  public IntersectionConsolidationModifierResult consolidate() {
    /* the parsed intersections, those with the kind junction, and the parsed intersection of each member node */
    var junctionIntersections = layer.getIntersections().stream(Intersection::isJunction).collect(Collectors.toList());
    var junctionIntersectionByMemberNode = new HashMap<Node, Intersection>();
    junctionIntersections.forEach(junction -> junction.getMemberNodes()
        .forEach(node -> junctionIntersectionByMemberNode.put(node, junction)));
    layer.getIntersections().stream(intersection -> intersection.isCrossingOnly() && intersection.isSignalised())
        .forEach(crossing -> memberNodesOfSignalisedCrossingOnlyIntersections.addAll(crossing.getMemberNodes()));
    layer.getIntersections().forEach(intersection -> intersection.getApproachSegments().stream()
        .filter(intersection::hasOwnSignal).forEach(approachesWithSignalBeforeConsolidation::add));

    /* each intersection cluster becomes the consolidated intersections of its chosen subdivision */
    var clusters = findJunctionClusters(junctionIntersections, junctionIntersectionByMemberNode);
    for (int index = 0; index < clusters.size(); ++index) {
      var cluster = clusters.get(index);
      final int clusterIndex = index;
      cluster.getParsedIntersections().forEach(parsed -> parsed.getMemberNodes()
          .forEach(node -> clusterIndexOfParsedMemberNode.put(node, clusterIndex)));
      if (cluster.isTooLargeToTryEverySubdivision()) {
        result.addIntersectionClusterSubdividedStepByStep(cluster.getParsedIntersections());
      }
      cluster.chooseSubdivision().forEach(this::buildConsolidatedIntersection);
    }

    /* junctions inside the radius of a junction are pulled in, until nothing more is */
    var grownByRadiusPullIn = new LinkedHashSet<Intersection>(pullInJunctionsWithinRadius());

    /* the nodes each intersection encloses, before any salvage: an enclosed node stays a member however the
       intersection grows, and enclosed road nodes widen the radius the pull-ins that follow reach with */
    joinEnclosedNodes(layer.getIntersections().streamSpatial().collect(Collectors.toList()));

    /* junctions are pulled in to salvage approaches without a signal, each round possibly bringing more junctions
       within the radius, until a round pulls none in. Each intersection that grew by a pull-in then joins the links
       now reached only through it, and the nodes it encloses */
    for (var salvaged = pullInJunctionsToSalvageApproachesWithoutSignal(); !salvaged.isEmpty();
        salvaged = pullInJunctionsToSalvageApproachesWithoutSignal()) {
      grownByRadiusPullIn.addAll(salvaged);
      grownByRadiusPullIn.addAll(pullInJunctionsWithinRadius());
    }
    var grownSpatial = grownByRadiusPullIn.stream().filter(layer.getIntersections()::contains)
        .filter(SpatialIntersection.class::isInstance).map(SpatialIntersection.class::cast)
        .collect(Collectors.toList());
    joinNodesOfLinksReachedOnlyThroughIntersection(grownSpatial);
    joinEnclosedNodes(grownSpatial);

    /* crossings join once the junctions are grouped, so a crossing never brings a junction within reach of another.
       Each intersection a crossing joined then joins the nodes it now encloses */
    var crossings = layer.getIntersections().stream(Intersection::isCrossingOnly).collect(Collectors.toList());
    joinCrossingsOnSlipLanes(joinCrossings(crossings, junctionIntersectionByMemberNode.keySet()));
    var nodesOfCrossingsJoined = crossings.stream().filter(crossing -> !layer.getIntersections().contains(crossing))
        .flatMap(crossing -> crossing.getMemberNodes().stream()).collect(Collectors.toSet());
    joinEnclosedNodes(layer.getIntersections().streamSpatial()
        .filter(intersection -> intersection.getMemberNodes().stream().anyMatch(nodesOfCrossingsJoined::contains))
        .collect(Collectors.toList()));

    /* roads entering without an approach become approaches, and movements are stored */
    addRoadsEnteringAsApproachesAndFindJunctionsWithFewSignalisedArms();
    storeMovements();

    return result;
  }
}
