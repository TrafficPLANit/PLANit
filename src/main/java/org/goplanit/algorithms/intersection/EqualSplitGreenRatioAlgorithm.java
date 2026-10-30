package org.goplanit.algorithms.intersection;

import java.util.List;

import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSegment;

/**
 * Computes green ratios giving every phase of a junction the same share of its usable green time. For example a
 * four-arm junction has two phases, each with (90 - 2 x 5) / 2 = 40 s of a 90 s cycle, a green ratio of 0.44
 *
 * @author markr
 */
public class EqualSplitGreenRatioAlgorithm extends GreenRatioAlgorithmBase {

  /**
   * Constructor
   *
   * @param settings to use
   */
  protected EqualSplitGreenRatioAlgorithm(GreenRatioAndSaturationFlowFactorSettings settings) {
    super(settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected double[] shareUsableGreenTime(
      Intersection junction, List<List<IntersectionSegment>> phases, double usableGreenTimeSeconds) {
    return shareEqually(phases.size(), usableGreenTimeSeconds);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GreenRatioAlgorithmType getType() {
    return GreenRatioAlgorithmType.EQUAL_SPLIT;
  }
}
