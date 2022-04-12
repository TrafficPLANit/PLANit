package org.goplanit.algorithms.shortest;

/**
 * Types of shortest path searches
 * 
 * @author markr
 *
 */
public enum ShortestSearchType {
  ALL_TO_ONE, ONE_TO_ALL, ONE_TO_ONE;

  /**
   * Verify if the shortest search type is inverted compared to "regular" one-to-x search
   * 
   * @return true when inverted, i.e., all-to-one, false otherwise
   */
  public boolean isInverted() {
    return this == ALL_TO_ONE ? true : false;
  }
}
