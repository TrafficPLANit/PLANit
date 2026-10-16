package org.goplanit.network.layer.modifier.consolidator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.directed.algorithms.BreadthFirstSearch;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.math.RoundUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * An intersection cluster: parsed intersections within the maximum internal link length of each other along the road,
 * and the nodes between them. It scores every way to divide its parsed intersections into cluster subsets, and chooses
 * the best. Deciding changes nothing in the layer.
 * <p>
 * The coverage of a cluster subset measures how well the traffic entering it can leave it in every direction the
 * cluster offers. For each segment entering the subset's nodes, the exits it reaches over the subset's internal links
 * are found, leaving out the exit back onto its own link. Each exit of the cluster then counts the cosine between its
 * direction and the closest direction reached, or 0 when that is negative. An entry's coverage is the average over the
 * cluster's exits, and the subset's coverage the average over its entries.
 * </p>
 *
 * @author markr
 */
class IntersectionCluster {

  /** a road segment with at least one end at a node of the cluster, with the direction from its start to its end
   * node */
  static final class ClusterSegment {

    /** the segment */
    final MacroscopicLinkSegment segment;

    /** its start node */
    final Node from;

    /** its end node */
    final Node to;

    /** east component of its direction, a unit vector */
    final double directionX;

    /** north component of its direction, a unit vector */
    final double directionY;

    /**
     * Constructor
     *
     * @param segment the segment
     * @param crsUtils to measure its direction with
     */
    ClusterSegment(MacroscopicLinkSegment segment, PlanitJtsCrsUtils crsUtils) {
      this.segment = segment;
      this.from = (Node) segment.getUpstreamVertex();
      this.to = (Node) segment.getDownstreamVertex();
      var fromPosition = from.getPosition().getCoordinate();
      var toPosition = to.getPosition().getCoordinate();
      if (fromPosition.equals2D(toPosition)) {
        this.directionX = 0;
        this.directionY = 0;
      } else {
        double azimuthRadians = Math.toRadians(crsUtils.getAzimuthInDegrees(fromPosition, toPosition, true));
        this.directionX = Math.sin(azimuthRadians);
        this.directionY = Math.cos(azimuthRadians);
      }
    }
  }

  /**
   * One cluster subset: its parsed intersections, with what a search along the road from their member nodes found for
   * them, see {@link IntersectionCluster#getSubset(List)}. That search depends on the cluster's other parsed
   * intersections too, so its outcome is kept, worked out once for each subset however many subdivisions contain it
   */
  static final class ClusterSubset {

    /** its parsed intersections, by id */
    final List<Intersection> parsedIntersections;

    /** its member nodes, the nodes on the short road connections between them and the nodes linking them, found by the
     * search */
    final Set<Node> nodes;

    /** true when the search connected its parsed intersections through its nodes */
    final boolean connected;

    /** its links of at most the maximum internal link length between two of its nodes. They follow from its nodes, and
     * are kept because every step of a search over the subset checks them */
    final Set<MacroscopicLink> internalLinks;

    /**
     * Constructor
     *
     * @param parsedIntersections of the subset
     * @param nodes of the subset
     * @param connected true when its parsed intersections are connected
     * @param internalLinks of the subset
     */
    ClusterSubset(List<Intersection> parsedIntersections, Set<Node> nodes, boolean connected,
        Set<MacroscopicLink> internalLinks) {
      this.parsedIntersections = parsedIntersections;
      this.nodes = nodes;
      this.connected = connected;
      this.internalLinks = internalLinks;
    }

    /**
     * The longest internal link
     *
     * @return its length in metres, 0 when there are none
     */
    double getLongestInternalLinkMeters() {
      return internalLinks.stream().mapToDouble(MacroscopicLink::getLengthMeters).max().orElse(0);
    }
  }

  /** the score of a cluster subset of two or more */
  private static final class SubsetScore {

    /** coverage of the subset */
    final double coverage;

    /** true when every member earns its place */
    final boolean allowed;

    /**
     * Constructor
     *
     * @param coverage of the subset
     * @param allowed true when every member earns its place
     */
    SubsetScore(double coverage, boolean allowed) {
      this.coverage = coverage;
      this.allowed = allowed;
    }
  }

  /** a subdivision considered, with its total and its tie breakers */
  private static final class ScoredSubdivision {

    /** the cluster subsets, largest first */
    final List<ClusterSubset> subsets;

    /** total over the parsed intersections of the coverage of their subset, or their own when alone */
    final double total;

    /** the longest internal link of any of its subsets, in metres */
    final double longestInternalLinkMeters;

    /**
     * Constructor
     *
     * @param subsets of the subdivision
     * @param total of the subdivision
     * @param longestInternalLinkMeters of the subdivision
     */
    ScoredSubdivision(List<ClusterSubset> subsets, double total, double longestInternalLinkMeters) {
      this.subsets = subsets;
      this.total = total;
      this.longestInternalLinkMeters = longestInternalLinkMeters;
    }
  }

  /** the parsed intersections of the cluster, by id */
  private final List<Intersection> parsedIntersections;

  /** every member node of a parsed intersection of the layer, with that intersection */
  private final Map<Node, Intersection> parsedIntersectionOfMemberNode;

  /** nodes of the cluster */
  private final Set<Node> nodes;

  /** road links of at most the maximum internal link length with both ends at nodes of the cluster */
  private final List<MacroscopicLink> shortLinks;

  /** nodes of the cluster that are not member nodes, with the member nodes they have short links to, when those belong
   * to two or more of its parsed intersections */
  private final Map<Node, Set<Node>> linkedMemberNodes = new LinkedHashMap<>();

  /** every road segment with at least one end at a node of the cluster */
  private final List<ClusterSegment> segments = new ArrayList<>();

  /** the segments leaving the cluster's nodes */
  private final List<ClusterSegment> clusterExits;

  /** the reach between parsed intersections, the least gain per member and the tie margin to use */
  private final IntersectionConsolidatorSettings settings;

  /** cluster subsets worked out so far, by the ids of their parsed intersections */
  private final Map<List<Long>, ClusterSubset> subsets = new HashMap<>();

  /** scores worked out so far, by the ids of the parsed intersections of the subset */
  private final Map<List<Long>, SubsetScore> scores = new HashMap<>();

  /**
   * Constructor
   *
   * @param parsedIntersections of the cluster
   * @param nodes of the cluster: member nodes, the nodes on the short road connections between them, and the nodes with
   *              short links to member nodes of two or more of its parsed intersections
   * @param parsedIntersectionOfMemberNode every member node of a parsed intersection of the layer, with that
   *                                       intersection
   * @param crsUtils to measure directions with
   * @param settings the reach between parsed intersections, which is also the longest link used for scoring, the least
   *                 gain per member and the tie margin to use
   */
  IntersectionCluster(List<Intersection> parsedIntersections, Set<Node> nodes,
      Map<Node, Intersection> parsedIntersectionOfMemberNode, PlanitJtsCrsUtils crsUtils,
      IntersectionConsolidatorSettings settings) {
    this.parsedIntersections = parsedIntersections.stream()
        .sorted(Comparator.comparingLong(Intersection::getId)).collect(Collectors.toList());
    this.parsedIntersectionOfMemberNode = parsedIntersectionOfMemberNode;
    this.nodes = nodes;
    this.settings = settings;
    var memberNodes = IntersectionUtils.collectMemberNodes(this.parsedIntersections);

    /* the road links touching the cluster's nodes, and the short ones among them with both ends in the cluster */
    var linksTouching = new LinkedHashSet<MacroscopicLink>();
    for (var node : nodes) {
      for (var edge : node.getEdges()) {
        var link = (MacroscopicLink) edge;
        if (IntersectionConsolidator.IS_ROAD_LINK.test(link)) {
          linksTouching.add(link);
        }
      }
    }
    this.shortLinks = linksTouching.stream().filter(link -> nodes.contains(link.getNodeA()) &&
        nodes.contains(link.getNodeB()) &&
        link.getLengthMeters() <= settings.getMaximumInternalLinkLengthMeters())
        .collect(Collectors.toList());
    /* their road segments with their directions, and those leaving the cluster as its exits */
    for (var link : linksTouching) {
      for (var segment : new MacroscopicLinkSegment[] {link.getLinkSegmentAb(), link.getLinkSegmentBa()}) {
        if (IntersectionConsolidator.IS_ROAD_SEGMENT.test(segment)) {
          segments.add(new ClusterSegment(segment, crsUtils));
        }
      }
    }
    this.clusterExits = segments.stream()
        .filter(segment -> nodes.contains(segment.from) && !nodes.contains(segment.to)).collect(Collectors.toList());

    /* nodes outside the member nodes with short links to member nodes of two or more parsed intersections */
    for (var node : nodes) {
      if (memberNodes.contains(node)) {
        continue;
      }
      var linked = new HashSet<Node>();
      for (var link : shortLinks) {
        Node other = link.getOtherNode(node);
        if (other != null && other != node && memberNodes.contains(other)) {
          linked.add(other);
        }
      }
      if (linked.stream().map(parsedIntersectionOfMemberNode::get).distinct().count() >= 2) {
        linkedMemberNodes.put(node, linked);
      }
    }
  }

  /**
   * The cluster subset of the given parsed intersections: its nodes are its member nodes, the nodes on the shortest
   * road connections of at most the maximum internal link length between them that stay on the cluster's nodes and do
   * not pass member nodes of the cluster's other parsed intersections, and the nodes with short links to two or more of
   * its member nodes. Such connections and nodes connect the parsed intersections they join
   *
   * @param subset parsed intersections of the cluster
   * @return the cluster subset
   */
  ClusterSubset getSubset(List<Intersection> subset) {
    return subsets.computeIfAbsent(IdAbleUtils.collectSortedIds(subset), key -> {
      var sorted = subset.stream().sorted(Comparator.comparingLong(Intersection::getId)).collect(Collectors.toList());
      /* the cluster's nodes a connection may pass: all but the member nodes of its other parsed intersections */
      var subsetMemberNodes = IntersectionUtils.collectMemberNodes(sorted);
      var allowed = new HashSet<>(nodes);
      IntersectionUtils.collectMemberNodes(parsedIntersections).stream()
          .filter(node -> !subsetMemberNodes.contains(node)).forEach(allowed::remove);

      /* add the nodes on short road connections between its member nodes, joining the parsed intersections they link */
      var subsetNodes = new LinkedHashSet<>(subsetMemberNodes);
      var joinedPairs = new ArrayList<Pair<Intersection, Intersection>>();
      for (var node : subsetMemberNodes) {
        var connections = MacroscopicNetworkLayerUtils.findShortestConnectionsAlongRoad(
            node, settings.getMaximumInternalLinkLengthMeters(), allowed::contains);
        for (var reached : connections.getReached()) {
          if (reached != node && subsetMemberNodes.contains(reached)) {
            subsetNodes.addAll(connections.getEntitiesOnConnectionTo(reached));
            joinedPairs.add(
                Pair.of(parsedIntersectionOfMemberNode.get(reached), parsedIntersectionOfMemberNode.get(node)));
          }
        }
      }
      /* add the nodes linked to two or more of its member nodes, joining those parsed intersections too */
      for (var entry : linkedMemberNodes.entrySet()) {
        var inside = entry.getValue().stream().filter(subsetMemberNodes::contains)
            .sorted(Comparator.comparingLong(Node::getId)).collect(Collectors.toList());
        if (inside.size() >= 2) {
          subsetNodes.add(entry.getKey());
          for (var other : inside.subList(1, inside.size())) {
            joinedPairs.add(
                Pair.of(parsedIntersectionOfMemberNode.get(other), parsedIntersectionOfMemberNode.get(inside.get(0))));
          }
        }
      }
      /* connected when all its parsed intersections were joined; internal links are the short ones between its nodes */
      boolean connected = ListUtils.extractDisjointSets(sorted, joinedPairs).size() == 1;

      var internalLinks = shortLinks.stream()
          .filter(link -> subsetNodes.contains(link.getNodeA()) && subsetNodes.contains(link.getNodeB()))
          .collect(Collectors.toCollection(LinkedHashSet::new));
      return new ClusterSubset(sorted, subsetNodes, connected, internalLinks);
    });
  }

  /**
   * The nodes reachable from the given nodes over the internal links of a subset in their direction of travel, the
   * given nodes included
   *
   * @param starts to start from
   * @param subset whose internal links to follow
   * @return nodes reached
   */
  private static Set<Node> reach(Set<Node> starts, ClusterSubset subset) {
    Predicate<MacroscopicLinkSegment> alongInternalLink = segment ->
        IntersectionConsolidator.IS_ROAD_SEGMENT.test(segment) && subset.internalLinks.contains(segment.getParent());
    return BreadthFirstSearch.<Node, MacroscopicLinkSegment>execute(starts, false, alongInternalLink,
        (previous, segment) -> alongInternalLink.test(segment), (node, segment) -> false).second().keySet();
  }

  /**
   * The coverage of the given entries over a cluster subset, see the class description
   *
   * @param subset to measure over
   * @param entries the segments entering the subset to average over
   * @return coverage, 0 when there are no entries or the cluster has no exits
   */
  private double coverage(ClusterSubset subset, List<ClusterSegment> entries) {
    if (entries.isEmpty() || clusterExits.isEmpty()) {
      return 0;
    }
    /* the segments leaving the subset, by the node they leave from */
    var exitsFrom = new HashMap<Node, List<ClusterSegment>>();
    for (var segment : segments) {
      if (subset.nodes.contains(segment.from) && !subset.nodes.contains(segment.to)) {
        exitsFrom.computeIfAbsent(segment.from, node -> new ArrayList<>(2)).add(segment);
      }
    }
    double total = 0;
    for (var entry : entries) {
      /* the exits the entry reaches over the internal links, other than back onto its own link */
      var reachable = new ArrayList<ClusterSegment>();
      for (var node : reach(Set.of(entry.to), subset)) {
        for (var exit : exitsFrom.getOrDefault(node, List.of())) {
          if (exit.segment.getParent() != entry.segment.getParent()) {
            reachable.add(exit);
          }
        }
      }
      /* each exit of the cluster counts the cosine to the closest direction reached, averaged over those exits */
      double score = 0;
      for (var clusterExit : clusterExits) {
        double best = 0;
        for (var exit : reachable) {
          best = Math.max(best, clusterExit.directionX * exit.directionX + clusterExit.directionY * exit.directionY);
        }
        score += best;
      }
      total += score / clusterExits.size();
    }
    return total / entries.size();
  }

  /**
   * The segments entering the given nodes from outside a set of nodes
   *
   * @param into the nodes they enter
   * @param outside the nodes they must not start at
   * @return the entering segments
   */
  private List<ClusterSegment> entering(Set<Node> into, Set<Node> outside) {
    return segments.stream().filter(segment -> into.contains(segment.to) && !outside.contains(segment.from))
        .collect(Collectors.toList());
  }

  /**
   * The coverage of a cluster subset over every segment entering it
   *
   * @param subset to measure
   * @return coverage
   */
  double coverage(ClusterSubset subset) {
    return coverage(subset, entering(subset.nodes, subset.nodes));
  }

  /**
   * How much better a member's own traffic does in the subset than on its own: the coverage of the segments entering
   * the member from outside the subset, reached over the subset, minus the same reached over the member alone
   *
   * @param member of the subset
   * @param subset to measure in
   * @return the gain, null when no segment enters the member from outside the subset
   */
  private Double improvement(Intersection member, ClusterSubset subset) {
    var entries = entering(IntersectionUtils.collectMemberNodes(List.of(member)), subset.nodes);
    if (entries.isEmpty()) {
      return null;
    }
    return coverage(subset, entries) - coverage(getSubset(List.of(member)), entries);
  }

  /**
   * How much worse the other members' traffic does without this member: the coverage of the segments entering the
   * other members from outside the subset, reached over the whole subset, minus the same reached without the member
   *
   * @param member of the subset
   * @param subset to measure in
   * @return the loss without it, null when no segment enters the others from outside the subset
   */
  private Double provides(Intersection member, ClusterSubset subset) {
    var rest = subset.parsedIntersections.stream().filter(other -> other != member).collect(Collectors.toList());
    var entries = entering(IntersectionUtils.collectMemberNodes(rest), subset.nodes);
    if (entries.isEmpty()) {
      return null;
    }
    return coverage(subset, entries) - coverage(getSubset(rest), entries);
  }

  /**
   * Verify if a member is an exit-only member of the subset: none of the traffic entering it from outside the subset
   * reaches another member over the subset's internal links, while traffic from another member reaches it
   *
   * @param member of the subset
   * @param subset to verify in
   * @return true when an exit-only member, false otherwise
   */
  private boolean isExitOnlyMember(Intersection member, ClusterSubset subset) {
    var ownMemberNodes = IntersectionUtils.collectMemberNodes(List.of(member));
    var otherMemberNodes = IntersectionUtils.collectMemberNodes(
        subset.parsedIntersections.stream().filter(other -> other != member).collect(Collectors.toList()));
    /* not exit-only when its own traffic from outside reaches another member */
    var ownEntryNodes = entering(ownMemberNodes, subset.nodes).stream()
        .map(segment -> segment.to).collect(Collectors.toSet());
    if (!ownEntryNodes.isEmpty() &&
        reach(ownEntryNodes, subset).stream().anyMatch(otherMemberNodes::contains)) {
      return false;
    }
    /* exit-only when traffic from another member reaches it */
    return reach(otherMemberNodes, subset).stream().anyMatch(ownMemberNodes::contains);
  }

  /**
   * Score a cluster subset of two or more: its coverage, and whether every member earns its place. A member earns it
   * when its own traffic gains at least the least gain, when the others lose at least that much without it, or when it
   * is an exit-only member. A member with no traffic entering it nor the others from outside is not judged
   *
   * @param subsetIntersections parsed intersections of the subset
   * @return its score
   */
  private SubsetScore score(List<Intersection> subsetIntersections) {
    return scores.computeIfAbsent(IdAbleUtils.collectSortedIds(subsetIntersections), key -> {
      var subset = getSubset(subsetIntersections);
      double coverage = coverage(subset);
      /* the least a judged member brings: its own gain, the others' loss without it, or the least gain if exit-only */
      Double leastJudged = null;
      for (var member : subset.parsedIntersections) {
        Double justified = null;
        for (var value : new Double[] {improvement(member, subset), provides(member, subset)}) {
          if (value != null) {
            justified = justified == null ? value : Math.max(justified, value);
          }
        }
        if (isExitOnlyMember(member, subset)) {
          justified = justified == null ? settings.getMinimumCoverageGainPerMember() :
              Math.max(justified, settings.getMinimumCoverageGainPerMember());
        }
        if (justified != null) {
          leastJudged = leastJudged == null ? justified : Math.min(leastJudged, justified);
        }
      }
      /* allowed when every judged member brings at least the least gain */
      boolean allowed = leastJudged != null &&
          leastJudged >= settings.getMinimumCoverageGainPerMember() - Precision.EPSILON_9;
      return new SubsetScore(coverage, allowed);
    });
  }

  /**
   * Compare two subdivisions by their subsets, largest first, then by the ids of their parsed intersections, so ties
   * that remain are broken the same way every time
   *
   * @param first subdivision
   * @param second subdivision
   * @return comparison
   */
  private static int compareSubsets(List<ClusterSubset> first, List<ClusterSubset> second) {
    for (int index = 0; index < Math.min(first.size(), second.size()); ++index) {
      var firstKey = IdAbleUtils.collectSortedIds(first.get(index).parsedIntersections);
      var secondKey = IdAbleUtils.collectSortedIds(second.get(index).parsedIntersections);
      for (int position = 0; position < Math.min(firstKey.size(), secondKey.size()); ++position) {
        int comparison = Long.compare(firstKey.get(position), secondKey.get(position));
        if (comparison != 0) {
          return comparison;
        }
      }
      if (firstKey.size() != secondKey.size()) {
        return Integer.compare(firstKey.size(), secondKey.size());
      }
    }
    return Integer.compare(first.size(), second.size());
  }

  /**
   * Choose the subdivision of the cluster. A cluster subset of two or more must be connected and every member must earn
   * its place. Each parsed intersection scores the coverage of its subset, or its own coverage when alone, and the
   * highest total wins. Totals within the tie margin of the highest count as a tie, won by fewer subsets, then by the
   * shorter longest internal link. A cluster of one parsed intersection is its own subset, without scoring
   *
   * @return the cluster subsets of the chosen subdivision, largest first
   */
  List<ClusterSubset> chooseSubdivision() {
    if (parsedIntersections.size() == 1) {
      return List.of(getSubset(parsedIntersections));
    }
    /* each parsed intersection on its own: its coverage and longest internal link */
    var single = new HashMap<Intersection, Double>();
    var longestAlone = new HashMap<Intersection, Double>();
    for (var intersection : parsedIntersections) {
      var alone = getSubset(List.of(intersection));
      single.put(intersection, coverage(alone));
      longestAlone.put(intersection, alone.getLongestInternalLinkMeters());
    }

    /* score every subdivision whose subsets of two or more are connected and have every member earn its place */
    var scored = new ArrayList<ScoredSubdivision>();
    subdivisionLoop:
    for (var subdivision : ListUtils.findAllPartitions(parsedIntersections)) {
      double total = 0;
      double longest = 0;
      for (var subset : subdivision) {
        if (subset.size() == 1) {
          total += single.get(subset.get(0));
          longest = Math.max(longest, longestAlone.get(subset.get(0)));
          continue;
        }
        var clusterSubset = getSubset(subset);
        if (!clusterSubset.connected) {
          continue subdivisionLoop;
        }
        var score = score(subset);
        if (!score.allowed) {
          continue subdivisionLoop;
        }
        total += score.coverage * subset.size();
        longest = Math.max(longest, clusterSubset.getLongestInternalLinkMeters());
      }
      var ordered = subdivision.stream().map(this::getSubset)
          .sorted(Comparator.<ClusterSubset>comparingInt(subset -> -subset.parsedIntersections.size())
              .thenComparingLong(subset -> subset.parsedIntersections.get(0).getId()))
          .collect(Collectors.toList());
      scored.add(new ScoredSubdivision(ordered, total, longest));
    }

    /* the highest total wins; a tie within the margin goes to fewer subsets, then the shorter longest internal link */
    double highest = scored.stream().mapToDouble(subdivision -> subdivision.total).max().orElseThrow();
    var best = scored.stream()
        .filter(subdivision -> subdivision.total >= highest - settings.getCoverageTieMargin() - Precision.EPSILON_9)
        .min(Comparator.<ScoredSubdivision>comparingInt(subdivision -> subdivision.subsets.size())
            .thenComparingDouble(subdivision -> RoundUtils.simpleDoubleRound(subdivision.longestInternalLinkMeters, 6))
            .thenComparingDouble(subdivision -> -RoundUtils.simpleDoubleRound(subdivision.total, 9))
            .thenComparing((first, second) -> compareSubsets(first.subsets, second.subsets)))
        .orElseThrow();
    return best.subsets;
  }

  /**
   * The parsed intersections of the cluster, by id
   *
   * @return parsed intersections
   */
  List<Intersection> getParsedIntersections() {
    return parsedIntersections;
  }

  /**
   * Verify if the cluster has more parsed intersections than are subdivided, see
   * {@link IntersectionConsolidatorSettings#getMaximumParsedIntersectionsPerIntersectionCluster()}
   *
   * @return true when too large to subdivide, false otherwise
   */
  boolean isTooLargeToSubdivide() {
    return parsedIntersections.size() > settings.getMaximumParsedIntersectionsPerIntersectionCluster();
  }
}
