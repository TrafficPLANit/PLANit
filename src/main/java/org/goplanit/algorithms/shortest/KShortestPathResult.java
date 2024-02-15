package org.goplanit.algorithms.shortest;

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
   * @param k indiex to set
   */
  public abstract void chooseKShortestPathIndex(int k);

  /**
   * Retrieve the currently chosen k-shortest path that we're retrieving
   *
   * @return k that is set
   */
  public abstract int getCurrentKShortestPathIndex();

}
