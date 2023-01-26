package org.goplanit.assignment.ltm.sltm;

import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.zoning.Centroid;

/**
 * Track turn based data of a bush.
 * <p>
 * For now we only track turn sending flows to minimise bookkeeping and memory usage, splitting rates are deduced from the turn sending flows when needed.
 * 
 * @author markr
 *
 */
public class BushTurnData {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(BushTurnData.class.getCanonicalName());

  /** track known bush turn sending flows s_ab by the combined key of incoming outgoing link segments */
  private final MultiKeyMap<Object, Double> compositionTurnSendingFlows;

  /**
   * Register sending flow on the container
   * 
   * @param fromSegment     of turn
   * @param toSegment       of turn
   * @param turnSendingFlow flow of turn
   */
  private void registerTurnSendingFlow(EdgeSegment fromSegment, EdgeSegment toSegment, double turnSendingFlow) {
    compositionTurnSendingFlows.put(fromSegment, toSegment, turnSendingFlow);
  }

  /**
   * Constructor
   * 
   */
  BushTurnData() {
    this.compositionTurnSendingFlows = new MultiKeyMap<>();
  }

  /**
   * copy constructor.
   * 
   * @param bushTurnData to copy
   */
  public BushTurnData(BushTurnData bushTurnData) {
    this.compositionTurnSendingFlows = bushTurnData.compositionTurnSendingFlows.clone();
  }

  /**
   * Update the turn sending flow for a given turn
   * 
   * @param fromSegment      of turn
   * @param toSegment        of turn
   * @param turnSendingFlow  to update
   * @param allowTurnRemoval when true we allow for removal of turn/edge segment when no flow remains, when false we keep regardless of the remaining flow
   * @return true when turn has any turn sending flow left after setting flow, false when turn sending flow no longer exists
   */
  public boolean setTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment, double turnSendingFlow, boolean allowTurnRemoval) {

    if (Double.isNaN(turnSendingFlow)) {
      LOGGER.severe("Turn (%s to %s) sending flow is NAN, shouldn't happen - consider identifying issue as turn flow cannot be updated properly, reset to 0.0 flow");
      turnSendingFlow = 0.0;
    } else if (!Precision.positive(turnSendingFlow)) {
      if (allowTurnRemoval) {
        removeTurn(fromSegment, toSegment);
        return false;
      } else if (turnSendingFlow < 0) {
        LOGGER.warning(
            String.format("** Turn (%s to %s) sending flow negative (%.9f), this is not allowed, reset to 0.0 ", fromSegment.getXmlId(), toSegment.getXmlId(), turnSendingFlow));
        turnSendingFlow = 0.0;
        return false;
      }
    }

    registerTurnSendingFlow(fromSegment, toSegment, turnSendingFlow);
    return true;

  }

  /**
   * Add turn sending flow for a given turn (can be negative), turn will not be removed if no flow remains
   * 
   * @param from     of turn
   * @param to       of turn
   * @param flowPcuH to add
   * @return the new turn sending flow after adding the given flow
   */
  public double addTurnSendingFlow(final EdgeSegment from, final EdgeSegment to, double flowPcuH) {
    return addTurnSendingFlow(from, to, flowPcuH, false);
  }

  /**
   * Add turn sending flow for a given turn (can be negative), flow can be removed if desired depending on flags set if desired
   * 
   * @param from             of turn
   * @param to               of turn
   * @param flowPcuH         to add
   * @param allowTurnRemoval when true we allow for removal of turn/edge segment when no flow remains, when false we keep regardless of the remaining flow
   * @return the new labelled turn sending flow after adding the given flow
   */
  public double addTurnSendingFlow(final EdgeSegment from, final EdgeSegment to, double flowPcuH, boolean allowTurnRemoval) {
    Double newSendingFlow = flowPcuH + getTurnSendingFlowPcuH(from, to);
    boolean hasRemainingFlow = setTurnSendingFlow(from, to, newSendingFlow, allowTurnRemoval);
    newSendingFlow = hasRemainingFlow ? newSendingFlow : 0.0;
    return newSendingFlow;
  }

  /**
   * Remove the turn entirely
   * 
   * @param fromEdgeSegment of turn
   * @param toEdgeSegment   of turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    compositionTurnSendingFlows.removeMultiKey(fromEdgeSegment, toEdgeSegment);
  }

  /**
   * Get the turn sending flow for a given turn
   * 
   * @param fromSegment of turn
   * @param toSegment   of turn
   * @return turn sending flow, 0 if not present
   */
  public double getTurnSendingFlowPcuH(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    Double existingSendingFlow = compositionTurnSendingFlows.get(fromSegment, toSegment);
    if (existingSendingFlow != null) {
      return existingSendingFlow;
    } else {
      return 0;
    }
  }

  /**
   * Total sending flows s_a from given segment
   * 
   * @param edgeSegment to use
   * @return sending flow s_a
   */
  public double getTotalSendingFlowFromPcuH(final EdgeSegment edgeSegment) {
    double totalSendingFlow = 0;
    for (var exitSegment : edgeSegment.getDownstreamVertex().getExitEdgeSegments()) {
      double s_ab = getTurnSendingFlowPcuH(edgeSegment, exitSegment);
      totalSendingFlow += s_ab;
    }
    return totalSendingFlow;
  }

  /**
   * Collect the accepted flow towards an edge segment in the bush with the specified label, if not present, zero flow is returned
   * 
   * @param edgeSegment           to collect sending flow towards to
   * @param flowAcceptanceFactors to convert sending flow to accepted flow
   * @return bush sending flow found
   */
  public double getTotalAcceptedFlowToPcuH(final EdgeSegment edgeSegment, double[] flowAcceptanceFactors) {
    if (edgeSegment.getUpstreamVertex() instanceof Centroid) {
      /* no preceding link segments, so same as what is being sent out of the segment */
      return getTotalSendingFlowFromPcuH(edgeSegment);
    }

    double totalAcceptedFlow = 0;
    for (var entrySegment : edgeSegment.getUpstreamVertex().getEntryEdgeSegments()) {
      double s_ab = getTurnSendingFlowPcuH(entrySegment, edgeSegment);
      double v_ab = s_ab * flowAcceptanceFactors[(int) entrySegment.getId()];
      totalAcceptedFlow += v_ab;
    }
    return totalAcceptedFlow;
  }

  /**
   * Verify if the turn sending flow for a given turn is positive
   * 
   * @param fromSegment of turn
   * @param toSegment   of turn
   * @return true when present, false otherwise
   */
  public boolean containsTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    return getTurnSendingFlowPcuH(fromSegment, toSegment) > 0;
  }

  /**
   * Collect the splitting rates for a given link segment and composition. Splitting rates are based on the current (labelled) turn sending flows s_ab. In case no flows are present
   * for the given composition label, zero splitting rates for all turns are returned.
   * 
   * @param fromSegment to collect bush splitting rates for
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the downstream from segment vertex
   */
  public double[] getSplittingRates(final EdgeSegment fromSegment) {
    var exitEdgeSegments = fromSegment.getDownstreamVertex().getExitEdgeSegments();

    /* determining number of edge segment is costly, instead use edges (which is larger or equal) and then copy result */
    double[] splittingRates = new double[fromSegment.getDownstreamVertex().getNumberOfEdges()];

    double totalSendingFlow = 0;
    int index = 0;
    for (var exitSegment : exitEdgeSegments) {
      double s_ab = getTurnSendingFlowPcuH(fromSegment, exitSegment);
      splittingRates[index++] = s_ab;
      totalSendingFlow += s_ab;
    }
    ArrayUtils.divideBy(splittingRates, totalSendingFlow, 0);

    /* truncate */
    Arrays.copyOf(splittingRates, index);
    return splittingRates;
  }

  /**
   * Collect the splitting rate for a given link segment. Splitting rates are based on the current turn sending flows s_ab.
   * <p>
   * When collecting multiple splitting rates with the same in link, do not use this method but instead collect all splitting rates at once and then filter the ones you require it
   * is computationally more efficient.
   * 
   * 
   * @param fromSegment of turn to collect splitting rate for
   * @param toSegment   of turn to collect splitting rate for
   * @return splitting rate, when turn is not present or not used, zero is returned
   */
  public double getSplittingRate(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    double turnSendingFlow = getTurnSendingFlowPcuH(fromSegment, toSegment);
    if (turnSendingFlow > 0) {
      double totalSendingFlow = getTotalSendingFlowFromPcuH(fromSegment);
      if (totalSendingFlow < turnSendingFlow) {
        LOGGER.severe(String.format("Total sending flow (%.10f) smaller than turn (%s,%s) sending flow (%.10f), this shouldn't happen", totalSendingFlow, fromSegment.getXmlId(),
            toSegment.getXmlId(), turnSendingFlow));
      }
      return turnSendingFlow / totalSendingFlow;
    } else {
      return 0;
    }
  }

  /**
   * Verify if any turn flows have been registered
   * 
   * @return true if so, false otherwise
   */
  public boolean hasTurnFlows() {
    return compositionTurnSendingFlows.isEmpty();
  }

}
