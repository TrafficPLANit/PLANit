package org.goplanit.assignment.ltm.sltm;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.directed.EdgeSegment;

/** Simple data class for grouping data relates to a performed S2 flow shift for a destination
 * bush by entry segment
 */
public class BushEntryShiftedS2FlowData {

  private final double s2Flowshifted;

  private final EdgeSegment entrySegment;

  private final RootedBush<?,?> bush;

  private final double[] s2MergeExitSplittingRates;

  public BushEntryShiftedS2FlowData(
          RootedBush<?,?> bush,
          EdgeSegment entrySegment,
          double s2Flowshifted,
          double[] s2MergeExitSplittingRates){

    this.bush = bush;
    this.entrySegment = entrySegment;
    this.s2Flowshifted = s2Flowshifted;
    this.s2MergeExitSplittingRates = s2MergeExitSplittingRates;
  }

  public double getS2Flowshifted() {
    return s2Flowshifted;
  }

  public EdgeSegment getEntrySegment() {
    return entrySegment;
  }

  public RootedBush<?,?> getBush() {
    return bush;
  }

  public double[] getS2MergeExitSplittingRates() {
    return s2MergeExitSplittingRates;
  }
}
