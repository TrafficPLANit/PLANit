package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.physical.Node;

import java.util.*;
import java.util.logging.Logger;

/**
 * Yen's K-shortest path algorithm + efficiency imprvements to avoid recomputing potential shortest paths when possible
 * (Yen, Jin Y. (Jul 1971). "Finding the k Shortest Loopless Paths in a Network". Management Science. 17 (11): 712–716. doi:10.1287/mnsc.17.11.712)
 *<p>
 * See also https://en.wikipedia.org/wiki/Yen%27s_algorithm#cite_note-yenksp2-2
 * </p>
 * <p>
 * Yen's K-shortest path algorithm does not allow for cycles which makes it suitable for path choice. It is however costly
 * to run, especially without any additional constraints on these paths and using Dijkstra for all intermediate path searches.
 * However, it is easy to understand and a good benchmark implementation for other more sophisticated approaches.
 * </p>
 * 
 * @author markr
 *
 */
public class ShortestPathKShortestYen implements ShortestPathOneToOne{

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ShortestPathKShortestYen.class.getCanonicalName());

  private ShortestPathOneToAll oneToAllBootStrapSearch;

  private ShortestPathOneToOne oneToOnePerOriginSearch;

  /** track previous origin used, so we can reuse bootstrap result of the one-to-all */
  private DirectedVertex prevOrigin;

  /** previous destination one-to-all bootstrap result */
  private ShortestPathResult prevOneToAllResult;

  /** k in k-shortest */
  private int numShortestPaths;

  /**
   * Access individual costs for edge segments to efficiently build up path cosy during k-shortest path
   *
   * @param edgeSegment to get cost for
   * @return cost
   */
  private double getEdgeSegmentCost(final EdgeSegment edgeSegment){
    return ((ShortestPathGeneralised)oneToAllBootStrapSearch).getEdgeSegmentCost(edgeSegment);
  }

  /**
   * Find and mark all exit segments as excluded for all k-2-shortest paths that share the root path of the k-1 shortest
   * path up until the provided forkNode index. These exit segments will be added to the to be provided excludedLinksToPopulate
   *
   * @param excludedLinksToPopulate add excluded link segments to this container
   * @param forkNodeIndex index of node up until it is expected that the rootpath coincides with the most recent k-shortest path in kShortestRawPathsWithCost
   * @param kShortestRawPathsWithCost the so far found k-shortest paths of which the last entry is used as reference to identify rootpaths
   * @param excludedLinksThreshold break condition upon which we terminate search if we reach threshold (inclusive, e.g., when container has x entries and threshold is x we stop)
   */
  private static void excludeExitLinkSegmentsFromCoincidingRootPaths(
          TreeSet<EdgeSegment> excludedLinksToPopulate, int forkNodeIndex, ArrayList<Pair<Deque<EdgeSegment>, Double>> kShortestRawPathsWithCost, int excludedLinksThreshold) {
    var lastestKShortestPath = kShortestRawPathsWithCost.get(kShortestRawPathsWithCost.size()-1).first();
    int numFoundKShortestPaths = kShortestRawPathsWithCost.size();
    for(int p = 0; p < numFoundKShortestPaths-1 ; ++p){
      var pathP = kShortestRawPathsWithCost.get(p).first();
      var pathPIterUpToFork = pathP.iterator();
      EdgeSegment pathPIterCurrSegment = pathPIterUpToFork.next();
      var kMin1IterUpToFork = lastestKShortestPath.iterator();
      int indexUpToForkNode = 0;
      while(indexUpToForkNode < forkNodeIndex){
        if(pathPIterUpToFork.hasNext() && !(kMin1IterUpToFork.next().idEquals(pathPIterCurrSegment))){
          break;
        }
        ++indexUpToForkNode;
        pathPIterCurrSegment = pathPIterUpToFork.next();
      }
      boolean coincidesUpToForkNode = indexUpToForkNode==forkNodeIndex;
      if(coincidesUpToForkNode && pathPIterUpToFork.hasNext()){
        /* exclude the link adjacent (exiting) the fork node on the path for our current potential shortest path  */
        excludedLinksToPopulate.add(pathPIterCurrSegment);
        if(excludedLinksToPopulate.size() >= excludedLinksThreshold){
          break; // pre-emptive check in case we know we can break already (because all potential exist segments are excluded for this fork)
        }
      }
    }
  }

  /**
   * Constructor for an edge cost based Dijkstra algorithm for finding shortest paths.
   *
   * @param oneToAllBootStrapSearch per origin, use this once to construct one-to-all wide shortest paths most efficiently
   * @param oneToOneSearch within each origin use this search to find all other k-1 shortest paths
   * @param numShortestPaths the number of shortest paths to find
   */
  public ShortestPathKShortestYen(
          ShortestPathOneToAll oneToAllBootStrapSearch,
          ShortestPathOneToOne oneToOneSearch,
          int numShortestPaths) {
    this.oneToAllBootStrapSearch = oneToAllBootStrapSearch;
    this.oneToOnePerOriginSearch = oneToOneSearch;
    this.numShortestPaths = numShortestPaths;
  }

  /**
   * Construct k-shortest paths from source node to sink based on directed LinkSegment edges.
   * <p>
   *   find all forknode alternative shortest paths related to the k-1 shortest path in the set by starting at each node
   *   of the previous shortest path and excluding the adjacent links of that node from consideration when they have
   *   been used in ANY preceding shortest path. Continue to find paths until all fork nodes are exhausted, after which
   *   the single shortest alternative found is chosen as the next k-shortest path. It is added to the set and then
   *   we use that alternative to find the next shortest path forking off from that shortest path, repeat until
   *   k-shortest paths are found
   * </p>
   *
   * @param origin origin vertex of source node
   * @param destination destination vertex of sink node
   * @return shortest path result that can be used to extract paths
   */
  @Override
  public KShortestPathResult executeOneToOne(DirectedVertex origin, DirectedVertex destination) {
    return executeOneToOne(origin, destination, null);
  }

  /**
   * Construct k-shortest paths from source node to sink based on directed LinkSegment edges but now with some predefined banned segments
   *
   * @param origin origin vertex of source node
   * @param destination destination vertex of sink node
   * @param bannedSegments to consider
   * @return shortest path result that can be used to extract paths
   */
  @Override
  public KShortestPathResult executeOneToOne(DirectedVertex origin, DirectedVertex destination, Set<? extends EdgeSegment> bannedSegments) {
    /* only recompute bootstrap result if origin changes - one-to-all so it is available for all destinations at once */
    var oneToAll =  prevOneToAllResult;
    if(!origin.idEquals(prevOrigin)){
      oneToAll = oneToAllBootStrapSearch.executeOneToAll(origin);
    }

    /* k-shortest (raw) paths found so far and their associated cost + shortest path as initial entry*/
    var kShortestRawPathsWithCost = new ArrayList<Pair<Deque<EdgeSegment>,Double>>(numShortestPaths);
    kShortestRawPathsWithCost.add(Pair.of(oneToAll.createRawPath(origin, destination), oneToAll.getCostToReach(destination)));
    DirectedVertex kMinus1ForkNode = origin;
    var excludedLinks = new TreeSet<EdgeSegment>();

    /* track potential candidates found when doing a pass along the most recent found k-shortest path by eliminating single
       links on that path from being used to find other paths which may or may not be the next shortest.

       we also keep track of the fork node of each potential path, i.e, location where it branched off from the previous
       shortest path to avoid unnecessary regeneration of already identified potential shortest paths during previous passes
     */
    var potentialShortestPathAlternatives = new TreeMap<Double, List<Pair<DirectedVertex, Deque<EdgeSegment>>>>(Comparator.naturalOrder());
    for(int k = 1; k<numShortestPaths; ++k){

      /* select previous k-shortest path as reference point */
      final var kMinus1ShortestPath = kShortestRawPathsWithCost.get(k-1).first();
      final int kMinus1PathSize = kMinus1ShortestPath.size();

      //todo: OPTIONAL by excluding only the single adjacent link and moving one node at the time the search space becomes very large
      // having minimumDetour filters can assist with this to speed up computation time and exclude paths that are not considered
      // eligible anyway
      var kMinus1ShortestPathIter = kMinus1ShortestPath.iterator();

      /* prep */
      var currPotentialShortestPath = new LinkedList<EdgeSegment>();
      double currPotentialPathCostUpToFork = 0;

      EdgeSegment prevForkSegment = null, currForkSegment = null;
      boolean reachedKMinus1ForkNode = false;
      for(int forkNodeIndex = 0; forkNodeIndex < kMinus1PathSize-1; ++forkNodeIndex){

        /* track cost up to the current chosen fork node so we can reconstruct full alternative path cost once we have it */
        prevForkSegment = currForkSegment;
        currForkSegment = kMinus1ShortestPathIter.next();
        var currForkNode = currForkSegment.getUpstreamVertex();

        if(prevForkSegment != null) {
          currPotentialShortestPath.add(prevForkSegment);
          currPotentialPathCostUpToFork += getEdgeSegmentCost(prevForkSegment);
        }

        /* skip any attempt to find alternative path using costly one-to-one path search until we reached the fork node
           of the k-1 shortest path (in relation to k-2) since up until that fork node, alternatives have already been
           added to the potential shortest paths because those links were part of k-2 shortest path which - when it was being constructed
           already identified all possible options by forking of that root path up to the fork
         */
        // todo: this could be made smarter as we now only track in relation to previous rootpath, but we could identify any coincidence with
        //       any rootpoth for found paths 0-(k-1) of course.
        if(currForkNode.idEquals(kMinus1ForkNode)) {
          reachedKMinus1ForkNode = true;
        }
        if(!reachedKMinus1ForkNode){
          continue;
        }

        /* only consider fork node if it has exit links */
        var numForkNodeExitSegments = currForkNode.getNumberOfExitEdgeSegments();
        int numExcludedForkExitSegments = (prevForkSegment!= null && prevForkSegment.hasOppositeDirectionSegment()) ? 2 : 1;
        if(numForkNodeExitSegments - numExcludedForkExitSegments <= 0){
          continue;
        }

        /****************** EXCLUDE non-eligible exit segments *****************************************************************/
        excludedLinks.clear();
        {
          // banned segments are always excluded
          if(bannedSegments != null){
            excludedLinks.addAll(bannedSegments);
          }
          // 0. do not allow loops or reusing of edges, instead of adding all link sup to here, just add preceding one which has the same effect in practice
          //    yet is computationally more efficient
          if(prevForkSegment != null){
            var prevKMinus1OppositeDirectionLink = prevForkSegment.getOppositeDirectionSegment();
            if(prevKMinus1OppositeDirectionLink != null) {
              excludedLinks.add(prevKMinus1OppositeDirectionLink);
            }
          }
          // 1. the one from the k-1 shortest path exiting the fork node, so this is not an option for the alternative
          excludedLinks.add(currForkSegment);

          // 2. make sure we also exclude all outgoing edges used in previous found shortest paths (other than k-1)
          // that shared the same paths up to the fork node, so we do not keep finding an earlier found alternative
          //todo: this could be simpler if we track how the k-1 relates to k-2 in terms of until what link it overlaps,
          // then we can use this to figure out if we need to add an excluded exit link, rather than traversing the entire path
          // over and over
          int excludedLinksThreshold = excludedLinks.size() + (numForkNodeExitSegments-numExcludedForkExitSegments);
          excludeExitLinkSegmentsFromCoincidingRootPaths(excludedLinks, forkNodeIndex, kShortestRawPathsWithCost, excludedLinksThreshold);
          if(excludedLinksThreshold == excludedLinks.size()){
            // no options remaining to fork at this node, so skip finding alternative path and move to next node
            continue;
          }
        }

        /****************** DO ONE-TO-ONE ALTERNATIVE PATH SEARCH *****************************************************************/
        var oneToOnePerOriginResult = oneToOnePerOriginSearch.executeOneToOne(currForkSegment.getUpstreamVertex(), destination, excludedLinks);
        var oneToOneRawPath = oneToOnePerOriginResult.createRawPath(currForkSegment.getUpstreamVertex(), destination);
        if(oneToOneRawPath == null){
          continue;
        }

        /**************** CONSTRUCT NEW POTENTIAL K_SHORTEST PATH *** *****************************************************************/
        var potentialKShortestPath = new LinkedList<>(currPotentialShortestPath);
        potentialKShortestPath.addAll(oneToOneRawPath);
        /* combine cost up to fork + fork to destination to get full path cost */
        double potentialKShortestPathCost = currPotentialPathCostUpToFork + oneToOnePerOriginResult.getCostToReach(destination);
        if(!potentialShortestPathAlternatives.containsKey(potentialKShortestPathCost)){
          potentialShortestPathAlternatives.put(potentialKShortestPathCost, new ArrayList<>(2));
        }
        potentialShortestPathAlternatives.get(potentialKShortestPathCost).add(Pair.of(currForkNode, potentialKShortestPath));
      }

      /**************** CHOOSE NEXT K_SHORTEST PATH FROM POTENTIAL OPTIONS********************************************************/
      if(potentialShortestPathAlternatives.isEmpty()){
        break;
      }
      /* Select cheapest, remove it from candidates, track fork node used so we can skip some unneeded searches in next round */
      Deque<EdgeSegment> foundKShortestPath = null;
      var foundKShortestPathWithEqualMinCostEntry = potentialShortestPathAlternatives.firstEntry();
      var foundKShortestPathsWithEqualMinCost = foundKShortestPathWithEqualMinCostEntry.getValue();
      foundKShortestPath = foundKShortestPathsWithEqualMinCost.get(0).second();
      kMinus1ForkNode = foundKShortestPathsWithEqualMinCost.get(0).first();

      if(foundKShortestPathsWithEqualMinCost.size()>1){
        foundKShortestPathsWithEqualMinCost.remove(0);
      }else{
        potentialShortestPathAlternatives.remove(foundKShortestPathWithEqualMinCostEntry.getKey());
      }
      kShortestRawPathsWithCost.add(Pair.of(foundKShortestPath, foundKShortestPathWithEqualMinCostEntry.getKey()));
    }
    return new KShortestPathResultImpl(origin, destination, kShortestRawPathsWithCost);
  }

}
