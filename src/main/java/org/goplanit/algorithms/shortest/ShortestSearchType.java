package org.goplanit.algorithms.shortest;

/**
 * Types of shortest path searches
 * 
 * @author markr
 *
 */
public enum ShortestSearchType {
  /** Search from all destinations back to a single origin. */
  ALL_TO_ONE,
  /** Search from a single origin to all reachable destinations. */
  ONE_TO_ALL,
  /** Search between a single origin and a single destination. */
  ONE_TO_ONE;

  /**
   * Verify if the shortest search type is inverted compared to "regular" one-to-x search
   * 
   * @return true when inverted, i.e., all-to-one, false otherwise
   */
  public boolean isInverted() {
    return this == ALL_TO_ONE ? true : false;
  }
}
