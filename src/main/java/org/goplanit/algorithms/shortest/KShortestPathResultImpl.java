package org.goplanit.algorithms.shortest;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.path.DirectedPathFactory;
import org.goplanit.utils.path.SimpleDirectedPath;

import java.util.*;
import java.util.logging.Logger;

/**
 * Class that stores the result of a k-shortest path execution allowing one to extract paths or cost information.
 * 
 * @author markr
 *
 */
public class KShortestPathResultImpl implements KShortestPathResult {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(KShortestPathResult.class.getCanonicalName());

  /** track the current k index (between 0 and K-1 paths */
  private int currentK;

  /** the k-shortest paths in raw form with their associated path costs */
  private final List<Pair<Deque<EdgeSegment>, Double>> kShortestRawPathsWithCost;

  private final DirectedVertex origin;

  private final DirectedVertex destination;

  private boolean validateOriginDestination(DirectedVertex origin, DirectedVertex destination){
    if(!origin.idEquals(this.origin) || !destination.idEquals(this.destination)){
      LOGGER.warning("Origin and/or destination do not match the origin-destination this k-shortest path result is configured for");
      return false;
    }
    return true;
  }

  /**
   * Constructor only to be used by shortest path algorithms
   *
   * @param origin origin the paths are associated with (this is fixed for all paths)
   * @param destination destination the paths are associated with (this is fixed for all paths)
   * @param kShortestRawPathsWithCost   raw paths with cost for each of the k-shortest paths found by the algorithm
   */
  public KShortestPathResultImpl(
          DirectedVertex origin, DirectedVertex destination, List<Pair<Deque<EdgeSegment>, Double>> kShortestRawPathsWithCost) {
    this.origin = origin;
    this.destination = destination;
    this.kShortestRawPathsWithCost = kShortestRawPathsWithCost;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void chooseKShortestPathIndex(int k) {
    if(k >= kShortestRawPathsWithCost.size()){
      LOGGER.warning(String.format(
              "Chosen a k index (%d) for k-shortest path that is larger than the max index given the available shortest paths, truncating to max index of %d", k, kShortestRawPathsWithCost.size()-1));
      k = kShortestRawPathsWithCost.size()-1;
    }
    this.currentK = k;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getCurrentKShortestPathIndex() {
    return currentK;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends SimpleDirectedPath> List<T> createPaths(DirectedPathFactory<T> pathFactory) {
    var kShortestPaths = new ArrayList<T>(kShortestRawPathsWithCost.size());
    for(int currK=0; currK < kShortestRawPathsWithCost.size(); ++currK){
      chooseKShortestPathIndex(currK);
      kShortestPaths.add(createPath(pathFactory, origin, destination));
    }
    return kShortestPaths;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends SimpleDirectedPath> T createPath(DirectedPathFactory<T> pathFactory, DirectedVertex origin, DirectedVertex destination) {
    if(!validateOriginDestination(origin, destination)){
      return null;
    }
    return pathFactory.createNew(kShortestRawPathsWithCost.get(getCurrentKShortestPathIndex()).first());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Deque<EdgeSegment> createRawPath(DirectedVertex origin, DirectedVertex destination) {
    if(!validateOriginDestination(origin, destination)){
      return null;
    }
    return kShortestRawPathsWithCost.get(currentK).first();
  }

  @Override
  public EdgeSegment getNextEdgeSegmentForVertex(Vertex vertex) {
    throw new PlanItRunTimeException("getNextEdgeSegmentForVertex not yet supported for k-shortest path result");
  }

  @Override
  public DirectedVertex getNextVertexForEdgeSegment(EdgeSegment edgeSegment) {
    throw new PlanItRunTimeException("getNextVertexForEdgeSegment not yet supported for k-shortest path result");
  }

  @Override
  public double getCostToReach(Vertex vertex) {
    if(vertex.idEquals(this.destination)){
      return kShortestRawPathsWithCost.get(currentK).second(); // full path cost to reach destination
    }
    throw new PlanItRunTimeException("getCostOf not yet supported for k-shortest path result other than for the destination vertex");
  }

  @Override
  public ShortestSearchType getSearchType() {
    throw new PlanItRunTimeException("getSearchType not yet supported for k-shortest path result");
  }
}
