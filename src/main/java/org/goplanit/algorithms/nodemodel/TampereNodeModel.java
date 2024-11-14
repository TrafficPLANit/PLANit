package org.goplanit.algorithms.nodemodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.function.NullaryDoubleSupplier;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.Aggregator;

/**
 * General First order node model implementation as proposed by Tampere et al. (2011). Here we utilise the algorithm description as presented in Bliemer et al. (2014).
 * <p>
 * Each run of this node model requires two inputs, the mapping of the network to the local node and the
 *</p>
 * <p>
 * Paper References:
 * <ul>
 * <li>Tampère, C. M. J., Corthout, R., Cattrysse, D., &amp; Immers, L. H. (2011). A generic class of first order node models for dynamic macroscopic simulation of traffic flows.
 * Transportation Research Part B: Methodological, 45(1), 289–309. <a href="https://doi.org/10.1016/j.trb.2010.06.004">doi.org/10.1016/j.trb.2010.06.004</a></li>
 * <li>Bliemer, M. C. J., Raadsen, M. P. H., Smits, E.-S., Zhou, B., &amp; Bell, M. G. H. (2014). Quasi-dynamic traffic assignment with residual point queues incorporating a first
 * order node model. Transportation Research Part B: Methodological, 68, 363–384. <a href="https://doi.org/10.1016/j.trb.2014.07.001">doi.org/10.1016/j.trb.2014.07.001</a></li>
 * </ul>
 *</p>
 * @author markr
 */
public class TampereNodeModel implements NodeModel {

  /** inputs for this node model instance */
  protected final TampereNodeModelInput inputs;
  /** track the number of in-link segments that have been processed */
  int numberOfInLinksProcessed;
  /** store the remaining receiving flows of each outgoing link segment */
  protected Array1D<Double> remainingReceivingFlows;
  /** store the remaining turn sending flows */
  protected Array2D<Double> scaledRemainingTurnSendingFlows;

  /**
   * track which in-link segments are processed X_topbar. Note this is the inverse since it tracks processed rather than unprocessed link segments
   */
  protected boolean[] processedInLinkSegments;

  /** the result of the node model are the acceptance factors for each incoming link segment */
  protected Array1D<Double> incomingLinkSegmentFlowAcceptanceFactors;

  /** the result of the node model - when run in turn absed mode - are the acceptance factors for each
   * turn */
  protected Array2D<Double> turnFlowAcceptanceFactors;

  /** default precision used, mainly used in relation between distinguishing non-zero flow from considering
   * something to be zero flow */
  protected double precisionEpsilon = Precision.EPSILON_9;

  /* optional outputs to collect */

  /* track most restricting out link for each in link */
  Map<Integer, Integer> mostRestrictingOutLinkIndexByInLinkIndex = new HashMap<Integer, Integer>();

  /**
   * Initialise the run conforming to Step 1 in Appendix A of Bliemer et al. 2014
   *
   * @param linkBasedDefault when true perform run in regular link based form, otherwise turn based
   *
   */
  protected void initialiseRun(boolean linkBasedDefault) {
    PlanItRunTimeException.throwIf(
            inputs.outgoingLinkSegmentReceivingFlows == null,
            "remaining receiving flows not initialised");

    // No in-link segments have been processed
    numberOfInLinksProcessed = 0;
    // t_ab = lambda_a*input t_ab
    scaledRemainingTurnSendingFlows = Array2D.PRIMITIVE64.copy(inputs.turnSendingFlows);
    scaledRemainingTurnSendingFlows.modifyMatchingInColumns(inputs.capacityScalingFactors, PrimitiveFunction.MULTIPLY);
    // remaining R_b = initial R_b
    remainingReceivingFlows = inputs.outgoingLinkSegmentReceivingFlows.copy();
    // initialise processed in link segments (none), i.e., X_bottombar
    processedInLinkSegments = new boolean[inputs.fixedInput.getNumberOfIncomingLinkSegments()];
    // initialise flow acceptance factors to 1
    if(linkBasedDefault) {
      this.incomingLinkSegmentFlowAcceptanceFactors = Array1D.PRIMITIVE64.makeFilled(inputs.fixedInput.getNumberOfIncomingLinkSegments(), NullaryDoubleSupplier.ONE);
    }else {
      this.turnFlowAcceptanceFactors =
              Array2D.PRIMITIVE64.makeFilled(
                      scaledRemainingTurnSendingFlows.countRows(),
                      scaledRemainingTurnSendingFlows.countColumns(),
                      NullaryDoubleSupplier.ONE);
    }
  }

  /**
   * Find most restricted unprocessed outgoing link segment based on the scaled sending flows
   * 
   * @return a pair of the restriction factor and outlinkSegmentIndex for the most restricted out link segment, null if no such out link could be found
   */
  protected Pair<Double, Integer> findMostRestrictingOutLinkSegmentIndex() {
    Integer foundOutLinkSegmentIndex = null;

    double foundRestrictionFactor = Double.POSITIVE_INFINITY;
    int numberOfOutLinkSegments = inputs.fixedInput.getNumberOfOutgoingLinkSegments();
    for (int outLinkSegmentIndex = 0; outLinkSegmentIndex < numberOfOutLinkSegments; ++outLinkSegmentIndex) {
      double remainingReceivingFlow = remainingReceivingFlows.get(outLinkSegmentIndex);
      // lambda_a * SUM of t_ab
      double sumScaledTurnSendingFlows = scaledRemainingTurnSendingFlows.aggregateColumn(outLinkSegmentIndex, Aggregator.SUM).doubleValue();

      // Only non-zero flows can lead to a restriction
      if (Precision.positive(sumScaledTurnSendingFlows, precisionEpsilon)) {
        // compute factor: remaining R_b for unprocessed b / SUM of lambda_a*t_ab
        double currentOutgoingRestrictionFactor = remainingReceivingFlow / sumScaledTurnSendingFlows;
        if (currentOutgoingRestrictionFactor < foundRestrictionFactor) {
          foundRestrictionFactor = currentOutgoingRestrictionFactor;
          foundOutLinkSegmentIndex = outLinkSegmentIndex;
        }
      }
    }

    if (foundOutLinkSegmentIndex == null) {
      return null;
    }
    return Pair.of(foundRestrictionFactor, foundOutLinkSegmentIndex);
  }

  /**
   * @param mostRestrictingOutLinkSegmentData with {@code <beta_b, b>} with the former representing the outgoing
   *                                          link segment restriction factor, and the latter the index of b
   * @return true if demand constrained in link(s) is/are found, false otherwise
   */
  protected boolean updateDemandConstrainedInLinkSegments(Pair<Double, Integer> mostRestrictingOutLinkSegmentData) {
    ArrayList<Long> demandConstrainedInLinksY = new ArrayList<>();

    /* ALL REMAINING DEMAND CONSTRAINED */
    if (mostRestrictingOutLinkSegmentData == null) {

      for (long index = 0; index < this.inputs.getFixedInput().getNumberOfIncomingLinkSegments(); ++index) {
        if (!isInLinkSegmentProcessed((int) index)) {
          demandConstrainedInLinksY.add(index);
        }
      }

    }
    /* POSSIBLE DEMAND CONSTRAINED IN-LINKS OF MOST RESTRICTING OUT_LINK */
    else {
      final int mostRestrictedOutLinkIndex = mostRestrictingOutLinkSegmentData.second();
      final double outLinkSegmentScalingFactorBeta = mostRestrictingOutLinkSegmentData.first();

      // Y(m) = { a of unprocessed in-links | t_ab_topbar > 0, lambda_a * beta_b > 1}
      scaledRemainingTurnSendingFlows.loopColumn(mostRestrictedOutLinkIndex, (inLinkSegmentIndex, outLinkSegmentIndex) -> {
        final double turnSendingFlow = scaledRemainingTurnSendingFlows.get(inLinkSegmentIndex, outLinkSegmentIndex);
        // t_ab_topbar > 0 && a is unprocessed in link segment
        if (Precision.greater(turnSendingFlow, precisionEpsilon) && !isInLinkSegmentProcessed((int) inLinkSegmentIndex)) {
          // lambda_a * beta_b
          final double requiredScalingFactor = inputs.capacityScalingFactors.get(inLinkSegmentIndex) * outLinkSegmentScalingFactorBeta;
          if (Precision.greaterEqual(requiredScalingFactor, 1, precisionEpsilon)) {
            demandConstrainedInLinksY.add(inLinkSegmentIndex);
          }
        }
      });
    }

    // update data based on identified demand constrained links
    demandConstrainedInLinksY.forEach((inLinkSegmentIndex) -> {
      setInLinkSegmentProcessed(inLinkSegmentIndex.intValue());
      ++numberOfInLinksProcessed;
      // reduce remaining receiving flows and sending flows by removing accepted flows from it
      updateRemainingReceivingAndSendingFlows(inLinkSegmentIndex);
    });

    return !demandConstrainedInLinksY.isEmpty();
  }

  /**
   * Based on the most restricting out-link segment, determine the flow acceptance factor for all
   * unprocessed in-link with non-zero (remaining) flows towards this out-link segment.
   * <p>
   *   When considering turn-based we also track the acceptance factors on turns even if they have zero-flow
   *   because any flow on that turn (in the limit to zero) would be exposed to the restriction on that exit link
   *   and has information that can be used in the context of route choice for example
   * </p>
   * 
   * @param mostRestrictingOutLinkSegmentData out-link segment restriction factor and index
   * @param linkBasedDefault when true apply the link based calculation and tracking of flow acceptance factor,
   *                         otherwise do it using the more involved turn based way
   */
  protected void updateCapacityConstrainedInLinkSegments(
          Pair<Double, Integer> mostRestrictingOutLinkSegmentData, boolean linkBasedDefault) {
    final int mostRestrictedOutLinkIndex = mostRestrictingOutLinkSegmentData.second();
    final double outLinkSegmentScalingFactorBeta = mostRestrictingOutLinkSegmentData.first();

    // Z(m) = { a of unprocessed in-links | t_ab_topbar > 0 }
    scaledRemainingTurnSendingFlows.loopColumn(mostRestrictedOutLinkIndex, (inLinkSegmentIndex, outLinkSegmentIndex) -> {
      final double turnSendingFlow = scaledRemainingTurnSendingFlows.get(inLinkSegmentIndex, outLinkSegmentIndex);
      // a is unprocessed in link segment
      if(!isInLinkSegmentProcessed((int) inLinkSegmentIndex)){

        // alpha_a = lambda_a*beta_b    (note that in case of turn based approach beta can be very large if zero turn and link flow,hence capping to 1)
        double flowAcceptanceFactor =
                Math.min(1, inputs.capacityScalingFactors.get(inLinkSegmentIndex) * outLinkSegmentScalingFactorBeta);

        // t_ab_topbar > 0
        boolean nonZeroTurnFlow = Precision.positive(turnSendingFlow, precisionEpsilon);
        if (nonZeroTurnFlow) {
          // sending partially flow accepted, remove accepted portion from remaining receiving flow
          updateRemainingReceivingAndSendingFlows(inLinkSegmentIndex, flowAcceptanceFactor);

          // mark in-link as processed
          setInLinkSegmentProcessed((int) inLinkSegmentIndex);
          ++numberOfInLinksProcessed;

          // track for user if required - predicated on link-based approach
          mostRestrictingOutLinkIndexByInLinkIndex.put((int) inLinkSegmentIndex, mostRestrictedOutLinkIndex);
        }

        if(linkBasedDefault && nonZeroTurnFlow){
          // set alpha_a - regular link-based approach where only single alpha per link based on non-zero flow into exit
          incomingLinkSegmentFlowAcceptanceFactors.set(inLinkSegmentIndex, flowAcceptanceFactor);
        }else if(!linkBasedDefault) {
          // set alpha_a - on turn level regardless if there is turn flow, applied alpha on link-level can be
          // deduced after the fact by multiplying out with turn-flows
          turnFlowAcceptanceFactors.set(inLinkSegmentIndex, outLinkSegmentIndex, flowAcceptanceFactor);
        }
      }
    });
  }

  /**
   * Remove all turn sending flows from provided in-link from remaining receiving flows (whichever out-link they go to) for a demand constrained in link
   * R_b' = R_b'-t_ab' for all out links b' t_ab' = 0 (to ensure the turn flows are not accidentally reused when updating lambda in next iteration)
   * 
   * @param inLinkSegmentIndex the inLink to base the reduction on
   */
  protected void updateRemainingReceivingAndSendingFlows(long inLinkSegmentIndex) {
    updateRemainingReceivingAndSendingFlows(inLinkSegmentIndex, 1);
  }

  /**
   * Remove all accepted turn sending flows (by scaling with flow acceptance factor) from provided in-link from remaining receiving flows (whichever out-link they go to)
   * R_b' = R_b'-alpha_a*t_ab' for all out links b' t_ab' = 0 (to ensure the turn flows are not accidentally reused when updating lambda in next iteration)
   * 
   * @param inLinkSegmentIndex   the inLink to base the reduction on
   * @param flowAcceptanceFactor to scale the sending flows to accepted flow
   */
  protected void updateRemainingReceivingAndSendingFlows(final long inLinkSegmentIndex, final double flowAcceptanceFactor) {
    // Remove all turn sending flows from this in-link from remaining receiving flows (whichever out-link they go to)
    // R_b' = R_b'-t_ab' for all b' out links where a is demand constrained
    inputs.turnSendingFlows.loopRow(inLinkSegmentIndex, (i, outLinkSegmentIndex2) -> {
      final double acceptedTurnSendingflowTo = inputs.turnSendingFlows.get(inLinkSegmentIndex, outLinkSegmentIndex2);
      remainingReceivingFlows.modifyOne(outLinkSegmentIndex2, PrimitiveFunction.SUBTRACT.by(acceptedTurnSendingflowTo * flowAcceptanceFactor));
    });
    // empty row in scaled sending flows: it won't be considered constructing next out-link restriction factor
    scaledRemainingTurnSendingFlows.fillRow(inLinkSegmentIndex, 0.0);
  }

  /**
   * Verify if in-link segment has been processed already or not
   * 
   * @param inLinkSegmentIndex the in link segment index
   * @return true if processed, false otherwise
   */
  protected boolean isInLinkSegmentProcessed(int inLinkSegmentIndex) {
    return processedInLinkSegments[inLinkSegmentIndex];
  }

  /**
   * Mark in-link segment as processed
   * 
   * @param inLinkSegmentIndex to mark as processed
   */
  protected void setInLinkSegmentProcessed(int inLinkSegmentIndex) {
    processedInLinkSegments[inLinkSegmentIndex] = true;
  }

  /**
   * Run the node model in either of the two run types, link-based which is the regular and default approach as in
   * the original paper, or turn-based where we track acceptance factors on each turn and zero flow turns receive
   * the acceptance factor of the exit link even if there is no flow into it. the latter provides more granular
   * information that may be helpful in route choice or cost calculations than the former.
   * <p>
   *   Calling method is expected to return the link or turn based results to the user. this method does not return
   *   anything it just populates the expected results depending on the flag provided.
   * </p>
   *
   * @param linkBasedDefault flag to apply
   */
  public void run(boolean linkBasedDefault){
    // Step 1. initialise
    initialiseRun(linkBasedDefault);
    while (numberOfInLinksProcessed < inputs.fixedInput.getNumberOfIncomingLinkSegments()) {
      // Step 2 and 3. Find most restricting out link factor and segment index
      Pair<Double, Integer> mostRestrictingOutLinkSegmentData = findMostRestrictingOutLinkSegmentIndex();
      // Step 4a + (5 and 6). Demand constrained verification
      boolean demandConstrainedInLinkFound = updateDemandConstrainedInLinkSegments(mostRestrictingOutLinkSegmentData);
      // Step 4b + (5 and 6). Capacity constrained verification
      if (!demandConstrainedInLinkFound) {
        updateCapacityConstrainedInLinkSegments(mostRestrictingOutLinkSegmentData, linkBasedDefault);
      }
    }
  }

  /**
   * Constructor
   * 
   * @param tampereNodeModelInput inputs for the model
   */
  public TampereNodeModel(TampereNodeModelInput tampereNodeModelInput){
    PlanItRunTimeException.throwIf(tampereNodeModelInput == null, "Tampere node model input is null");
    this.inputs = tampereNodeModelInput;
  }

  /**
   * Run the Tampere node model
   * 
   * @return flowAcceptanceFactor per incoming linksegment index
   */
  public Array1D<Double> run(){
    run(true);
    return incomingLinkSegmentFlowAcceptanceFactors;
  }

  /**
   * Run the Tampere node model but yielding turn level acceptance factors
   * <p>
   *   when a turn has no flow it may still have a flow acceptance factor below 1 as at this discontinuity point
   *   its limit from either side may give a different cost. Here we return the most restricting one of the two
   * </p>
   *
   * @return flowAcceptanceFactor per turn
   */
  public Array2D<Double> runTurnBased() {
    run(false);
    return turnFlowAcceptanceFactors;
  }

  /**
   * Provide access to the inputs used
   * 
   * @return inputs used
   */
  public TampereNodeModelInput getInputs() {
    return inputs;
  }

  /**
   * collect most restricted out link index by in link index. Only available after run and only for capacity constrained in links entries exist
   * 
   * @return map result, empty if no capacity constrained in links were found
   */
  public Map<Integer, Integer> getMostRestrictedOutLinkByInLink() {
    return this.mostRestrictingOutLinkIndexByInLinkIndex;
  }

  /**
   *   Precision used, mainly used in relation between distinguishing non-zero flow from considering
   *   something to be zero flow. Default 10^-9
   *
   * @param epsilon to use
   */
  public void setPrecisionEpsilon(double epsilon){
    this.precisionEpsilon = epsilon;
  }

  /**
   *   Precision used, mainly used in relation between distinguishing non-zero flow from considering
   *   something to be zero flow. Default 10^-9
   *
   * @return epsilon set
   */
  public double getPrecisionEpsilon(){
    return precisionEpsilon;
  }

}
