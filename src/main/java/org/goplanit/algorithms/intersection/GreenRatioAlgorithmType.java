package org.goplanit.algorithms.intersection;

/**
 * The algorithms that compute the green ratios and saturation flow factors of the approaches of signalised
 * intersections, created by {@link GreenRatioAlgorithmFactory}. They differ only in how a junction's usable green time
 * is shared between its phases
 *
 * @author markr
 */
public enum GreenRatioAlgorithmType {

  /** every phase gets the same share of the usable green time */
  EQUAL_SPLIT,

  /** each phase gets a share of the usable green time weighted by its capacity, the larger of its paired approaches */
  CAPACITY_WEIGHTED;
}
