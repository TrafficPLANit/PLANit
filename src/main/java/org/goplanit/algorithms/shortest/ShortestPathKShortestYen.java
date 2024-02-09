package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Yen's K-shortest path algorithm
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

  /** track previous origin used so we can reuse bootstrap result of the one-to-all */
  private DirectedVertex prevOrigin;

  /** previous destination one-to-all bootstrap result */
  private ShortestPathResult prevOneToAllResult;

  /** k in k-shortest */
  private int numShortestPaths;

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
   * Construct k-shortest paths from source node to sink based on directed LinkSegment edges
   *
   * @param origin origin vertex of source node
   * @param destination destination vertex of sink node
   * @return shortest path result that can be used to extract paths
   */
  @Override
  public ShortestPathResult executeOneToOne(DirectedVertex origin, DirectedVertex destination) {

    /* only recompute bootstrap result if origin changes */
    var oneToAll =  prevOneToAllResult;
    if(!prevOrigin.idEquals(origin)){
      oneToAll = oneToAllBootStrapSearch.executeOneToAll(origin);
    }

    var kShortestRawPaths = new ArrayList<Deque<EdgeSegment>>(numShortestPaths);
    kShortestRawPaths.add(oneToAll.createRawPath(origin, destination));
    for(int k = 1; k<numShortestPaths; ++k){
      // find next shortest path by starting at each node of the previous shortest path and excluding the adjacent links
      // of that node from consideration when they have been used in ANY preceding shortest path. Continue to find
      // paths until k is reached.
      final var kMinus1ShortestPath = kShortestRawPaths.get(k-1);
      final int kMinusOnePathSize = kMinus1ShortestPath.size();

      //todo: OPTIONAL by excluding only the single adjacent link and moving one node at the time the search space becomes very large
      // having minimumDetour filters can assist with this to speed up computation time and exclude paths that are not considered
      // eligible anyway
      var kMinus1ShortestPathIter = kMinus1ShortestPath.iterator();
      for(int forkNodeIndex = 0; forkNodeIndex < kMinusOnePathSize-2; ++forkNodeIndex){
        var currForkSegment = kMinus1ShortestPathIter.next();
        var currForkNode = currForkSegment.getUpstreamVertex();
        int minExitSegmentsRequired = currForkSegment.hasOppositeDirectionSegment() ? 3 : 2;

        //todo: MISSING --> CONTINUE WITH must remove all outgoing links of fork node of previous found shortest paths that
        // share the exact same route path

        if(currForkNode.getNumberOfExitEdgeSegments()<minExitSegmentsRequired){
          // no options to fork at this node, so skip finding alternative path and move to next node
          continue;
        }

        // continue here
        oneToOnePerOriginSearch.executeOneToOne(origin, destination).createRawPath(origin, destination);
      }


    }
    return null;
  }

  /**
   * Construct k-shortest paths from source node to sink based on directed LinkSegment edges
   *
   * @param origin origin vertex of source node
   * @param destination destination vertex of sink node
   * @param bannedSegments to consider
   * @return shortest path result that can be used to extract paths
   */
  @Override
  public ShortestPathResult executeOneToOne(DirectedVertex origin, DirectedVertex destination, Set<? extends EdgeSegment> bannedSegments) {
    return null;
  }

}
