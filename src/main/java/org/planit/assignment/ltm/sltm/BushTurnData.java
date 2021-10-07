package org.planit.assignment.ltm.sltm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.planit.graph.directed.acyclic.ACyclicSubGraph;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.misc.HashUtils;

/**
 * Track the turn based data of a bush.
 * <p>
 * For now we only track turn sending flows to minimise bookkeeping and memory usage, splitting rates are deduced from the turn sending flows when needed.
 * 
 * @author markr
 *
 */
public class BushTurnData implements Cloneable {

  /** track known bush turn sending flows s_ab by the combined hash code of incoming and outgoing link segment */
  private final Map<Integer, Double> turnSendingFlows;

  /**
   * Constructor
   */
  BushTurnData() {
    this.turnSendingFlows = new HashMap<Integer, Double>();
  }

  /**
   * copy constructor
   * 
   * @param bushTurnData to copy
   */
  public BushTurnData(BushTurnData bushTurnData) {
    this.turnSendingFlows = new HashMap<Integer, Double>(bushTurnData.turnSendingFlows);
  }

  /**
   * Update the turn sending flow for a given turn
   * 
   * @param fromSegment     of turn
   * @param toSegment       of turn
   * @param turnSendingFlow to update
   */
  public void updateTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment, final double turnSendingFlow) {
    turnSendingFlows.put(HashUtils.createCombinedHashCode(fromSegment, toSegment), turnSendingFlow);
  }

  /**
   * Add turn sending flow for a given turn
   * 
   * @param fromSegment     of turn
   * @param toSegment       of turn
   * @param turnSendingFlow to update
   */
  public void addTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment, final double turnSendingFlow) {
    int hashId = HashUtils.createCombinedHashCode(fromSegment, toSegment);
    turnSendingFlows.put(hashId, turnSendingFlows.get(hashId) + turnSendingFlow);
  }

  /**
   * Remove the turn
   * 
   * @param fromEdgeSegment of turn
   * @param toEdgeSegment   of turn
   */
  public void removeTurn(final EdgeSegment fromEdgeSegment, final EdgeSegment toEdgeSegment) {
    turnSendingFlows.remove(HashUtils.createCombinedHashCode(fromEdgeSegment, toEdgeSegment));
  }

  /**
   * Get the turn sending flow for a given turn
   * 
   * @param fromSegment from of turn
   * @param toSegment   to of turn
   * @return turn sending flow, 0 if not present
   */
  public double getTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    return turnSendingFlows.getOrDefault(HashUtils.createCombinedHashCode(fromSegment, toSegment), 0.0);
  }

  /**
   * Check if entry exists
   * 
   * @param fromSegment of turn
   * @param toSegment   of turn
   * @return true when present, false otherwise
   */
  public boolean containsTurnSendingFlow(final EdgeSegment fromSegment, final EdgeSegment toSegment) {
    return turnSendingFlows.containsKey(HashUtils.createCombinedHashCode(fromSegment, toSegment));
  }

  /**
   * Collect the splitting rates for a given link segment. Splitting rates are based on the current turn sending flows s_ab.
   * 
   * @param fromSegment to collect bush splitting rates for
   * @param dag         contains information on which downstream exit segments are used
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the downstream from segment vertex
   */
  public double[] getSplittingRates(final EdgeSegment fromSegment, final ACyclicSubGraph dag) {
    Set<EdgeSegment> exitEdgeSegments = fromSegment.getDownstreamVertex().getExitEdgeSegments();
    double[] splittingRates = new double[exitEdgeSegments.size()];

    double totalSendingFlow = 0;
    int index = 0;
    for (EdgeSegment exitSegment : exitEdgeSegments) {
      double s_ab = getTurnSendingFlow(fromSegment, exitSegment);
      splittingRates[index++] = s_ab;
      totalSendingFlow += s_ab;
    }
    for (index = 0; index < splittingRates.length; ++index) {
      splittingRates[index] /= totalSendingFlow;
    }
    return splittingRates;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BushTurnData clone() {
    return new BushTurnData(this);
  }
}
