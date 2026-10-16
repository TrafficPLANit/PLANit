package org.goplanit.network.layer.modifier.consolidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.algorithms.shortest.ShortestPathBreadthFirstResult;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.network.layer.macroscopic.SpatialMovementUtils;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentUtils;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidationModifierResult.LinkReachedOnlyThroughIntersection;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.NodeUtils;

/**
 * Consolidates the intersections of a layer: decides which parsed intersections form one junction, see
 * {@link IntersectionCluster}, and builds the consolidated intersections, letting crossings join them. Intersections
 * are removed through the layer modifier, so each removal fires its event. Used once per consolidation, run through
 * {@link org.goplanit.utils.network.layer.modifier.MacroscopicNetworkLayerModifier#consolidateIntersections}, not on
 * its own.
 *
 * @author markr
 */
public class IntersectionConsolidator {

  /** orders nodes by id, to break ties the same way every time */
  private static final Comparator<Node> NODES_BY_ID = Comparator.comparingLong(Node::getId);

  /** orders link segments by id, to break ties the same way every time */
  private static final Comparator<MacroscopicLinkSegment> SEGMENTS_BY_ID =
      Comparator.comparingLong(MacroscopicLinkSegment::getId);

  /** the layer to consolidate */
  private final MacroscopicNetworkLayerImpl layer;

  /** to measure directions and distances with, in the layer's coordinate reference system */
  private final PlanitJtsCrsUtils crsUtils;

  /** the reach, scoring values and warning lengths, a copy of those given */
  private final IntersectionConsolidatorSettings settings;

  /** true for a link segment with a vehicle signal standing on it, which the layer does not record. Asked only for
   * segments that become internal: such a segment is added as an internal segment with a signal, and a link reached
   * only through the intersection is listed as having a signal when one of its segments has one */
  private final Predicate<MacroscopicLinkSegment> hasVehicleSignal;

  /** what was done, filled where each change is made */
  private final IntersectionConsolidationModifierResult result = new IntersectionConsolidationModifierResult();

  /** true for a link a motorised road mode may use in at least one direction */
  static final Predicate<Edge> IS_ROAD_LINK = MacroscopicLinkUtils.permitsMotorisedRoadModeOnAnySegment();

  /** true for a segment a motorised road mode may use, false for none */
  static final Predicate<MacroscopicLinkSegment> IS_ROAD_SEGMENT =
      MacroscopicLinkSegmentUtils.permitsMotorisedRoadMode()::test;

  /**
   * Find the intersection clusters (C2 step 1): parsed intersections a member node of which lies within the maximum
   * internal link length of a member node of another, along the road in either direction, chained. A cluster's nodes
   * are its member nodes, the nodes on the shortest road connections of at most that length between them, and any node
   * joined by links of at most that length to member nodes of two or more of its parsed intersections
   *
   * @param parsedIntersections of the layer, by id
   * @param parsedIntersectionOfMemberNode each member node with its parsed intersection
   * @return the intersection clusters, by the lowest id of their parsed intersections
   */
  private List<IntersectionCluster> findIntersectionClusters(
      List<Intersection> parsedIntersections, Map<Node, Intersection> parsedIntersectionOfMemberNode) {
    /* join parsed intersections that reach each other's member nodes along the road within the reach */
    var reachingPairs = new ArrayList<Pair<Intersection, Intersection>>();
    var connectionsFrom = new HashMap<Node, ShortestPathBreadthFirstResult<Node>>();
    for (var entry : parsedIntersectionOfMemberNode.entrySet()) {
      var connections = MacroscopicNetworkLayerUtils.findShortestConnectionsAlongRoad(
          entry.getKey(), settings.getMaximumInternalLinkLengthMeters(), node -> true);
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

    /* nodes with short links to member nodes of two or more parsed intersections of one cluster */
    var linkedParsedIntersections = new HashMap<Node, Set<Intersection>>();
    for (var entry : parsedIntersectionOfMemberNode.entrySet()) {
      var memberNode = entry.getKey();
      for (var link : memberNode.<MacroscopicLink>getLinks()) {
        Node neighbour = link.getOtherNode(memberNode);
        if (neighbour != memberNode && IS_ROAD_LINK.test(link) &&
            link.getLengthMeters() <= settings.getMaximumInternalLinkLengthMeters() + Precision.EPSILON_9 &&
            !parsedIntersectionOfMemberNode.containsKey(neighbour)) {
          linkedParsedIntersections.computeIfAbsent(neighbour, node -> new HashSet<>()).add(entry.getValue());
        }
      }
    }
    /* add such a node to the cluster holding two or more of the parsed intersections it links to */
    for (var linked : linkedParsedIntersections.entrySet()) {
      var countByCluster = linked.getValue().stream()
          .collect(Collectors.groupingBy(clusterIndexOf::get, Collectors.counting()));
      countByCluster.forEach((clusterIndex, count) -> {
        if (count >= 2) {
          clusters.get(clusterIndex).second().add(linked.getKey());
        }
      });
    }
    /* create the intersection clusters from what was gathered */
    return clusters.stream().map(cluster -> new IntersectionCluster(
        cluster.first(), cluster.second(), parsedIntersectionOfMemberNode, crsUtils, settings))
        .collect(Collectors.toList());
  }

  /**
   * Find the links entering the given nodes that road traffic can only reach by driving through them (C2 step 9). From
   * each road segment entering the nodes from outside, trace back against the traffic over road segments, never into
   * the nodes, adding up the distance from the node it enters, the segment itself included. A way back ends at one of
   * the nodes. When a way back goes further than the maximum distance, or reaches a node no road traffic arrives at,
   * traffic comes from outside. Turning back onto the link just traced does not count as a way in, so a dead-end road
   * stays an approach. Each case found is listed in the result
   *
   * @param nodes of the consolidated intersection
   * @return the nodes passed by the links found, empty when none
   */
  private Set<Node> findNodesOfLinksReachedOnlyThroughIntersection(Set<Node> nodes) {
    var nodesPassed = new LinkedHashSet<Node>();
    /* each road segment entering the nodes from outside, itself within the reach, is a candidate */
    for (var node : nodes.stream().sorted(NODES_BY_ID).collect(Collectors.toList())) {
      for (var entering : NodeUtils.findEntryLinkSegments(node, IS_ROAD_SEGMENT)) {
        var start = entering.getUpstreamNode();
        double enteringMeters = entering.getParent().getLengthMeters();
        if (nodes.contains(start) ||
            enteringMeters > settings.getMaximumDistanceReachedOnlyThroughIntersectionMeters() + Precision.EPSILON_9) {
          continue;
        }

        /* trace back against the traffic from where it starts, shortest way first, until traffic comes from outside */
        var distance = new HashMap<Node, Double>();
        var tracedBackLeaving = new HashMap<Node, List<MacroscopicLinkSegment>>();
        var queue = new PriorityQueue<Map.Entry<Double, Node>>(Comparator.<Map.Entry<Double, Node>>comparingDouble(
            Map.Entry::getKey).thenComparing(Map.Entry::getValue, NODES_BY_ID));
        var segmentsTracedBack = new LinkedHashSet<MacroscopicLinkSegment>();
        var feedingNodes = new TreeSet<>(NODES_BY_ID);
        double longest = 0;
        boolean fromOutside = false;

        distance.put(start, enteringMeters);
        tracedBackLeaving.computeIfAbsent(start, key -> new ArrayList<>(2)).add(entering);
        segmentsTracedBack.add(entering);
        queue.add(Map.entry(enteringMeters, start));
        while (!queue.isEmpty() && !fromOutside) {
          var entry = queue.poll();
          double reached = entry.getKey();
          var current = entry.getValue();
          if (reached > distance.get(current) + Precision.EPSILON_9) {
            continue;
          }
          var arriving = new TreeSet<>(SEGMENTS_BY_ID);
          tracedBackLeaving.get(current).forEach(leaving -> arriving.addAll(
              MacroscopicLinkSegmentUtils.findPredecessors(leaving, IS_ROAD_SEGMENT)));
          if (arriving.isEmpty()) {
            fromOutside = true;
            break;
          }
          for (var segment : arriving) {
            double further = reached + segment.getParent().getLengthMeters();
            if (further > settings.getMaximumDistanceReachedOnlyThroughIntersectionMeters() + Precision.EPSILON_9) {
              fromOutside = true;
              break;
            }
            segmentsTracedBack.add(segment);
            var previous = segment.getUpstreamNode();
            if (nodes.contains(previous)) {
              feedingNodes.add(previous);
              longest = Math.max(longest, further);
              continue;
            }
            tracedBackLeaving.computeIfAbsent(previous, key -> new ArrayList<>(2)).add(segment);
            if (!distance.containsKey(previous)) {
              distance.put(previous, further);
              queue.add(Map.entry(further, previous));
            }
          }
        }
        if (fromOutside) {
          continue;
        }

        /* traffic reaches it only through the nodes: list the case and keep the nodes passed */
        result.addLinkReachedOnlyThroughIntersection(new LinkReachedOnlyThroughIntersection(entering,
            new ArrayList<>(feedingNodes), longest, new ArrayList<>(segmentsTracedBack),
            segmentsTracedBack.stream().anyMatch(hasVehicleSignal)));
        nodesPassed.addAll(distance.keySet());
      }
    }
    return nodesPassed;
  }

  /**
   * Make every link segment between two member nodes of an intersection an internal segment (C2 step 8), one a vehicle
   * signal stands on an internal segment with a signal. A raw edit of the intersection
   *
   * @param intersection to add the internal segments to
   */
  private void addInternalSegments(SpatialIntersection intersection) {
    for (var memberNode : intersection.getMemberNodes()) {
      for (var segment : memberNode.<MacroscopicLinkSegment>getExitLinkSegments()) {
        if (intersection.isExitSegment(segment)) {
          continue;
        }
        if (hasVehicleSignal.test(segment)) {
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
        .sorted(NODES_BY_ID).collect(Collectors.toList());

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
          result.addApproachSegmentMadeInternal(approach);
        }
      }
      memberNodes.forEach(intersection::addMemberNode);
      hasOwnSignalOfMergedApproach.forEach((approach, hasOwnSignal) -> {
        if (memberNodes.contains(approach.getUpstreamNode())) {
          result.addApproachSegmentMadeInternal(approach);
        } else if (intersection.addApproachSegment(approach)) {
          intersection.setApproachSegmentHasOwnSignal(approach, hasOwnSignal);
        }
      });
      addInternalSegments(intersection);
    });
    return joined;
  }

  /**
   * Join intersections into one spatial intersection, with the given nodes as member nodes too (C2 steps 7, 10 and
   * 11). When one of them is spatial already, the others are merged into it. Otherwise a new spatial intersection
   * replaces all of them. Each intersection merged away is listed as replaced, with the spatial intersection that
   * replaced it. A single non-spatial intersection that would gain no node is left as it is
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
    }
    /* merge the others into it, each listed as replaced */
    var target = spatial;
    var merged = joining.stream().filter(intersection -> intersection != target).collect(Collectors.toList());
    merged.forEach(other -> result.addReplacedIntersection(other, target));
    mergeInto(target, merged, nodesToAdd).forEach(result::addJoinedNode);
    return target;
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
   * Let each crossing-only intersection join the intersection with a parsed member node within the maximum internal
   * link length of the crossing's node along the road, the nearest when it reaches several (C2 step 10). The crossing's
   * node and the nodes on the road connection between them become member nodes, see
   * {@link #joinIntoSpatialIntersection(List, Collection)}. A crossing that reaches no junction stays as it is
   *
   * @param parsedMemberNodes the member nodes of the parsed intersections, found in the intersection they are in now
   * @return the crossings left as they are
   */
  private List<Intersection> joinCrossings(Set<Node> parsedMemberNodes) {
    var left = new ArrayList<Intersection>();
    var crossingOnly = EnumSet.of(IntersectionType.CROSSING);
    /* collected first, since a crossing joining a junction changes the intersections */
    var crossings = layer.getIntersections().stream(intersection -> intersection.getTypes().equals(crossingOnly))
        .collect(Collectors.toList());
    for (var crossing : crossings) {
      /* find the nearest intersection along the road from the crossing's node, by its parsed member nodes */
      Intersection nearest = null;
      List<Node> connectionNodes = null;
      double nearestMeters = Double.POSITIVE_INFINITY;
      for (var crossingNode : crossing.getMemberNodes()) {
        var connections = MacroscopicNetworkLayerUtils.findShortestConnectionsAlongRoad(
            crossingNode, settings.getMaximumInternalLinkLengthMeters(), node -> true);
        for (var reached : connections.getReached()) {
          var junction = parsedMemberNodes.contains(reached) ? layer.getIntersections().getByMemberNode(reached) : null;
          if (junction == null) {
            continue;
          }
          double meters = connections.getCostToReach(reached);
          if (meters < nearestMeters - Precision.EPSILON_9 || (meters <= nearestMeters +
              Precision.EPSILON_9 && junction.getId() < nearest.getId())) {
            nearest = junction;
            nearestMeters = meters;
            connectionNodes = connections.getEntitiesOnConnectionTo(reached);
          }
        }
      }
      /* join it, or leave the crossing as it is when none is within reach */
      if (nearest == null) {
        left.add(crossing);
        continue;
      }
      joinIntoSpatialIntersection(List.of(nearest, crossing), connectionNodes);
      result.addCrossingJoined(crossing);
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
      double meters = 0;
      var current = start;
      while (current != null) {
        meters += current.getParent().getLengthMeters();
        if (meters > maximumDistanceMeters + Precision.EPSILON_9) {
          break;
        }
        var reached = intoJunction ? current.getDownstreamNode() : current.getUpstreamNode();
        var junction = layer.getIntersections().getByMemberNode(reached);
        if (junction != null && junction.getTypes().contains(IntersectionType.JUNCTION)) {
          return junction;
        }
        var onward = MacroscopicLinkSegmentUtils.findOnward(current, intoJunction, IS_ROAD_SEGMENT);
        current = onward.size() == 1 ? onward.get(0) : null;
      }
    }
    return null;
  }

  /**
   * Let a crossing intersection on a slip lane join its junction (C2 step 11): the slip lane leaves a road leading into
   * the junction and rejoins a road coming out of it, each within twice the slip lane reach along the road, and the
   * crossing lies within the slip lane reach of a member node of the junction in a straight line. Along the road the
   * crossing is further away, round the end of the slip lane. The crossing's node becomes a member node, with the slip
   * lane segment into it an approach and the one out of it an exit; the slip lane's own ends stay outside
   *
   * @param crossings left by {@link #joinCrossings(Set)}
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
      /* follow the slip lane back to where it leaves a road, and on to where it rejoins one */
      var leavingRoad =
          MacroscopicLinkSegmentUtils.followUntilItBranches(
              in.get(0), false, roadSearchMeters, IS_ROAD_SEGMENT);
      var rejoiningRoad =
          MacroscopicLinkSegmentUtils.followUntilItBranches(
              out.get(0), true, roadSearchMeters, IS_ROAD_SEGMENT);
      if (leavingRoad == null || rejoiningRoad == null) {
        continue;
      }
      /* both roads must lead to the same junction, with the crossing within reach of one of its member nodes */
      var junction = findJunctionAlongRoad(
          leavingRoad.getUpstreamNode(), leavingRoad.getParent(), true, roadSearchMeters);
      if (junction == null || junction != findJunctionAlongRoad(
          rejoiningRoad.getDownstreamNode(), rejoiningRoad.getParent(), false, roadSearchMeters)) {
        continue;
      }
      if (junction.getMemberNodes().stream().noneMatch(memberNode -> crsUtils.getDistanceInMetres(
          crossingNode.getPosition(), memberNode.getPosition()) <=
          settings.getMaximumDistanceSlipLaneCrossingToJunctionMeters())) {
        continue;
      }
      joinIntoSpatialIntersection(List.of(junction, crossing), List.of());
      result.addCrossingJoinedFromSlipLane(crossing);
    }
  }

  /**
   * List, for each signalised junction, the road segments entering it from outside that are not approaches, and the
   * junctions where fewer than half of the road approaches have a signal of their own (C3). Nothing is changed
   */
  private void findMissingApproachesAndJunctionsWithFewLitArms() {
    layer.getIntersections().stream(intersection ->
        intersection.isSignalised() && intersection.getTypes().contains(IntersectionType.JUNCTION)).forEach(
            junction -> {
      /* road segments entering a member node from outside that are not approaches */
      for (var memberNode : junction.getMemberNodes()) {
        NodeUtils.findEntryLinkSegments(memberNode, IS_ROAD_SEGMENT).stream()
            .filter(segment -> !junction.hasMemberNode(segment.getUpstreamVertex()) &&
                !ListUtils.containsByIdentity(junction.getApproachSegments(), segment))
            .forEach(result::addMissingApproachSegment);
      }
      /* few lit arms: fewer than half of the road approaches with a signal of their own */
      var roadApproaches = junction.getApproachSegments().stream().filter(IS_ROAD_SEGMENT)
          .collect(Collectors.toList());
      if (2 * roadApproaches.stream().filter(junction::hasOwnSignal).count() < roadApproaches.size()) {
        result.addJunctionWithFewLitArms(junction);
      }
    });
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
        shortestOfExit.keySet().stream().sorted(SEGMENTS_BY_ID).forEach(exit -> {
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
   * Constructor
   *
   * @param layer to consolidate, through its own modifier
   * @param crs of the layer's geometry
   * @param settings the reach, scoring values and warning lengths to use
   * @param hasVehicleSignal true for a link segment with a vehicle signal standing on it, asked only for segments that
   *                         become internal
   */
  public IntersectionConsolidator(MacroscopicNetworkLayerImpl layer, CoordinateReferenceSystem crs,
      IntersectionConsolidatorSettings settings, Predicate<MacroscopicLinkSegment> hasVehicleSignal) {
    this.layer = layer;
    this.crsUtils = new PlanitJtsCrsUtils(crs);
    this.settings = new IntersectionConsolidatorSettings(settings);
    this.hasVehicleSignal = hasVehicleSignal;
  }

  /**
   * Consolidate the intersections of the layer
   *
   * @return what was done
   */
  public IntersectionConsolidationModifierResult consolidate() {
    /* the parsed intersections, those with the kind junction, and the parsed intersection of each member node */
    var parsedIntersections = layer.getIntersections().stream(
        intersection -> intersection.getTypes().contains(IntersectionType.JUNCTION)).collect(Collectors.toList());
    var parsedIntersectionOfMemberNode = new HashMap<Node, Intersection>();
    parsedIntersections.forEach(intersection -> intersection.getMemberNodes()
        .forEach(node -> parsedIntersectionOfMemberNode.put(node, intersection)));

    /* each intersection cluster becomes the consolidated intersections of its chosen subdivision */
    for (var cluster : findIntersectionClusters(parsedIntersections, parsedIntersectionOfMemberNode)) {
      if (cluster.isTooLargeToSubdivide()) {
        result.addIntersectionClusterTooLargeToSubdivide(cluster.getParsedIntersections());
        continue;
      }
      cluster.chooseSubdivision().forEach(this::buildConsolidatedIntersection);
    }

    /* crossings join, missing information is counted, and movements are stored */
    joinCrossingsOnSlipLanes(joinCrossings(parsedIntersectionOfMemberNode.keySet()));
    findMissingApproachesAndJunctionsWithFewLitArms();
    storeMovements();

    return result;
  }
}
