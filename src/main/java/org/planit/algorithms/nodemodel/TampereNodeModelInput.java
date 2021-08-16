package org.planit.algorithms.nodemodel;

import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.aggregator.Aggregator;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.math.Precision;

/**
 * Inner class that allows the user to set all inputs for the TampereNodeModel, it takes fixed inputs and supplements it with the information of the variable inputs, meaning inputs
 * that can vary during the simulation such as turn sending flows (t_ab), and potentially receiving flows (r_b)
 * 
 */
public class TampereNodeModelInput {

  /**
   * Verify if the provided inputs are compatible and populated
   * 
   * @param fixedInput       the fixed input
   * @param turnSendingFlows the turn sending flows
   * @throws PlanItException thrown if error
   */
  private void verifyInputs(TampereNodeModelFixedInput fixedInput, Array2D<Double> turnSendingFlows) throws PlanItException {
    PlanItException.throwIf(fixedInput == null, "network mapping is null");
    PlanItException.throwIf(turnSendingFlows == null, "turn sending flows are null");
    PlanItException.throwIf(
        turnSendingFlows.countRows() != fixedInput.getNumberOfIncomingLinkSegments() || turnSendingFlows.countColumns() != fixedInput.getNumberOfOutgoingLinkSegments(),
        "Number of rows and/or columns in turn sending flows do not match the number of incoming and/or outgoing links in the node model mapping");
  }

  /**
   * Compute the capacity scaling factors for each in link segment
   */
  private void computeInLinkSegmentCapacityScalingFactors() {
    // copy existing turn sending flows as starting point for scaled flows
    capacityScalingFactors = Array1D.PRIMITIVE64.makeZero(fixedInput.getNumberOfIncomingLinkSegments());

    for (int inIndex = 0; inIndex < fixedInput.getNumberOfIncomingLinkSegments(); ++inIndex) {
      double inLinkSegmentCapacity = fixedInput.incomingLinkSegmentCapacities.get(inIndex);
      // Sum_b(t_ab)
      double inLinkSendingFlow = turnSendingFlows.aggregateRow(inIndex, Aggregator.SUM).doubleValue();
      // lambda_a = C_a/Sum_b(t_ab)
      double lambdaIncomingLinkScalingFactor = Double.POSITIVE_INFINITY;
      if (Precision.isGreaterEqual(inLinkSendingFlow, 0)) {
        lambdaIncomingLinkScalingFactor = inLinkSegmentCapacity / turnSendingFlows.aggregateRow(inIndex, Aggregator.SUM).doubleValue();
      }
      capacityScalingFactors.set(inIndex, lambdaIncomingLinkScalingFactor);
    }
  }

  /** fixed inputs to use */
  protected final TampereNodeModelFixedInput fixedInput;

  /** the turn sending flows offered to the node model t_ab */
  protected Array2D<Double> turnSendingFlows;

  /** store the available receiving flows of each outgoing link segment */
  protected Array1D<Double> outgoingLinkSegmentReceivingFlows;

  /** the scaling factor to scale sending flows up to capacity per in link segment */
  protected Array1D<Double> capacityScalingFactors;

  /**
   * Constructor for a particular node model run
   * 
   * @param fixedInput       the fixed inputs to use
   * @param turnSendingFlows the turn sending flows
   * @throws PlanItException thrown if error
   */
  public TampereNodeModelInput(TampereNodeModelFixedInput fixedInput, Array2D<Double> turnSendingFlows) throws PlanItException {
    verifyInputs(fixedInput, turnSendingFlows);
    this.fixedInput = fixedInput;
    this.turnSendingFlows = turnSendingFlows;
    this.outgoingLinkSegmentReceivingFlows = fixedInput.outgoingLinkSegmentReceivingFlows;

    // determine all lambda_a*t_ab , with lambda_a=C_a/Sum_b(t_ab)
    computeInLinkSegmentCapacityScalingFactors();
  }

  /**
   * Constructor for a particular node model run. Here the receiving flows are provided explicitly, overriding the fixed receiving flows (if any) from the networkMapping
   * 
   * @param fixedInput                        the fixed inputs to use
   * @param turnSendingFlows                  the turn sending flows
   * @param outgoingLinkSegmentReceivingFlows the receiving flows
   * @throws PlanItException thrown if error
   */
  public TampereNodeModelInput(TampereNodeModelFixedInput fixedInput, Array2D<Double> turnSendingFlows, Array1D<Double> outgoingLinkSegmentReceivingFlows) throws PlanItException {
    this(fixedInput, turnSendingFlows);
    this.outgoingLinkSegmentReceivingFlows = outgoingLinkSegmentReceivingFlows;
  }

  /**
   * Collect the computed capacity scaling factors per in-link segment a such that lambda_a = C_a/Sum_b(t_ab)
   * 
   * @return capacityScalingFactor
   */
  public Array1D<Double> getCapacityScalingFactors() {
    return capacityScalingFactors;
  }

  /**
   * Provide access to the fixed input
   * 
   * @return fixed input used
   */
  public TampereNodeModelFixedInput getFixedInput() {
    return fixedInput;
  }

  /**
   * Access to the used turn sending flows
   * 
   * @return turn sending flows
   */
  public Array2D<Double> getTurnSendingFlows() {
    return turnSendingFlows;
  }

  /**
   * The receiving flows used
   * 
   * @return receiving flows used
   */
  public Array1D<Double> getUsedReceivingFlows() {
    return outgoingLinkSegmentReceivingFlows;
  }
}
