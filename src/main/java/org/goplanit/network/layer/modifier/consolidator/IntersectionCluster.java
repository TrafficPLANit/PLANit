package org.goplanit.network.layer.modifier.consolidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
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
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.modifier.consolidator.IntersectionConsolidatorSettings;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * An intersection cluster: parsed intersections within the joining reach of each other along the road,
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
    final LinkSegment segment;

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
    ClusterSegment(LinkSegment segment, PlanitJtsCrsUtils crsUtils) {
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

    /** its links of at most the joining reach between two of its nodes. They follow from its nodes, and
     * are kept because every step of a search over the subset checks them */
    final Set<Link> internalLinks;

    /**
     * Constructor
     *
     * @param parsedIntersections of the subset
     * @param nodes of the subset
     * @param connected true when its parsed intersections are connected
     * @param internalLinks of the subset
     */
    ClusterSubset(List<Intersection> parsedIntersections, Set<Node> nodes, boolean connected,
        Set<Link> internalLinks) {
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
      return internalLinks.stream().mapToDouble(Link::getLengthMeters).max().orElse(0);
    }
  }

  /** the score of a cluster subset of two or more */
  private static final class SubsetScore {

    /** coverage of the subset */
    final double coverage;

    /** true when every member earns its place and an approach with a signal stays one */
    final boolean allowed;

    /**
     * Constructor
     *
     * @param coverage of the subset
     * @param allowed true when every member earns its place and an approach with a signal stays one
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

  /** road links of at most the joining reach with both ends at nodes of the cluster */
  private final List<Link> shortLinks;

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

  /** scores worked out so far, by cluster subset, of which there is one for each set of parsed intersections */
  private final Map<ClusterSubset, SubsetScore> scores = new HashMap<>();

  /**
   * Build the cluster subset of the given parsed intersections: its nodes are its member nodes, the nodes on the
   * shortest road connections of at most the joining reach between them that stay on the cluster's nodes and do not
   * pass member nodes of the cluster's other parsed intersections, and the nodes with short links to two or more of
   * its member nodes. Such connections and nodes connect the parsed intersections they join
   *
   * @param subset parsed intersections of the cluster
   * @return the cluster subset
   */
  private ClusterSubset buildSubset(List<Intersection> subset) {
    var sorted = subset.stream().sorted(IdAbleUtils.byId()).collect(Collectors.toList());
    /* the cluster's nodes a connection may pass: all but the member nodes of its other parsed intersections */
    var subsetMemberNodes = IntersectionUtils.collectMemberNodes(sorted);
    var allowed = new HashSet<>(nodes);
    IntersectionUtils.collectMemberNodes(parsedIntersections).stream()
        .filter(node -> !subsetMemberNodes.contains(node)).forEach(allowed::remove);

    /* add the nodes on short road connections between its member nodes, joining the parsed intersections they link */
    var subsetNodes = new LinkedHashSet<>(subsetMemberNodes);
    var joinedPairs = new ArrayList<Pair<Intersection, Intersection>>();
    for (var node : subsetMemberNodes) {
      var connections = MacroscopicNetworkLayerUtils.findShortestConnections(node,
          IntersectionConsolidator.IS_ROAD_LINK, allowed::contains, settings.getMaximumJoiningReachMeters());
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
          .sorted(IdAbleUtils.byId()).collect(Collectors.toList());
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
  }

  /**
   * The cluster subset of the given parsed intersections, built once for each set of parsed intersections, see
   * {@link #buildSubset(List)}
   *
   * @param subset parsed intersections of the cluster
   * @return the cluster subset
   */
  private ClusterSubset getSubset(List<Intersection> subset) {
    return subsets.computeIfAbsent(IdAbleUtils.collectSortedIds(subset), key -> buildSubset(subset));
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
    Predicate<LinkSegment> alongInternalLink = segment ->
        IntersectionConsolidator.IS_ROAD_SEGMENT.test(segment) && subset.internalLinks.contains(segment.getParent());
    return BreadthFirstSearch.<Node, LinkSegment>execute(starts, false, alongInternalLink,
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
   * The coverage of a cluster subset over every segment entering it
   *
   * @param subset to measure
   * @return coverage
   */
  private double coverage(ClusterSubset subset) {
    return coverage(subset, entering(subset.nodes, subset.nodes));
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
   * Score a cluster subset of two or more: its coverage, and whether it is allowed. It is when every member earns its
   * place, and an approach with a signal stays one, see
   * {@link IntersectionConsolidator#wouldLoseEveryApproachWithSignal}. A member earns it when its own traffic gains at
   * least the least gain, when the others lose at least that much without it, or when it is an exit-only member. A
   * member with no traffic entering it nor the others from outside is not judged
   *
   * @param subset to score
   * @return its score
   */
  private SubsetScore computeScore(ClusterSubset subset) {
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
    /* allowed when every judged member brings at least the least gain, and an approach with a signal stays one */
    boolean allowed = leastJudged != null &&
        leastJudged >= settings.getMinimumCoverageGainPerMember() - Precision.EPSILON_9 &&
        !IntersectionConsolidator.wouldLoseEveryApproachWithSignal(subset.parsedIntersections, subset.nodes);
    return new SubsetScore(coverage, allowed);
  }

  /**
   * The score of a cluster subset of two or more, worked out once for each cluster subset, see
   * {@link #computeScore(ClusterSubset)}
   *
   * @param subset to score
   * @return its score
   */
  private SubsetScore score(ClusterSubset subset) {
    return scores.computeIfAbsent(subset, key -> computeScore(subset));
  }

  /**
   * The ids of the parsed intersections of each cluster subset of a subdivision, in the order of its cluster subsets
   *
   * @param subdivision cluster subsets of the parsed intersections
   * @return the ids per cluster subset, each sorted
   */
  private static List<List<Long>> idsOf(List<ClusterSubset> subdivision) {
    return subdivision.stream().map(subset -> IdAbleUtils.collectSortedIds(subset.parsedIntersections))
        .collect(Collectors.toList());
  }

  /**
   * Compare two subdivisions by the ids of their subsets, subset by subset and id by id, so ties that remain are broken
   * the same way every time, see {@link ListUtils#compareEntryByEntry}
   *
   * @param first ids per subset of one subdivision
   * @param second ids per subset of the other
   * @return comparison
   */
  private static int compareIds(List<List<Long>> first, List<List<Long>> second) {
    return ListUtils.compareEntryByEntry(first, second,
        (firstIds, secondIds) -> ListUtils.compareEntryByEntry(firstIds, secondIds, Comparator.naturalOrder()));
  }

  /**
   * A subdivision in its usual order: the cluster subsets largest first, then by the id of their first parsed
   * intersection. A cluster subset holds its parsed intersections by id
   *
   * @param subdivision cluster subsets of the parsed intersections
   * @return the same cluster subsets, ordered
   */
  private static List<ClusterSubset> ordered(List<ClusterSubset> subdivision) {
    return subdivision.stream()
        .sorted(Comparator.<ClusterSubset>comparingInt(subset -> -subset.parsedIntersections.size())
            .thenComparingLong(subset -> subset.parsedIntersections.get(0).getId()))
        .collect(Collectors.toList());
  }

  /**
   * The cluster subsets of a subdivision given as lists of parsed intersections, in the usual order, see
   * {@link #ordered}
   *
   * @param subdivision subsets of the parsed intersections
   * @return its cluster subsets, ordered
   */
  private List<ClusterSubset> toOrderedClusterSubsets(List<? extends List<Intersection>> subdivision) {
    return ordered(subdivision.stream().map(this::getSubset).collect(Collectors.toList()));
  }

  /**
   * Score a subdivision: each parsed intersection scores the coverage of its subset, or its own coverage when alone
   *
   * @param subdivision cluster subsets of the parsed intersections, ordered, see {@link #ordered}
   * @param single each parsed intersection's coverage on its own
   * @param isAllowed true when a subset of two or more may be chosen, see {@link #chooseSubdivision}
   * @return the scored subdivision, null when a subset of two or more is not connected or not allowed
   */
  private ScoredSubdivision scoreSubdivision(List<ClusterSubset> subdivision, Map<Intersection, Double> single,
      Predicate<ClusterSubset> isAllowed) {
    double total = 0;
    double longest = 0;
    for (var subset : subdivision) {
      if (subset.parsedIntersections.size() == 1) {
        total += single.get(subset.parsedIntersections.get(0));
      } else if (!subset.connected || !isAllowed.test(subset)) {
        return null;
      } else {
        total += score(subset).coverage * subset.parsedIntersections.size();
      }
      longest = Math.max(longest, subset.getLongestInternalLinkMeters());
    }
    return new ScoredSubdivision(subdivision, total, longest);
  }

  /**
   * The total a subdivision scores while it is searched step by step, see {@link #searchSubdivisionsStepByStep}. A
   * subset of two or more that is not connected, or not allowed, counts as its members alone, so the search can pass
   * through it on the way to one that keeps the rules
   *
   * @param subdivision cluster subsets of the parsed intersections
   * @param single each parsed intersection's coverage on its own
   * @param isAllowed true when a subset of two or more may be chosen, see {@link #chooseSubdivision}
   * @return the total while searching
   */
  private double searchTotal(List<ClusterSubset> subdivision, Map<Intersection, Double> single,
      Predicate<ClusterSubset> isAllowed) {
    double total = 0;
    for (var subset : subdivision) {
      if (subset.parsedIntersections.size() > 1 && subset.connected && isAllowed.test(subset)) {
        total += score(subset).coverage * subset.parsedIntersections.size();
      } else {
        total += subset.parsedIntersections.stream().mapToDouble(single::get).sum();
      }
    }
    return total;
  }

  /**
   * The subdivision made by moving one parsed intersection from one subset to another, or out into a subset of its own
   *
   * @param subdivision cluster subsets of the parsed intersections
   * @param from index of the cluster subset it leaves
   * @param member the parsed intersection to move
   * @param to index of the cluster subset it joins, or -1 for a cluster subset of its own
   * @return the new subdivision, ordered
   */
  private List<ClusterSubset> move(List<ClusterSubset> subdivision, int from, Intersection member, int to) {
    var moved = new ArrayList<List<Intersection>>();
    for (int index = 0; index < subdivision.size(); ++index) {
      var subset = new ArrayList<>(subdivision.get(index).parsedIntersections);
      if (index == from) {
        subset.remove(member);
      }
      if (index == to) {
        subset.add(member);
      }
      if (!subset.isEmpty()) {
        moved.add(subset);
      }
    }
    if (to < 0) {
      moved.add(List.of(member));
    }
    return toOrderedClusterSubsets(moved);
  }

  /**
   * Find subdivisions of a cluster too large to try every one, step by step. The search starts from the whole cluster
   * as one subset. Each step moves one parsed intersection of each subdivision kept to another subset, or out into a
   * subset of its own, and keeps the best of the new subdivisions by their total while searching, see
   * {@link #searchTotal}; how many is a setting, see
   * {@link IntersectionConsolidatorSettings#getSubdivisionsKeptPerSearchStep()}. Moving members this way can reach
   * every subdivision. It does not try them all, so it can miss the best one; keeping more than one per step makes
   * that less likely, since a move that looks worse at first can still lead on to a better subdivision. The search
   * ends after as many steps as the cluster has parsed intersections, or once a step finds no subdivision it has not
   * seen before
   *
   * @param single each parsed intersection's coverage on its own
   * @param isAllowed true when a subset of two or more may be chosen, see {@link #chooseSubdivision}
   * @return every subdivision the search came across, each parsed intersection on its own among them
   */
  private List<List<ClusterSubset>> searchSubdivisionsStepByStep(Map<Intersection, Double> single,
      Predicate<ClusterSubset> isAllowed) {
    /* the whole cluster as one subset to start from, and each parsed intersection on its own, which keeps the rules */
    var seen = new HashSet<List<List<Long>>>();
    var found = new ArrayList<List<ClusterSubset>>();
    var whole = toOrderedClusterSubsets(List.of(parsedIntersections));
    var alone = toOrderedClusterSubsets(parsedIntersections.stream().map(List::of).collect(Collectors.toList()));
    for (var subdivision : List.of(whole, alone)) {
      if (seen.add(idsOf(subdivision))) {
        found.add(subdivision);
      }
    }

    /* each step: every move of one member of each subdivision kept, each subdivision once, then keep the best few */
    List<List<ClusterSubset>> kept = List.of(whole);
    for (int step = 0; step < parsedIntersections.size() && !kept.isEmpty(); ++step) {
      var next = new ArrayList<List<ClusterSubset>>();
      for (var subdivision : kept) {
        for (int from = 0; from < subdivision.size(); ++from) {
          for (var member : subdivision.get(from).parsedIntersections) {
            for (int to = -1; to < subdivision.size(); ++to) {
              if (to == from || (to < 0 && subdivision.get(from).parsedIntersections.size() == 1)) {
                continue;
              }
              var moved = move(subdivision, from, member, to);
              if (seen.add(idsOf(moved))) {
                next.add(moved);
              }
            }
          }
        }
      }
      found.addAll(next);
      var totals = new HashMap<List<ClusterSubset>, Double>();
      next.forEach(subdivision ->
          totals.put(subdivision, RoundUtils.simpleDoubleRound(searchTotal(subdivision, single, isAllowed), 9)));
      next.sort(Comparator.<List<ClusterSubset>>comparingDouble(subdivision -> -totals.get(subdivision))
          .thenComparing((first, second) -> compareIds(idsOf(first), idsOf(second))));
      kept = next.subList(0, Math.min(settings.getSubdivisionsKeptPerSearchStep(), next.size()));
    }
    return found;
  }

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
        .sorted(IdAbleUtils.byId()).collect(Collectors.toList());
    this.parsedIntersectionOfMemberNode = parsedIntersectionOfMemberNode;
    this.nodes = nodes;
    this.settings = settings;
    var memberNodes = IntersectionUtils.collectMemberNodes(this.parsedIntersections);

    /* the road links touching the cluster's nodes, and the short ones among them with both ends in the cluster */
    var linksTouching = new LinkedHashSet<Link>();
    for (var node : nodes) {
      for (var edge : node.getEdges()) {
        var link = (Link) edge;
        if (IntersectionConsolidator.IS_ROAD_LINK.test(link)) {
          linksTouching.add(link);
        }
      }
    }
    this.shortLinks = linksTouching.stream().filter(link -> nodes.contains(link.getNodeA()) &&
        nodes.contains(link.getNodeB()) &&
        link.getLengthMeters() <= settings.getMaximumJoiningReachMeters())
        .collect(Collectors.toList());
    /* their road segments with their directions, and those leaving the cluster as its exits */
    for (var link : linksTouching) {
      for (var segment : new LinkSegment[] {link.getLinkSegmentAb(), link.getLinkSegmentBa()}) {
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
   * Choose the subdivision of the cluster. A cluster subset of two or more must be connected, every member must earn
   * its place, an approach with a signal must stay one, and joining it must not be refused, as for nodes the settings
   * keep in separate intersections. Each parsed intersection scores the coverage of its subset, or its own coverage
   * when alone, and the highest total wins. Totals within the tie margin of the highest count as a tie, won by fewer
   * subsets, then by the shorter longest internal link. Every subdivision is tried, or, for a cluster too large to try
   * every one, those a step by step search comes across, see {@link #searchSubdivisionsStepByStep}. A cluster of one
   * parsed intersection is its own subset, without scoring
   *
   * @param isRefuseJoining true when joining the given intersections, with the given further nodes, into one is
   *                        refused, see {@link IntersectionConsolidatorTrackingData#isRefuseJoining}
   * @return the cluster subsets of the chosen subdivision, largest first
   */
  List<ClusterSubset> chooseSubdivision(
      BiPredicate<Collection<? extends Intersection>, Collection<Node>> isRefuseJoining) {
    if (parsedIntersections.size() == 1) {
      return List.of(getSubset(parsedIntersections));
    }
    /* each parsed intersection's coverage on its own */
    var single = new HashMap<Intersection, Double>();
    for (var intersection : parsedIntersections) {
      single.put(intersection, coverage(getSubset(List.of(intersection))));
    }

    /* a subset of two or more may be chosen when its score allows it and joining it is not refused */
    Predicate<ClusterSubset> isAllowed = subset -> score(subset).allowed &&
        !isRefuseJoining.test(subset.parsedIntersections, subset.nodes);

    /* the subdivisions to choose from: every one, or those found step by step in a cluster too large for that */
    var subdivisions = isTooLargeToTryEverySubdivision() ? searchSubdivisionsStepByStep(single, isAllowed) :
        ListUtils.findAllPartitions(parsedIntersections).stream().map(this::toOrderedClusterSubsets)
            .collect(Collectors.toList());

    /* score those whose subsets of two or more are connected and allowed */
    var scored = subdivisions.stream().map(subdivision -> scoreSubdivision(subdivision, single, isAllowed))
        .filter(Objects::nonNull).collect(Collectors.toList());

    /* the highest total wins; a tie within the margin goes to fewer subsets, then the shorter longest internal link */
    double highest = scored.stream().mapToDouble(subdivision -> subdivision.total).max().orElseThrow();
    var best = scored.stream()
        .filter(subdivision -> subdivision.total >= highest - settings.getCoverageTieMargin() - Precision.EPSILON_9)
        .min(Comparator.<ScoredSubdivision>comparingInt(subdivision -> subdivision.subsets.size())
            .thenComparingDouble(subdivision -> RoundUtils.simpleDoubleRound(subdivision.longestInternalLinkMeters, 6))
            .thenComparingDouble(subdivision -> -RoundUtils.simpleDoubleRound(subdivision.total, 9))
            .thenComparing((first, second) -> compareIds(idsOf(first.subsets), idsOf(second.subsets))))
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
   * Verify if the cluster has more parsed intersections than every subdivision is tried for, see
   * {@link IntersectionConsolidatorSettings#getMaximumParsedIntersectionsToTryEverySubdivision()}. Such a cluster is
   * subdivided step by step instead
   *
   * @return true when too large to try every subdivision, false otherwise
   */
  boolean isTooLargeToTryEverySubdivision() {
    return parsedIntersections.size() > settings.getMaximumParsedIntersectionsToTryEverySubdivision();
  }

  /**
   * The cluster subset of every parsed intersection of the cluster, see {@link #getSubset}: its nodes are their member
   * nodes and the nodes on the shortest road connections between them
   *
   * @return the cluster subset
   */
  public ClusterSubset getSubsetOfEveryParsedIntersection() {
    return getSubset(parsedIntersections);
  }
}
