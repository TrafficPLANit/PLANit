package org.goplanit.network.layer.modifier.consolidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * What intersection consolidation keeps track of during one run, see {@link IntersectionConsolidator}: what the layer's
 * intersections were before consolidation, which later steps still need once intersections have joined, and what
 * consolidation has made so far. For example, an approach with a signal of its own is still known as one once its start
 * node has joined a junction. It also holds the nodes the settings keep in separate intersections, with the check every
 * step that joins intersections or nodes makes first, see {@link #isRefuseJoining}
 *
 * @author markr
 */
public class IntersectionConsolidatorTrackingData {

  /**
   * Group the listed nodes of each list to keep apart by the intersection they are in: listed nodes in one intersection
   * cannot be split, so they count as one, and a node in none counts on its own. A list left with fewer than two
   * groups has nothing to keep apart, so it is left out. For example, nodes A and B of one junction and node C of
   * another give two groups, {A, B} and {C}
   *
   * @param intersections the layer's intersections, before consolidation
   * @param listedNodesOfEachList per list to keep apart, the nodes found
   * @return per list with something to keep apart, its groups
   */
  private static List<List<Set<Node>>> groupNodesToKeepApart(
      Intersections intersections, List<List<Node>> listedNodesOfEachList) {
    var nodesKeptApartOfEachList = new ArrayList<List<Set<Node>>>();
    for (var listedNodes : listedNodesOfEachList) {
      var nodesKeptApartOfList = new ArrayList<Set<Node>>();
      var nodesKeptApartOfIntersection = new HashMap<Intersection, Set<Node>>();
      for (var node : listedNodes) {
        var intersection = intersections.getByMemberNode(node);
        var keptApart = intersection == null ? null : nodesKeptApartOfIntersection.get(intersection);
        if (keptApart == null) {
          keptApart = new HashSet<Node>();
          nodesKeptApartOfList.add(keptApart);
          if (intersection != null) {
            nodesKeptApartOfIntersection.put(intersection, keptApart);
          }
        }
        keptApart.add(node);
      }
      if (nodesKeptApartOfList.size() >= 2) {
        nodesKeptApartOfEachList.add(nodesKeptApartOfList);
      }
    }
    return nodesKeptApartOfEachList;
  }

  /** the member nodes of the signalised crossing-only intersections before consolidation */
  private final Set<Node> memberNodesOfSignalisedCrossingOnlyIntersections = new HashSet<>();

  /** the approaches with a signal of their own of the layer's intersections before consolidation */
  private final Set<LinkSegment> approachesWithSignalBeforeConsolidation = new HashSet<>();

  /** the approaches with a signal of their own of the layer's ramp meters before consolidation */
  private final Set<LinkSegment> approachesWithSignalOfRampMetersBeforeConsolidation = new HashSet<>();

  /** per member node of a parsed intersection, the index of the intersection cluster it was weighed in */
  private final Map<Node, Integer> clusterIndexOfParsedMemberNode = new HashMap<>();

  /** the spatial intersections consolidation made, as opposed to those the layer had */
  private final Set<Intersection> madeByConsolidation = Collections.newSetFromMap(new IdentityHashMap<>());

  /** per list to keep in separate intersections, the groups of its nodes no intersection may hold two of, see
   * {@link #groupNodesToKeepApart} */
  private final List<List<Set<Node>>> nodesKeptApartOfEachList;

  /** every node of the groups kept apart, so most checks are settled by one lookup per node */
  private final Set<Node> nodesKeptApart = new HashSet<>();

  /**
   * Constructor, taking what it needs from the layer's intersections as they are before consolidation
   *
   * @param layer to consolidate, not yet consolidated
   * @param listedNodesToKeepApartOfEachList per list to keep in separate intersections, the nodes found
   */
  public IntersectionConsolidatorTrackingData(
      MacroscopicNetworkLayer layer, List<List<Node>> listedNodesToKeepApartOfEachList) {
    layer.getIntersections().stream(intersection -> intersection.isCrossingOnly() && intersection.isSignalised())
        .forEach(crossing -> memberNodesOfSignalisedCrossingOnlyIntersections.addAll(crossing.getMemberNodes()));
    layer.getIntersections().forEach(intersection -> intersection.getApproachSegments().stream()
        .filter(intersection::hasOwnSignal).forEach(approachesWithSignalBeforeConsolidation::add));
    layer.getIntersections().stream(Intersection::isRampMeterOnly).forEach(rampMeter -> rampMeter.getApproachSegments()
        .stream().filter(rampMeter::hasOwnSignal).forEach(approachesWithSignalOfRampMetersBeforeConsolidation::add));
    nodesKeptApartOfEachList = groupNodesToKeepApart(layer.getIntersections(), listedNodesToKeepApartOfEachList);
    nodesKeptApartOfEachList.forEach(nodesKeptApartOfList -> nodesKeptApartOfList.forEach(nodesKeptApart::addAll));
  }

  /**
   * Verify if a node was a member node of a signalised crossing-only intersection before consolidation, so a segment
   * into the crossing is still known once the crossing has joined a junction
   *
   * @param node to verify
   * @return true when it was, false otherwise
   */
  public boolean isMemberNodeOfSignalisedCrossingOnlyIntersectionBeforeConsolidation(Node node) {
    return memberNodesOfSignalisedCrossingOnlyIntersections.contains(node);
  }

  /**
   * Verify if a segment was an approach with a signal of its own of one of the layer's intersections before
   * consolidation, so it is still known as one once its start node has joined
   *
   * @param segment to verify
   * @return true when it was, false otherwise
   */
  public boolean isApproachWithSignalBeforeConsolidation(LinkSegment segment) {
    return approachesWithSignalBeforeConsolidation.contains(segment);
  }

  /**
   * Verify if a segment was an approach with a signal of its own of a ramp meter before consolidation, so it is still
   * known as one once the ramp meter has joined a junction
   *
   * @param segment to verify
   * @return true when it was, false otherwise
   */
  public boolean isApproachWithSignalOfRampMeterBeforeConsolidation(LinkSegment segment) {
    return approachesWithSignalOfRampMetersBeforeConsolidation.contains(segment);
  }

  /**
   * Record the intersection cluster in which the member nodes of the given parsed intersections were weighed
   *
   * @param parsedIntersections of the cluster
   * @param clusterIndex the cluster's index
   */
  public void addParsedIntersectionsWeighedInCluster(Collection<Intersection> parsedIntersections, int clusterIndex) {
    parsedIntersections.forEach(parsed -> parsed.getMemberNodes()
        .forEach(node -> clusterIndexOfParsedMemberNode.put(node, clusterIndex)));
  }

  /**
   * Verify if two junctions hold parsed intersections that were weighed in one intersection cluster. The subdivision
   * chosen for the cluster then decided whether they form one junction. Junctions that no road within the joining
   * reach links were never weighed together
   *
   * @param one junction
   * @param other junction
   * @return true when weighed in one cluster, false otherwise
   */
  public boolean wereWeighedInOneCluster(Intersection one, Intersection other) {
    var clustersOfOne = one.getMemberNodes().stream().map(clusterIndexOfParsedMemberNode::get)
        .filter(Objects::nonNull).collect(Collectors.toSet());
    return other.getMemberNodes().stream().map(clusterIndexOfParsedMemberNode::get).anyMatch(clustersOfOne::contains);
  }

  /**
   * Record a spatial intersection consolidation made, as opposed to one the layer had
   *
   * @param intersection made
   */
  public void addMadeByConsolidation(Intersection intersection) {
    madeByConsolidation.add(intersection);
  }

  /**
   * Verify if consolidation made an intersection, as opposed to the layer having it. One merged away again is not
   * listed as replaced itself
   *
   * @param intersection to verify
   * @return true when consolidation made it, false otherwise
   */
  public boolean isMadeByConsolidation(Intersection intersection) {
    return madeByConsolidation.contains(intersection);
  }

  /**
   * Verify if any of the given nodes is a node the settings keep in a separate intersection. When none is, nothing
   * made of these nodes can ever be refused joining, see {@link #isRefuseJoining}
   *
   * @param nodes to verify
   * @return true when at least one is kept apart, false otherwise
   */
  public boolean isAnyNodeKeptApart(Collection<Node> nodes) {
    return !nodesKeptApart.isEmpty() && nodes.stream().anyMatch(nodesKeptApart::contains);
  }

  /**
   * Verify if joining intersections, with further nodes, into one intersection is refused because it would put two
   * groups of nodes a list keeps apart into one: the intersection would hold nodes of both
   *
   * @param joining the intersections to join
   * @param nodesToAdd further nodes to make member nodes, member nodes of the intersections joining may be among them
   * @return true when refused, false otherwise
   */
  public boolean isRefuseJoining(Collection<? extends Intersection> joining, Collection<Node> nodesToAdd) {
    if (nodesKeptApart.isEmpty()) {
      return false;
    }
    /* refusing takes nodes of two groups kept apart, so with fewer than two such nodes joining it is settled at once */
    var nodesKeptApartJoining = Stream.concat(
        joining.stream().flatMap(intersection -> intersection.getMemberNodes().stream()), nodesToAdd.stream())
        .filter(nodesKeptApart::contains).collect(Collectors.toSet());
    if (nodesKeptApartJoining.size() < 2) {
      return false;
    }
    return nodesKeptApartOfEachList.stream().anyMatch(nodesKeptApartOfList -> nodesKeptApartOfList.stream()
        .filter(keptApart -> keptApart.stream().anyMatch(nodesKeptApartJoining::contains)).count() >= 2);
  }

  /**
   * The nodes that may join the intersections joining: each node in turn is kept, unless joining it together with the
   * nodes kept before it is refused, see {@link #isRefuseJoining}
   *
   * @param joining the intersections to join
   * @param nodesToAdd the nodes to make member nodes, in the order to take them
   * @return the nodes allowed to join, in order
   */
  public List<Node> findNodesAllowedToJoin(Collection<? extends Intersection> joining, Collection<Node> nodesToAdd) {
    /* with no node kept apart among those to add, adding them changes nothing about refusing, so all or none join */
    if (!isAnyNodeKeptApart(nodesToAdd)) {
      return isRefuseJoining(joining, List.of()) ? List.of() : List.copyOf(nodesToAdd);
    }
    return ListUtils.filterEachTogetherWithThoseKept(nodesToAdd, kept -> !isRefuseJoining(joining, kept));
  }

  /**
   * The junctions that may be pulled into an intersection: each junction in turn, by id, is kept, unless pulling it in
   * together with the junctions kept before it is refused, see {@link #isRefuseJoining}
   *
   * @param into the intersection pulling them in
   * @param junctions the junctions to pull in
   * @return the junctions allowed to be pulled in, by id
   */
  public List<Intersection> findJunctionsAllowedToBePulledIn(Intersection into, Collection<Intersection> junctions) {
    var byId = junctions.stream().sorted(IdAbleUtils.byId()).collect(Collectors.toList());
    return ListUtils.filterEachTogetherWithThoseKept(byId, kept -> {
      var joining = new ArrayList<Intersection>(kept);
      joining.add(into);
      return !isRefuseJoining(joining, List.of());
    });
  }
}
