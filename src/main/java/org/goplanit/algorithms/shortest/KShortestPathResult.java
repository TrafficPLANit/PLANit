package org.goplanit.algorithms.shortest;

import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.path.DirectedPathFactory;
import org.goplanit.utils.path.SimpleDirectedPath;

import java.util.List;

/**
 * Base interface that defines how to access results of a shortest X execution allowing one to extract information
 * 
 * @author markr
 *
 */
public interface KShortestPathResult extends ShortestPathResult {

  /**
   * Switch to the k of the k shortest paths generated, such that when you
   * extract a shortest path, this is the path that is being provided
   *
   * @param k index to set
   */
  public abstract void chooseKShortestPathIndex(int k);

  /**
   * Retrieve the currently chosen k-shortest path that we're retrieving
   *
   * @return k that is set
   */
  public abstract int getCurrentKShortestPathIndex();

  /**
   * Convenience method to construct all k-shortest paths at once for the last performed search
   *
   * @param pathFactory to use to create paths
   * @return created paths
   * @param <T> type of path
   */
  public abstract <T extends SimpleDirectedPath> List<T> createPaths(final DirectedPathFactory<T> pathFactory);

}
