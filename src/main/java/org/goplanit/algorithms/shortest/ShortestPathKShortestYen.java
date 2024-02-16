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
 * <p/>
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
   * Constructor for an edge cost based Dijkstra algorithm for finding shortest paths.
   *
   * @param oneToAllBootStrapSearch per origin, use this once to construct one-to-all wide shortest paths most efficiently
   * @param oneToOneSearch within each origin use this search to find all other k-1 shortest paths
   */
  public ShortestPathKShortestYen(
          ShortestPathOneToAll oneToAllBootStrapSearch, ShortestPathOneToOne oneToOneSearch,
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
    /* only recompute bootstrap result if origin changes */
    var oneToAll =  prevOneToAllResult;
    if(!origin.idEquals(prevOrigin)){
      oneToAll = oneToAllBootStrapSearch.executeOneToAll(origin);
    }

    // track k-shortest paths found and their associated cost
    var kShortestRawPathsWithCost = new ArrayList<Pair<Deque<EdgeSegment>,Double>>(numShortestPaths);
    // track potential candidates for a particular k-shortest path by their cost (when multiple entries with the same cost exist track all options)
    // we also keep track of the fork node, i.e, location where it branched off from the previous shortest path to avoid unnecessary regeneration
    // of paths
    var potentialShortestPathAlternatives = new TreeMap<Double, List<Pair<DirectedVertex, Deque<EdgeSegment>>>>(Comparator.naturalOrder());

    var upstreamForkNodeIndex = new ArrayList<Integer>(numShortestPaths);
    kShortestRawPathsWithCost.add(Pair.of(oneToAll.createRawPath(origin, destination), oneToAll.getCostToReach(destination)));
    DirectedVertex kMinus1ForkNode = origin;
    for(int k = 1; k<numShortestPaths; ++k){

      // find all fork nodes for the current k-1 shortest path as a starting point for finding the next possible alternative
      // shortest path
      final var kMinus1ShortestPath = kShortestRawPathsWithCost.get(k-1).first();
      final int kMinus1PathSize = kMinus1ShortestPath.size();

      //todo: OPTIONAL by excluding only the single adjacent link and moving one node at the time the search space becomes very large
      // having minimumDetour filters can assist with this to speed up computation time and exclude paths that are not considered
      // eligible anyway
      var kMinus1ShortestPathIter = kMinus1ShortestPath.iterator();

      var altShortestPath = new LinkedList<EdgeSegment>();
      double currAltPathCost = 0;

      var excludedLinks = new TreeSet<EdgeSegment>();

      EdgeSegment prevForkSegment = null;
      EdgeSegment currForkSegment = null;
      boolean reachedKMinus1ForkNode = false;
      for(int forkNodeIndex = 0; forkNodeIndex < kMinus1PathSize-1; ++forkNodeIndex){
        prevForkSegment = currForkSegment;
        currForkSegment = kMinus1ShortestPathIter.next();
        var currForkNode = currForkSegment.getUpstreamVertex();

        if(prevForkSegment != null) {
          altShortestPath.add(prevForkSegment);
          currAltPathCost += getEdgeSegmentCost(prevForkSegment);
        }

        // skip finding alternative paths until we reached the fork node of the k-1 shortest path (in relation to k-2),
        // since up until that fork node, alternatives have already been added because those links were part of k-2 which has
        // already been exhausted for options present in the potential pool of paths
        // todo: this could be made smarter as we now only track in relation to previous rootpath, but we could identify any coincidence with
        //       any rootpoth for found paths 0-(k-1) of course.
        if(currForkNode.idEquals(kMinus1ForkNode)) {
          reachedKMinus1ForkNode = true;
        }
        if(!reachedKMinus1ForkNode){
          continue;
        }

        var numForkNodeExitSegments = currForkNode.getNumberOfExitEdgeSegments();
        int numExcludedForkExitSegments = (prevForkSegment!= null && prevForkSegment.hasOppositeDirectionSegment()) ? 2 : 1;
        if(numForkNodeExitSegments - numExcludedForkExitSegments <= 0){
          // no options to fork at this node, so skip finding alternative path and move to next node
          continue;
        }

        /****************** EXCLUDE non-eligible exit segments *****************************************************************/
        // excluding already used exit edges of forknode  from previously found shortest paths that coincided up to the fork node
        excludedLinks.clear();
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
        for(int p = 0; p < k-1 ; ++p){
          var pathP = kShortestRawPathsWithCost.get(p).first();
          var pathPIterUpToFork = pathP.iterator();
          EdgeSegment pathPIterCurrSegment = pathPIterUpToFork.next();
          var kMin1IterUpToFork = kMinus1ShortestPath.iterator();
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
            excludedLinks.add(pathPIterCurrSegment);
            ++numExcludedForkExitSegments;
            if(numForkNodeExitSegments - numExcludedForkExitSegments <= 0){
              break; // pre-emptive check in case we know we can break already because all potential exist segments are excluded for this fork
            }
          }
        }

        if(numForkNodeExitSegments - numExcludedForkExitSegments <= 0){
          // no options remaining to fork at this node, so skip finding alternative path and move to next node
          continue;
        }

        /****************** DO ONE-TO-ONE ALTERNATIVE PATH SEARCH *****************************************************************/
        var oneToOnePerOriginResult = oneToOnePerOriginSearch.executeOneToOne(currForkSegment.getUpstreamVertex(), destination, excludedLinks);
        var oneToOneRawPath = oneToOnePerOriginResult.createRawPath(currForkSegment.getUpstreamVertex(), destination);
        if(oneToOneRawPath == null){
          continue;
        }

        /**************** CONSTRUCT POTENTIAL K_SHORTEST PATH *** *****************************************************************/
        var potentialKShortestPath = new LinkedList<>(altShortestPath);
        potentialKShortestPath.addAll(oneToOneRawPath);
        double potentialKShortestPathCost = currAltPathCost + oneToOnePerOriginResult.getCostToReach(destination); // cost from fork to destination
        if(!potentialShortestPathAlternatives.containsKey(potentialKShortestPathCost)){
          potentialShortestPathAlternatives.put(potentialKShortestPathCost, new ArrayList<>(2));
        }
        potentialShortestPathAlternatives.get(potentialKShortestPathCost).add(Pair.of(currForkNode, potentialKShortestPath));
      }

      /**************** CHOOSE NEXT K_SHORTEST PATH FROM POTENTIAL OPTIONS********************************************************/
      if(potentialShortestPathAlternatives.isEmpty()){
        break;
      }
      // select and remove from remaining options + update offset cost up to fork node for next iteration
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
