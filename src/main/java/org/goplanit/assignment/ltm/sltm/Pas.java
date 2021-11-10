package org.goplanit.assignment.ltm.sltm;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.goplanit.algorithms.shortestpath.ShortestPathResult;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.math.Precision;

/**
 * Paired Alternative Segment (PAS) implementation comprising two subpaths (segments), one of a higher cost than the other. In a PAS both subpaths start at the same vertex and end
 * at the same vertex without any intermediate links overlapping.
 * 
 * @author markr
 *
 */
public class Pas {

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Pas.class.getCanonicalName());

  /** cheap PA segment s1 */
  private EdgeSegment[] s1;

  /** expensive PA segment s2 */
  private EdgeSegment[] s2;

  /** cheap path cost */
  private double s1Cost;

  /** expensive path cost */
  private double s2Cost;

  /** registered origin bushes */
  private final Set<Bush> originBushes;

  /**
   * Remove bushes from this PAS
   * 
   * @param bushes to remove
   */
  private void removeOrigins(List<Bush> originsWithoutRemainingPasFlow) {
    originsWithoutRemainingPasFlow.forEach((bush) -> originBushes.remove(bush));
  }

  /**
   * Execute a flow shift on a given bush for the given PAS segment. This does not move flow through the final merge vertex nor the initial diverge vertex.
   * 
   * @param origin                bush at hand
   * @param flowShiftPcuH         to execute (assumed to be correctly proportioned in relation to other bushes for this PAS
   * @param pasSegment            to update on bush
   * @param flowAcceptanceFactors to use when updating the flows
   * @param potentialBushPruning  when true verify if the flow shift has caused the bush turn sending flows to become non-positive, if so remove (prune) the turn from the bush
   * @return sending flow on last edge segment of the PAS alternative after the flow shift (considering encountered reductions)
   */
  private double executeBushFlowShift(Bush origin, double flowShiftPcuH, EdgeSegment[] pasSegment, double[] flowAcceptanceFactors, boolean potentialBushPruning) {
    int index = 0;
    EdgeSegment currentSegment = null;
    EdgeSegment nextSegment = pasSegment[index];
    while (++index < pasSegment.length) {
      currentSegment = nextSegment;
      nextSegment = pasSegment[index];
      if (potentialBushPruning && !Precision.isPositive(origin.getTurnSendingFlow(currentSegment, nextSegment) - flowShiftPcuH)) {
        /* no remaining flow at all after flow shift, remove turn from bush entirely */
        origin.removeTurn(currentSegment, nextSegment);
      } else {
        origin.addTurnSendingFlow(currentSegment, nextSegment, flowShiftPcuH);
      }
      flowShiftPcuH *= flowAcceptanceFactors[(int) currentSegment.getId()];
    }

    return flowShiftPcuH;
  }

  /**
   * Constructor
   * 
   * @param s1 cheap subpath
   * @param s2 expensive subpath
   */
  private Pas(final EdgeSegment[] s1, final EdgeSegment[] s2) {
    this.s1 = s1;
    this.s2 = s2;
    this.originBushes = new HashSet<Bush>();
  }

  /**
   * update costs of an alternative
   * 
   * @param edgeSegmentCosts to use
   * @param updateS1         Flag indicating to update cost of s1 (cheap) segment, when false update the s2 (costlier) segment
   */
  protected void updateCost(final double[] edgeSegmentCosts, boolean updateS1) {

    EdgeSegment[] alternative = updateS1 ? s1 : s2;
    double cost = 0;
    for (int index = 0; index < alternative.length; ++index) {
      cost += edgeSegmentCosts[(int) alternative[index].getId()];
    }

    if (updateS1) {
      s1Cost = cost;
    } else {
      s2Cost = cost;
    }
  }

  /**
   * Shift flows for this PAS given the currently known costs and smoothing procedure to apply
   * 
   * @param networkS2FlowPcuH     total flow currently using the high cost alternative
   * @param flowShiftPcuH         amount to shift from high cost to low cost segment
   * @param flowAcceptanceFactors to use
   * @return true when flow shifted, false otherwise
   */
  protected boolean executeFlowShift(double networkS2FlowPcuH, double flowShiftPcuH, final double[] flowAcceptanceFactors) {

    List<Bush> originsWithoutRemainingPasFlow = new ArrayList<Bush>();
    for (Bush origin : originBushes) {

      double bushS2Flow = 0;
      double[] bushEntryTurnFlows = null;
      double totalBushEntryTurnFlows = 0;
      int numberOfUsedEntrySegments = 0;

      bushS2Flow = origin.computeSubPathSendingFlow(getDivergeVertex(), getMergeVertex(), s2);
      if (getDivergeVertex().hasEntryEdgeSegments()) {
        /*
         * for each incoming edgeSegment into the diverge we must obtain its portion of flow contributing to the total subpath flow on the high cost segment. This we then use to
         * determine how to spread the flow shift across the turns into the PAS segment for this bush
         */

        /* total turn accepted flow into PAS s2 from bush */
        bushEntryTurnFlows = new double[getDivergeVertex().getEntryEdgeSegments().size()];
        int index = 0;
        for (EdgeSegment entrySegment : getDivergeVertex().getEntryEdgeSegments()) {
          if (origin.containsEdgeSegment(entrySegment)) {
            double turnSendingFlow = origin.getTurnSendingFlow(entrySegment, getFirstEdgeSegment(false /* high cost */));
            double turnAcceptedFlow = turnSendingFlow * flowAcceptanceFactors[(int) entrySegment.getId()];
            bushEntryTurnFlows[index] = turnAcceptedFlow;
            totalBushEntryTurnFlows += turnAcceptedFlow;
            ++numberOfUsedEntrySegments;
          }
          ++index;
        }
        /* scale to portion attributed to PAS s2 */
        double pasPortionScalingFactor = bushS2Flow / totalBushEntryTurnFlows;
        for (index = 0; index < bushEntryTurnFlows.length; ++index) {
          bushEntryTurnFlows[index] *= pasPortionScalingFactor;
        }
      }

      /* Bush flow portion */
      boolean potentialBushPruning = false;
      double bushPortion = Precision.isPositive(networkS2FlowPcuH) ? Math.min(bushS2Flow / networkS2FlowPcuH, 1) : 1;
      double bushFlowShift = flowShiftPcuH * bushPortion;
      if (Precision.isGreaterEqual(bushFlowShift, bushS2Flow)) {
        /* remove this origin from the PAS when done as no flow remains on high cost segment */
        originsWithoutRemainingPasFlow.add(origin);
        /* remove what we can */
        bushFlowShift = bushS2Flow;
        /* possibility that bush along path s2 has no more flow */
        potentialBushPruning = true;
      }

      if (numberOfUsedEntrySegments >= 1) {
        /*
         * multiple entry segments lead into the PAS segment we are moving flow from --> we must update turn flows/splitting rates of turns passing through initial diverge vertex
         * before updating s1/s2
         */
        int index = 0;
        EdgeSegment firstS2EdgeSegment = getFirstEdgeSegment(false /* high cost segment */);
        EdgeSegment firstS1EdgeSegment = getFirstEdgeSegment(true /* low cost segment */);
        for (EdgeSegment entrySegment : getDivergeVertex().getEntryEdgeSegments()) {
          double entryEdgeSegmentFlowPcuH = bushEntryTurnFlows[index++];
          if (potentialBushPruning || Precision.isPositive(entryEdgeSegmentFlowPcuH)) {
            double entryPortion = entryEdgeSegmentFlowPcuH / totalBushEntryTurnFlows;
            /* convert back to sending flow as alpha<1 increases sending flow on entry segment compared to the sending flow component on the first s2 segment */
            double bushEntrySegmentFlowShift = bushFlowShift * entryPortion * (1 / flowAcceptanceFactors[(int) entrySegment.getId()]);

            if (potentialBushPruning && !Precision.isPositive(origin.getTurnSendingFlow(entrySegment, firstS2EdgeSegment) - bushEntrySegmentFlowShift)) {
              /* no remaining flow at all after flow shift, remove turn from bush entirely */
              origin.removeTurn(entrySegment, firstS2EdgeSegment);
            } else {
              origin.addTurnSendingFlow(entrySegment, firstS2EdgeSegment, -bushEntrySegmentFlowShift);
            }
            origin.addTurnSendingFlow(entrySegment, firstS1EdgeSegment, bushEntrySegmentFlowShift);
          }
        }
      }

      /* now update s2/s1 with the flow shift */
      {
        /* origin-bush high cost segment: -delta flow */
        double s2FinalShiftedFlow = executeBushFlowShift(origin, -bushFlowShift, s2, flowAcceptanceFactors, potentialBushPruning);
        /* origin-bush low cost segment: +delta flow */
        double s1FinalSendingFlow = executeBushFlowShift(origin, bushFlowShift, s1, flowAcceptanceFactors, false);
        EdgeSegment lastS1Segment = getLastEdgeSegment(true /* low cost */);
        EdgeSegment lastS2Segment = getLastEdgeSegment(false /* high cost */);

        /*
         * for the turn sending flow shift through the final merge vertex, we use the splitting rates of the bush S2 segment and transfer them to the s1 segment, i.e. the
         * percentage of flow using each exit segment is applied to the s1 segment
         */
        if (getMergeVertex().hasExitEdgeSegments()) {
          int index = 0;
          double[] splittingRates = origin.getSplittingRates(lastS2Segment);
          for (EdgeSegment exitSegment : getMergeVertex().getExitEdgeSegments()) {
            if (origin.containsEdgeSegment(exitSegment)) {
              double splittingRate = splittingRates[index];

              /* remove flow for s2 */
              double s2FlowShift = s2FinalShiftedFlow * splittingRate;
              if (potentialBushPruning && !Precision.isPositive(origin.getTurnSendingFlow(lastS2Segment, exitSegment) - s2FlowShift)) {
                /* no remaining flow at all after flow shift, remove turn from bush entirely */
                origin.removeTurn(lastS2Segment, exitSegment);
              } else {
                origin.addTurnSendingFlow(lastS2Segment, exitSegment, s2FlowShift);
              }

              /* add flow for s1 */
              origin.addTurnSendingFlow(lastS1Segment, exitSegment, s1FinalSendingFlow * splittingRate);
            }
            ++index;
          }
        }
      }
    }

    /* remove irrelevant bushes */
    removeOrigins(originsWithoutRemainingPasFlow);

    return true;
  }

  /**
   * Create a new PAS (factory method)
   * 
   * @param s1 to use
   * @param s2 to use
   * 
   * @return newly created PAS
   */
  protected static Pas create(final EdgeSegment[] s1, final EdgeSegment[] s2) {
    return new Pas(s1, s2);
  }

  /**
   * Collect the end vertex of the PAS
   * 
   * @return end vertex
   */
  public DirectedVertex getMergeVertex() {
    return s2[s2.length - 1].getDownstreamVertex();
  }

  /**
   * Collect the start vertex of the PAS
   * 
   * @return start vertex
   */
  public DirectedVertex getDivergeVertex() {
    return s2[0].getUpstreamVertex();
  }

  /**
   * Register origin on the PAS
   * 
   * @param origin bush to register
   */
  public void registerOrigin(final Bush origin) {
    originBushes.add(origin);
  }

  /**
   * Verify if PAS (still) has origins registered on it
   * 
   * @return true when origins are present, false otherwise
   */
  public boolean hasOrigins() {
    return !originBushes.isEmpty();
  }

  /**
   * Check if bush is overlapping with one of the alternatives, and if it is how much sending flow this sub-path currently represents
   * 
   * @param bush                             to verify
   * @param lowCost                          when true check with low cost alternative otherwise high cost
   * @param linkSegmentFlowAcceptanceFactors to use to obtain accepted flow along subpath, where the flow at the start of the high cost segment is used as starting demand
   * @return when non-negative the segment is overlapping with the PAS, where the value indicates the accepted flow on this sub-path for the bush (with sendinf flow at start as
   *         base demand)
   */
  public double computeOverlappingAcceptedFlow(Bush bush, boolean lowCost, double[] linkSegmentFlowAcceptanceFactors) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    return bush.computeSubPathAcceptedFlow(getDivergeVertex(), getMergeVertex(), alternative, linkSegmentFlowAcceptanceFactors);
  }

  /**
   * check if shortest path tree is overlapping with one of the alternatives
   * 
   * @param pathMatchForCheapPath to verify
   * @param lowCost               when true check with low cost alternative otherwise high cost
   * @return true when overlapping, false otherwise
   */
  public boolean isOverlappingWith(ShortestPathResult pathMatchForCheapPath, boolean lowCost) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    EdgeSegment currEdgeSegment = null;
    EdgeSegment matchingEdgeSegment = null;
    for (int index = alternative.length - 1; index >= 0; --index) {
      currEdgeSegment = alternative[index];
      matchingEdgeSegment = pathMatchForCheapPath.getIncomingEdgeSegmentForVertex(currEdgeSegment.getDownstreamVertex());
      if (!currEdgeSegment.idEquals(matchingEdgeSegment)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if any of the set link segments is present on the indicated alternative
   * 
   * @param linkSegments where we verify against set link segments
   * @param lowCost      when true check with low cost alternative otherwise high cost
   * @return true when overlapping, false otherwise
   */
  public boolean containsAny(final BitSet linkSegments, boolean lowCost) {
    EdgeSegment[] alternative = lowCost ? s1 : s2;
    EdgeSegment currEdgeSegment = null;
    for (int index = alternative.length - 1; index >= 0; --index) {
      currEdgeSegment = alternative[index];
      if (linkSegments.get((int) currEdgeSegment.getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if any of the set link segments is present on either alternative
   * 
   * @param linkSegments where we verify against set link segments
   * @return true when overlapping, false otherwise
   */
  public boolean containsAny(final BitSet linkSegments) {
    return containsAny(linkSegments, true) || containsAny(linkSegments, false);
  }

  /**
   * update costs of both paths. In case the low cost path is no longer the low cost path, switch it with the high cost path
   * 
   * @param edgeSegmentCosts to use
   * @return true when updated costs caused a switch in what is the high and low cost path
   */
  public boolean updateCost(final double[] edgeSegmentCosts) {
    updateCost(edgeSegmentCosts, true);
    updateCost(edgeSegmentCosts, false);

    if (s1Cost > s2Cost) {
      double tempCost = s1Cost;
      s1Cost = s2Cost;
      s2Cost = tempCost;

      EdgeSegment[] tempSegment = s1;
      s1 = s2;
      s2 = tempSegment;
      return true;
    }
    return false;
  }

  /**
   * Apply consumer to each vertex on one of the cost segments
   * 
   * @param lowCostSegment when true applied to low cost segment, when false the high cost segment
   * @param vertexConsumer to apply
   */
  public void forEachVertex(boolean lowCostSegment, Consumer<DirectedVertex> vertexConsumer) {
    EdgeSegment[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      vertexConsumer.accept(alternative[index].getUpstreamVertex());
    }
    vertexConsumer.accept(alternative[alternative.length - 1].getDownstreamVertex());
  }

  /**
   * Apply consumer to each edgeSegment on one of the cost segments
   * 
   * @param lowCostSegment      when true applied to low cost segment, when false the high cost segment
   * @param edgeSegmentConsumer to apply
   */
  public void forEachEdgeSegment(boolean lowCostSegment, Consumer<EdgeSegment> edgeSegmentConsumer) {
    EdgeSegment[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      edgeSegmentConsumer.accept(alternative[index]);
    }
  }

  /**
   * get cost of high cost alternative segment
   * 
   * @return cost
   */
  public double getAlternativeHighCost() {
    return s2Cost;
  }

  /**
   * get cost of high cost alternative segment
   * 
   * @return cost
   */
  public double getAlternativeLowCost() {
    return s1Cost;
  }

  /**
   * Collect the last edge segment of one of the two segments
   * 
   * @param lowCostSegment when true collect for low cost segment, otherwise the high cost segment
   * @return edge segment
   */
  public EdgeSegment getLastEdgeSegment(boolean lowCostSegment) {
    return lowCostSegment ? s1[s1.length - 1] : s2[s2.length - 1];
  }

  /**
   * Collect the first edge segment of one of the two segments
   * 
   * @param lowCostSegment when true collect for low cost segment, otherwise the high cost segment
   * @return edge segment
   */
  public EdgeSegment getFirstEdgeSegment(boolean lowCostSegment) {
    return lowCostSegment ? s1[0] : s2[0];
  }

  /**
   * Access to the two alternatives that reflect the PAS
   * 
   * @param lowCostSegment when true return s1 (lowCost), otherwise s2 (highCost)
   * @return ordered edge segments representing the alternative
   */
  public EdgeSegment[] getAlternative(boolean lowCostSegment) {
    return lowCostSegment ? s1 : s2;
  }

  /**
   * Returns the difference between the cost of the high cost and the low cost segment. Should always be larger than zero assuming an {@link #updateCost(double[])} has been
   * conducted to ensure the segments are labelled correctly regarding which one is high and which one is low cost
   * 
   * @return s2Cost - s2Cost
   */
  public double getReducedCost() {
    return s2Cost - s1Cost;
  }

  /**
   * Match first link segment of PAS segment to predicate provided
   * 
   * @param lowCostSegment when true apply on s1, otherwise on s2
   * @param predicate      to test
   * @return edge segment that matches, null if none matches
   */
  public EdgeSegment matchFirst(boolean lowCostSegment, Predicate<EdgeSegment> predicate) {
    EdgeSegment[] alternative = getAlternative(lowCostSegment);
    for (int index = 0; index < alternative.length; ++index) {
      if (predicate.test(alternative[index])) {
        return alternative[index];
      }
    }
    return null;
  }

}
