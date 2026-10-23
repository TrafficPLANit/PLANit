package org.goplanit.algorithms.intersection;

import java.util.Collection;
import java.util.List;

import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;

/**
 * An algorithm computing the green ratios and saturation flow factors of the approaches of signalised intersections,
 * where no signal timing is known. It changes only values that are not set or that it computed before, never a value
 * set directly, an internal segment or a movement. Create one with {@link GreenRatioAlgorithmFactory}.
 * <p>
 * Run it on intersections that each cover a whole junction, for example once they are consolidated. A junction split
 * over several intersections is timed as several junctions: one with fewer approaches gets fewer phases, and so more
 * green than the junction has, and a carriageway between two of its nodes counts as an approach, so traffic through
 * the junction meets two reduced approaches instead of one.
 * </p>
 *
 * @author markr
 */
public interface GreenRatioAlgorithm {

  /**
   * The type of this algorithm
   *
   * @return its type
   */
  public abstract GreenRatioAlgorithmType getType();

  /**
   * Compute the green ratios and saturation flow factors of the approaches of the given intersections, those that are
   * signalised, and remove the values computed earlier that no longer apply
   *
   * @param intersections to compute them for, for example all intersections of a layer
   * @param crsUtils for the coordinate reference system of the nodes and link geometries, to measure directions in
   * @return what was done
   */
  public abstract GreenRatioAndSaturationFlowFactorResult computeGreenRatiosAndSaturationFlowFactors(
      Collection<? extends Intersection> intersections, PlanitJtsCrsUtils crsUtils);

  /**
   * Compute the green ratios and saturation flow factors of the approaches of one intersection, see
   * {@link #computeGreenRatiosAndSaturationFlowFactors(Collection, PlanitJtsCrsUtils)}
   *
   * @param intersection to compute them for
   * @param crsUtils for the coordinate reference system of the nodes and link geometries, to measure directions in
   * @return what was done
   */
  public default GreenRatioAndSaturationFlowFactorResult computeGreenRatiosAndSaturationFlowFactors(
      Intersection intersection, PlanitJtsCrsUtils crsUtils) {
    return computeGreenRatiosAndSaturationFlowFactors(List.of(intersection), crsUtils);
  }
}
