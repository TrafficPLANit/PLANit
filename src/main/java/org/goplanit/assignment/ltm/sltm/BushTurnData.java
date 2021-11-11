package org.goplanit.assignment.ltm.sltm;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.goplanit.utils.graph.EdgeSegment;
import org.goplanit.utils.math.Precision;

/**
 * Track the turn based data of a bush.
 * <p>
 * For now we only track turn sending flows to minimise bookkeeping and memory usage, splitting rates are deduced from the turn sending flows when needed.
 * 
 * @author markr
 *
 */
public class BushTurnData implements Cloneable {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(BushTurnData.class.getCanonicalName());

  /** track known bush turn sending flows s_ab by the combined key of incoming and outgoing link segments */
  private final MultiKeyMap<Object, Double> turnSendingFlows;

  /**
   * Constructor
   */
  BushTurnData() {
    this.turnSendingFlows = new MultiKeyMap<Object, Double>();
  }

  /**
   * copy constructor
   * 
   * @param bushTurnData to copy
   */
  public BushTurnData(BushTurnData bushTurnData) {
    this.turnSendingFlows = bushTurnData.turnSendingFlows.clone();
  }

  /**
   * Update the turn sending flow for a given turn
   * 
   * @param fromSegment     of turn
   * @param toSegment       of turn
   * @param turnSendingFlow to update
   */
  public void updateTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment, final double turnSendingFlow) {
    turnSendingFlows.put(fromSegment, toSegment, turnSendingFlow);
  }

  /**
   * Add turn sending flow for a given turn
   * 
   * @param fromSegment     of turn
   * @param toSegment       of turn
   * @param turnSendingFlow to update
   */
  public void addTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment, final double turnSendingFlow) {

    double newSendingFlow = turnSendingFlow;
    Double existingSendingFlow = turnSendingFlows.get(fromSegment, toSegment);
    if (existingSendingFlow != null) {
      newSendingFlow += existingSendingFlow;
    }

    if (!Precision.isPositive(newSendingFlow)) {
      LOGGER.warning(String.format("Turn (%s to %s) sending flow negative (%.2f) after adding %.2f flow, reset to 0.0", fromSegment.getXmlId(), toSegment.getXmlId(),
          newSendingFlow, turnSendingFlow));
      newSendingFlow = 0;
    }

    turnSendingFlows.put(fromSegment, toSegment, newSendingFlow);
  }

  /**
   * Remove the turn
   * 
   * @param fromEdgeSegment of turn
   * @param toEdgeSegment   of turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    turnSendingFlows.remove(fromEdgeSegment, toEdgeSegment);
  }

  /**
   * Get the turn sending flow for a given turn
   * 
   * @param fromSegment from of turn
   * @param toSegment   to of turn
   * @return turn sending flow, 0 if not present
   */
  public double getTurnSendingFlowPcuH(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    Double existingSendingFlow = turnSendingFlows.get(fromSegment, toSegment);
    if (existingSendingFlow != null) {
      return existingSendingFlow;
    } else {
      return 0;
    }
  }

  /**
   * Total sending flows s_a from given segment
   * 
   * @param fromSegment to use
   * @return sending flow s_a
   */
  public double getTotalSendingFlowPcuH(final EdgeSegment fromSegment) {
    double totalSendingFlow = 0;
    for (EdgeSegment exitSegment : fromSegment.getDownstreamVertex().getExitEdgeSegments()) {
      double s_ab = getTurnSendingFlowPcuH(fromSegment, exitSegment);
      totalSendingFlow += s_ab;
    }
    return totalSendingFlow;
  }

  /**
   * Check if entry exists
   * 
   * @param fromSegment of turn
   * @param toSegment   of turn
   * @return true when present, false otherwise
   */
  public boolean containsTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    return turnSendingFlows.containsKey(fromSegment, toSegment);
  }

  /**
   * Collect the splitting rates for a given link segment. Splitting rates are based on the current turn sending flows s_ab. In case no flows are present, zero splitting rates for
   * all turns are returned
   * 
   * @param fromSegment to collect bush splitting rates for
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the downstream from segment vertex
   */
  public double[] getSplittingRates(final EdgeSegment fromSegment) {
    Set<EdgeSegment> exitEdgeSegments = fromSegment.getDownstreamVertex().getExitEdgeSegments();
    double[] splittingRates = new double[exitEdgeSegments.size()];

    double totalSendingFlow = 0;
    int index = 0;
    for (EdgeSegment exitSegment : exitEdgeSegments) {
      double s_ab = getTurnSendingFlowPcuH(fromSegment, exitSegment);
      splittingRates[index++] = s_ab;
      totalSendingFlow += s_ab;
    }
    if (Precision.isPositive(totalSendingFlow)) {
      for (index = 0; index < splittingRates.length; ++index) {
        splittingRates[index] /= totalSendingFlow;
      }
    } else {
      for (index = 0; index < splittingRates.length; ++index) {
        splittingRates[index] = 0;
      }
    }
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
    if (Precision.isPositive(turnSendingFlow)) {
      return turnSendingFlow / getTotalSendingFlowPcuH(fromSegment);
    } else {
      return 0;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BushTurnData clone() {
    return new BushTurnData(this);
  }
}