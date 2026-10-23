package org.goplanit.algorithms.intersection;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSegment;

/**
 * Computes green ratios giving each phase of a junction a share of its usable green time weighted by its capacity. The
 * capacity of a phase of two paired approaches is the larger of the two, the one that needs the green. For example a
 * main road of 3,600 pcu/h each way crossing a side road of 1,800 pcu/h each way gets 80 x 3,600 / 5,400 = 53.3 s of a
 * 90 s cycle, a green ratio of 0.59, and the side road 26.7 s, 0.30. Without capacity on any phase, the phases share it
 * equally, with a warning
 *
 * @author markr
 */
public class CapacityWeightedGreenRatioAlgorithm extends GreenRatioAlgorithmBase {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(CapacityWeightedGreenRatioAlgorithm.class.getCanonicalName());

  /**
   * Constructor
   *
   * @param settings to use
   */
  protected CapacityWeightedGreenRatioAlgorithm(GreenRatioAndSaturationFlowFactorSettings settings) {
    super(settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected double[] shareUsableGreenTime(
      Intersection junction, List<List<IntersectionSegment>> phases, double usableGreenTimeSeconds) {
    var capacities = phases.stream().mapToDouble(phase -> phase.stream().mapToDouble(
        approach -> approach.getLinkSegment().getCapacityOrDefaultPcuH()).max().orElse(0)).toArray();
    double totalCapacity = Arrays.stream(capacities).sum();
    if (!(totalCapacity > 0)) {
      LOGGER.warning(String.format("No capacity on any phase of intersection %s, its usable green time is shared " +
          "equally", junction.getIdsAsString()));
      return shareEqually(phases.size(), usableGreenTimeSeconds);
    }
    var greenTimesSeconds = new double[capacities.length];
    for (int phase = 0; phase < capacities.length; ++phase) {
      greenTimesSeconds[phase] = usableGreenTimeSeconds * capacities[phase] / totalCapacity;
    }
    return greenTimesSeconds;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GreenRatioAlgorithmType getType() {
    return GreenRatioAlgorithmType.CAPACITY_WEIGHTED;
  }
}
