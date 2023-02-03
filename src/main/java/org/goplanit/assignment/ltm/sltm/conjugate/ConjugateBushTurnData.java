package org.goplanit.assignment.ltm.sltm.conjugate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.math.Precision;

/**
 * Track conjugate edge segment based data, i.e., turn data of a conjugate bush.
 * <p>
 * For now we only track (turn) sending flows to minimise bookkeeping and memory usage, turn splitting rates are deduced from the turn sending flows when needed.
 * 
 * @author markr
 *
 */
public class ConjugateBushTurnData{

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateBushTurnData.class.getCanonicalName());

  /** track known conjugate bush (turn) sending flows s_ab by the combined key of incoming outgoing link segments */
  private final Map<ConjugateEdgeSegment, Double> turnSendingFlows;

  /**
   * Register turn sending flow on the container
   * 
   * @param turn            the turn
   * @param turnSendingFlow flow of turn
   */
  private void registerTurnSendingFlow(ConjugateEdgeSegment turn, double turnSendingFlow) {
    turnSendingFlows.put(turn, turnSendingFlow);
  }

  /**
   * Constructor
   * 
   */
  ConjugateBushTurnData() {
    this.turnSendingFlows = new HashMap<ConjugateEdgeSegment, Double>();
  }

  /**
   * copy constructor.
   * 
   * @param bushTurnData to copy
   */
  public ConjugateBushTurnData(ConjugateBushTurnData bushTurnData) {
    this.turnSendingFlows = new HashMap<>(bushTurnData.turnSendingFlows);
  }

  /**
   * Update the turn sending flow for a given turn
   * 
   * @param turnSegment      the turn
   * @param turnSendingFlow  to update
   * @param allowTurnRemoval when true we allow for removal of turn/edge segment when no flow remains, when false we keep regardless of the remaining flow
   * @return true when turn has any turn sending flow left after setting flow, false when turn sending flow no longer exists
   */
  public boolean setTurnSendingFlow(final ConjugateEdgeSegment turnSegment, double turnSendingFlow, boolean allowTurnRemoval) {

    if (Double.isNaN(turnSendingFlow)) {
      LOGGER.severe("Turn sending flow is NAN, shouldn't happen - consider identifying issue as turn flow cannot be updated properly, reset to 0.0 flow");
      turnSendingFlow = 0.0;
    } else if (!Precision.positive(turnSendingFlow)) {
      if (allowTurnRemoval) {
        removeTurn(turnSegment);
        return false;
      } else if (turnSendingFlow < 0) {
        var originalEdgeSegments = turnSegment.getOriginalAdjcentEdgeSegments();
        LOGGER.warning(String.format("** Turn (%s to %s) sending flow negative (%.9f), this is not allowed, reset to 0.0 ", originalEdgeSegments.first().getXmlId(),
            originalEdgeSegments.second().getXmlId(), turnSendingFlow));
        turnSendingFlow = 0.0;
        return false;
      }
    }

    registerTurnSendingFlow(turnSegment, turnSendingFlow);
    return true;

  }

  /**
   * Add turn sending flow for a given turn (can be negative), turn will not be removed if no flow remains
   * 
   * @param turnSegment the turn
   * @param flowPcuH    to add
   * @return the new turn sending flow after adding the given flow
   */
  public double addTurnSendingFlow(final ConjugateEdgeSegment turnSegment, double flowPcuH) {
    return addTurnSendingFlow(turnSegment, flowPcuH, false);
  }

  /**
   * Add turn sending flow for a given turn (can be negative), flow can be removed if desired depending on flags set if desired
   * 
   * @param turnSegment      the turn
   * @param flowPcuH         to add
   * @param allowTurnRemoval when true we allow for removal of turn/edge segment when no flow remains, when false we keep regardless of the remaining flow
   * @return the new labelled turn sending flow after adding the given flow
   */
  public double addTurnSendingFlow(final ConjugateEdgeSegment turnSegment, double flowPcuH, boolean allowTurnRemoval) {
    Double newSendingFlow = flowPcuH + getTurnSendingFlowPcuH(turnSegment);
    boolean hasRemainingFlow = setTurnSendingFlow(turnSegment, newSendingFlow, allowTurnRemoval);
    newSendingFlow = hasRemainingFlow ? newSendingFlow : 0.0;
    return newSendingFlow;
  }

  /**
   * Remove the turn entirely
   * 
   * @param turnSegment the turn
   */
  public void removeTurn(final ConjugateEdgeSegment turnSegment) {
    turnSendingFlows.remove(turnSegment);
  }

  /**
   * Get the turn sending flow for a given turn
   * 
   * @param turnSegment the turn
   * @return turn sending flow, 0 if not present
   */
  public double getTurnSendingFlowPcuH(final ConjugateEdgeSegment turnSegment) {
    Double existingSendingFlow = turnSendingFlows.get(turnSegment);
    if (existingSendingFlow != null) {
      return existingSendingFlow;
    } else {
      return 0;
    }
  }

  /**
   * Total sending flows s_a from given original segment collected by means of the conjugate node
   * 
   * @param node conjugate node to use
   * @return sending flow s_a
   */
  public double getTotalSendingFlowFromPcuH(final ConjugateDirectedVertex node) {
    double totalSendingFlow = 0;
    for (var turn : node.getExitEdgeSegments()) {
      double s_ab = getTurnSendingFlowPcuH(turn);
      totalSendingFlow += s_ab;
    }
    return totalSendingFlow;
  }

  /**
   * Collect the accepted flow towards a conjugate node (original edge segment) in the bush, if not present, zero flow is returned
   * 
   * @param node                                        conjugate node to collect accepted flow towards to
   * @param originalNetworkSegmentFlowAcceptanceFactors to convert sending flow to accepted flow (based on original edge segment ids)
   * @return bush sending flow found
   */
  public double getTotalAcceptedFlowToPcuH(final ConjugateDirectedVertex node, double[] originalNetworkSegmentFlowAcceptanceFactors) {
    if (!node.hasEntryEdgeSegments()) {
      /* no preceding conjugate link segments, so no incoming turns, hence it must be a root vertex connected to an origin */
      return getTotalSendingFlowFromPcuH(node);
    }

    double totalAcceptedFlow = 0;
    for (var turn : node.getEntryEdgeSegments()) {
      double s_ab = getTurnSendingFlowPcuH(turn);

      var originalEntrySegment = turn.getOriginalAdjcentEdgeSegments().first();
      double v_ab = s_ab * originalNetworkSegmentFlowAcceptanceFactors[(int) originalEntrySegment.getId()];
      totalAcceptedFlow += v_ab;
    }
    return totalAcceptedFlow;
  }

  /**
   * Verify if the turn sending flow for a given turn is positive
   * 
   * @param turnSegment the turn
   * @return true when present, false otherwise
   */
  public boolean containsTurnSendingFlow(final ConjugateEdgeSegment turnSegment) {
    return getTurnSendingFlowPcuH(turnSegment) > 0;
  }

  /**
   * Collect the splitting rates for a given conjugate node (original link segment). Splitting rates are based on the current turn sending flows s_ab. In case no flows are present
   * zero splitting rates for all turns are returned.
   * 
   * @param conjugateVertex to collect bush splitting rates for
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the conjugate node
   */
  public double[] getSplittingRates(final ConjugateDirectedVertex conjugateVertex) {
    var turns = conjugateVertex.getExitEdgeSegments();

    /* determining number of edge segment is costly, instead use edges (which is larger or equal) and then copy result */
    double[] splittingRates = new double[conjugateVertex.getNumberOfEdges()];

    double totalSendingFlow = 0;
    int index = 0;
    for (var turn : turns) {
      double s_ab = getTurnSendingFlowPcuH(turn);
      splittingRates[index++] = s_ab;
      totalSendingFlow += s_ab;
    }
    ArrayUtils.divideBy(splittingRates, totalSendingFlow, 0);

    /* truncate */
    Arrays.copyOf(splittingRates, index);
    return splittingRates;
  }

  /**
   * Collect the splitting rate for a given conjugate link segment. Splitting rates are based on the current turn sending flows s_ab.
   * <p>
   * When collecting multiple splitting rates with the same in link, do not use this method but instead collect all splitting rates at once and then filter the ones you require it
   * is computationally more efficient.
   * 
   * 
   * @param turnSegment the turn to collect splitting rate for
   * @return splitting rate, when turn is not present or not used, zero is returned
   */
  public double getSplittingRate(final ConjugateEdgeSegment turnSegment) {
    double turnSendingFlow = getTurnSendingFlowPcuH(turnSegment);
    if (turnSendingFlow > 0) {
      double totalSendingFlow = getTotalSendingFlowFromPcuH(turnSegment.getUpstreamVertex());
      if (totalSendingFlow < turnSendingFlow) {
        var originalPair = turnSegment.getOriginalAdjcentEdgeSegments();
        LOGGER.severe(String.format("Total sending flow (%.10f) smaller than turn (%s,%s) sending flow (%.10f), this shouldn't happen", totalSendingFlow,
            originalPair.first().getXmlId(), originalPair.second().getXmlId(), turnSendingFlow));
      }
      return turnSendingFlow / totalSendingFlow;
    } else {
      return 0;
    }
  }

  /**
   * Shallow copy
   *
   * @return shallow copy
   */
  public ConjugateBushTurnData shallowClone() {
    return new ConjugateBushTurnData(this);
  }

  /**
   * Verify if any turn flows have been registered
   * 
   * @return true if so, false otherwise
   */
  public boolean hasTurnFlows() {
    return turnSendingFlows.isEmpty();
  }

}
