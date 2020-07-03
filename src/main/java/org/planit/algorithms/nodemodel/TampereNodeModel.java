package org.planit.algorithms.nodemodel;

import java.util.concurrent.atomic.AtomicBoolean;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.aggregator.Aggregator;
import org.planit.exceptions.PlanItException;
import org.planit.math.Precision;
import org.planit.math.function.NullaryDoubleSupplier;
import org.planit.utils.misc.Pair;

/**
 * General First order node model implementation as proposed by Tampere et al. (2011). Here we utilise the algorithm description
 * as presented in Bliemer et al. (2014).
 * 
 * Each run of this node model requires two inputs, the mapping of the network to the local node and the 
 * 
 * Paper References: 
 * <li>Tampère, C. M. J., Corthout, R., Cattrysse, D., & Immers, L. H. (2011). 
 * A generic class of first order node models for dynamic macroscopic simulation of traffic flows. 
 * Transportation Research Part B: Methodological, 45(1), 289–309. https://doi.org/10.1016/j.trb.2010.06.004
 * </li>
 * <li>
 * Bliemer, M. C. J., Raadsen, M. P. H., Smits, E.-S., Zhou, B., & Bell, M. G. H. (2014). 
 * Quasi-dynamic traffic assignment with residual point queues incorporating a first order node model. 
 * Transportation Research Part B: Methodological, 68, 363–384. https://doi.org/10.1016/j.trb.2014.07.001
 * </li>
 * 
 * @author markr
 */
public class TampereNodeModel extends NodeModel {
      
  /** inputs for this node model instance */
  protected final TampereNodeModelInput inputs;
  /** track the number of in-link segments that have been processed */
  int numberOfInLinksProcessed;
  /** store the remaining receiving flows of each outgoing link segment */
  protected Array1D<Double> remainingReceivingFlows;
  /** store the remaining turn sending flows */
  protected Array2D<Double> remainingTurnSendingFlows;
    
  /** track which in-link segments are processed X_topbar. 
   * Note this is the inverse since it tracks processed rather than unprocessed link segments */
  protected boolean[] processedInLinkSegments;
  /** track which out-link segments are processed X_bottombar. 
   * Note this is the inverse since it tracks processed rather than unprocessed link segments */
  protected boolean[] processedOutLinkSegments;
  
  /** the result of the node model are the acceptance factors for each incoming link segment */
  protected Array1D<Double> incomingLinkSegmentFlowAcceptanceFactors;
  
  /**
   * Initialise the run conforming to Step 1 in Appendix A of Bliemer et al. 2014
   * 
   * @throws PlanItException thrown if error
   */
  protected void initialiseRun() throws PlanItException {
    PlanItException.throwIf(inputs.outgoingLinkSegmentReceivingFlows==null, "remaining receiving flows not initialised");
    // No in-link segments have been processed
    numberOfInLinksProcessed = 0;
    // t_ab = input t_ab
    remainingTurnSendingFlows = Array2D.PRIMITIVE64.copy(inputs.turnSendingFlows);
    // remaining R_b = initial R_b
    remainingReceivingFlows = inputs.outgoingLinkSegmentReceivingFlows.copy();
    // initialise processed in and out link segments (none), i.e., X_topbar and X_bottombar, respectively
    processedInLinkSegments   = new boolean[inputs.fixedInput.getNumberOfIncomingLinkSegments()];
    processedOutLinkSegments  = new boolean[inputs.fixedInput.getNumberOfIncomingLinkSegments()];
    // initialise flow acceptance factors to 1
    this.incomingLinkSegmentFlowAcceptanceFactors = Array1D.PRIMITIVE64.makeFilled(inputs.fixedInput.getNumberOfIncomingLinkSegments(), NullaryDoubleSupplier.ONE);    
  }
  
  /** Find most restricted unprocessed outgoing link segment based on the scaled sending flows
   * @return pair<factor,outlinkSegmentIndex> carrying the restriction factor for the most restricted out link segment index
   */
  protected Pair<Double, Integer> findMostRestrictingOutLinkSegmentIndex() {
    double foundRestrictionFactor = Double.POSITIVE_INFINITY;
    int foundOutLinkSegmentIndex = -1;
    for(int outLinkSegmentIndex = 0; outLinkSegmentIndex < processedOutLinkSegments.length; ++outLinkSegmentIndex) {
      if(!isOutLinkSegmentProcessed(outLinkSegmentIndex)) {
        double remainingReceivingFlow = remainingReceivingFlows.get(outLinkSegmentIndex);
        for(int inLinkSegmentIndex = 0; inLinkSegmentIndex < processedInLinkSegments.length; ++inLinkSegmentIndex) {
          if(!isInLinkSegmentProcessed(inLinkSegmentIndex)) {
            // SUM of t_ab
            double sumTurnSendingFlows = remainingTurnSendingFlows.aggregateColumn(outLinkSegmentIndex, Aggregator.SUM).doubleValue();
            
            // Only non-zero flows can lead to a restriction
            if(Precision.isGreaterEqual(sumTurnSendingFlows, Precision.EPSILON_6)) {
              // compute factor
              double currentOutgoingRestrictionFactor =
                  // remaining R_b for unprocessed b
                  remainingReceivingFlow
                  /
                  // lambda_a * SUM of t_ab 
                  ( inputs.capacityScalingFactors.get(inLinkSegmentIndex) 
                    * 
                    remainingTurnSendingFlows.aggregateColumn(outLinkSegmentIndex, Aggregator.SUM).doubleValue()
                  );
                          
              // update when more restrictive
              if(currentOutgoingRestrictionFactor < foundRestrictionFactor) {
                foundRestrictionFactor = currentOutgoingRestrictionFactor;
                foundOutLinkSegmentIndex = outLinkSegmentIndex;
              }              
            }
          }
        }
      }
    }
    return new Pair<Double,Integer>(foundRestrictionFactor,foundOutLinkSegmentIndex);
  }
  
  /** Based on the outlink segment, we determine which in links are demand constrained (if any). If there is one ore more, those are removed from
   *  the remaining unprocessed links and there sending flow is accepted as is. If not, then they are marked as capacity constrained and their sending
   *  flow must be reduced.
   *   
   * @param mostRestrictingOutLinkSegmentData out-link segment restriction factor and index
   */
  protected void updateSets(Pair<Double, Integer> mostRestrictingOutLinkSegmentData) {
    boolean demandConstrainedInLinkFound = updateDemandConstrainedInLinkSegments(mostRestrictingOutLinkSegmentData);
    if(!demandConstrainedInLinkFound) {
      updateCapacityConstrainedInLinkSegments(mostRestrictingOutLinkSegmentData);
    }
    setOutLinkSegmentProcessed(mostRestrictingOutLinkSegmentData.getSecond());
  }
 
  /**
   * @param mostRestrictingOutLinkSegmentData with <beta_b, b> with the former representing the outgoing link segment restriction factor, and the latter the index of b
   * @return true if demand constrained in link(s) is/are found, false otherwise
   */
  protected boolean updateDemandConstrainedInLinkSegments(Pair<Double, Integer> mostRestrictingOutLinkSegmentData) {
    final int mostRestrictedOutLinkIndex = mostRestrictingOutLinkSegmentData.getSecond();
    final double outLinkSegmentScalingFactorBeta = mostRestrictingOutLinkSegmentData.getFirst();
    
    // Y(m) = { a of unprocessed in-links | t_ab_topbar > 0,  lambda_a * beta_b > 1}
    AtomicBoolean foundDemandconstrainedInLink = new AtomicBoolean(false);      
    remainingTurnSendingFlows.loopColumn(mostRestrictedOutLinkIndex, (inLinkSegmentIndex,outLinkSegmentIndex) -> {
      final double turnSendingFlow = remainingTurnSendingFlows.get(inLinkSegmentIndex, outLinkSegmentIndex);
      // t_ab_topbar > 0 && a is unprocessed in link segment
      if( Precision.isGreaterEqual(turnSendingFlow,Precision.EPSILON_6) && !isInLinkSegmentProcessed((int)inLinkSegmentIndex)){       
          // lambda_a * beta_b
          final double requiredScalingFactor = inputs.capacityScalingFactors.get(inLinkSegmentIndex)*outLinkSegmentScalingFactorBeta; 
          if(Precision.isGreaterEqual(requiredScalingFactor, 1)) {
            // Demand constrained
            foundDemandconstrainedInLink.set(true);
            setInLinkSegmentProcessed((int)inLinkSegmentIndex);
            ++numberOfInLinksProcessed;
            // reduce remaining receiving flows and sending flows by removing accepted flows from it
            updateRemainingReceivingAndSendingFlows(inLinkSegmentIndex);
          }
      }
    });
           
    return foundDemandconstrainedInLink.get();
  }
  
  
  /** Based on the most restricting out-link segment, determine the flow acceptance factor for all unprocessed in-link with non-zero (remaining) flows towards 
   * this out-link segment
   * @param mostRestrictingOutLinkSegmentData out-link segment restriction factor and index
   */
  protected void updateCapacityConstrainedInLinkSegments(Pair<Double, Integer> mostRestrictingOutLinkSegmentData) {
    final int mostRestrictedOutLinkIndex = mostRestrictingOutLinkSegmentData.getSecond();
    final double outLinkSegmentScalingFactorBeta = mostRestrictingOutLinkSegmentData.getFirst();
    
    // Z(m) = { a of unprocessed in-links | t_ab_topbar > 0 }     
    remainingTurnSendingFlows.loopColumn(mostRestrictedOutLinkIndex, (inLinkSegmentIndex,outLinkSegmentIndex) -> {
      final double turnSendingFlow = remainingTurnSendingFlows.get(inLinkSegmentIndex, outLinkSegmentIndex);
      // t_ab_topbar > 0 && a is unprocessed in link segment
      if( Precision.isGreaterEqual(turnSendingFlow,Precision.EPSILON_6) &&
          !isInLinkSegmentProcessed((int)inLinkSegmentIndex)){ 
        // capacity constrained
        setInLinkSegmentProcessed((int)inLinkSegmentIndex);
        ++numberOfInLinksProcessed;
        // alpha_a = lambda_a*beta_b 
        double flowAcceptanceFactor = inputs.capacityScalingFactors.get(inLinkSegmentIndex)*outLinkSegmentScalingFactorBeta;
        // sending partially flow accepted, remove accepted portion from remaining receiving flow
        updateRemainingReceivingAndSendingFlows(inLinkSegmentIndex,flowAcceptanceFactor);
        // set alpha_a
        incomingLinkSegmentFlowAcceptanceFactors.set(inLinkSegmentIndex, flowAcceptanceFactor);
      }
    });    
  }  
  
  /**
   * Remove all turn sending flows from provided in-link from remaining receiving flows (whichever out-link they go to) for a demand
   * constrained in link
   * 
   * R_b' = R_b'-t_ab' for all out links b'
   * t_ab' = 0 (to ensure the turn flows are not accidentally reused when updating lambda in next iteration)
   * 
   * @param inLinkSegmentIndex the inLink to base the reduction on
   */
  protected void updateRemainingReceivingAndSendingFlows(long inLinkSegmentIndex) {
    updateRemainingReceivingAndSendingFlows(inLinkSegmentIndex,1); 
  }
  
  /**
   * Remove all accepted turn sending flows (by scaling with flow acceptance factor) 
   * from provided in-link from remaining receiving flows (whichever out-link they go to)
   * 
   * R_b' = R_b'-alpha_a*t_ab' for all out links b'
   * t_ab' = 0 (to ensure the turn flows are not accidentally reused when updating lambda in next iteration)
   * 
   * @param inLinkSegmentIndex the inLink to base the reduction on
   * @param flowAcceptanceFactor to scale the sending flows to accepted flow
   */
  protected void updateRemainingReceivingAndSendingFlows(long inLinkSegmentIndex, double flowAcceptanceFactor) {
    // Remove all turn sending flows from this in-link from remaining receiving flows (whichever out-link they go to)
    // R_b' = R_b'-t_ab' for all b' out links where a is demand constrained
    remainingTurnSendingFlows.loopRow(inLinkSegmentIndex, (i,outLinkSegmentIndex2) -> {
      double acceptedTurnSendingflowTo = remainingTurnSendingFlows.get(inLinkSegmentIndex, outLinkSegmentIndex2);
      remainingReceivingFlows.modifyOne(outLinkSegmentIndex2,PrimitiveFunction.SUBTRACT.by(acceptedTurnSendingflowTo*flowAcceptanceFactor));
      remainingTurnSendingFlows.fillRow(inLinkSegmentIndex, 0.0);
    });
  }
    
  
  /** Verify if in-link segment has been processed already or not
   * @param inLinkSegmentIndex
   * @return true if processed, false otherwise
   */
  protected boolean isInLinkSegmentProcessed(int inLinkSegmentIndex) {
    return !processedInLinkSegments[inLinkSegmentIndex];
  }
  
  /** Mark in-link segment as processed
   * @param inLinkSegmentIndex to mark as processed
   */
  protected void setInLinkSegmentProcessed(int inLinkSegmentIndex) {
    processedInLinkSegments[inLinkSegmentIndex] = true;
  }  
  
  /** Verify if out-link segment has been processed already or not
   * @param outLinkSegmentIndex
   * @return true if processed, false otherwise
   */
  protected boolean isOutLinkSegmentProcessed(int outLinkSegmentIndex) {
    return !processedOutLinkSegments[outLinkSegmentIndex];
  }
  
  /** Mark out-link segment as processed
   * @param inLinkSegmentIndex to mark as processed
   */  
  protected void setOutLinkSegmentProcessed(int outLinkSegmentIndex) {
    processedOutLinkSegments[outLinkSegmentIndex] = true;
  }


  /** Constructor
   * 
   * @param tampereNodeModelInput inputs for the model
   * @throws PlanItException thrown if error
   */
  public TampereNodeModel(TampereNodeModelInput tampereNodeModelInput) throws PlanItException {
    PlanItException.throwIf(tampereNodeModelInput==null, "Tampere node model input is null");
    this.inputs = tampereNodeModelInput; 
  }
  
  /** Run the Tampere node model
   * @return flowAcceptanceFactor per incoming linksegment index
   * @throws PlanItException thrown if error
   */
  public Array1D<Double> run() throws PlanItException {
    // Step 1. initialise
    initialiseRun();
    while(numberOfInLinksProcessed < inputs.fixedInput.getNumberOfIncomingLinkSegments()) {
      // Step 2 and 3. Find most restricting out link factor and segment index
      Pair<Double, Integer> mostRestrictingOutLinkSegmentData = findMostRestrictingOutLinkSegmentIndex();
      // Step 4a + (5 and 6). Demand constrained verification
      boolean demandConstrainedInLinkFound = updateDemandConstrainedInLinkSegments(mostRestrictingOutLinkSegmentData);
      // Step 4b + (5 and 6). Capacity constrained verification
      if(!demandConstrainedInLinkFound) {
        updateCapacityConstrainedInLinkSegments(mostRestrictingOutLinkSegmentData);
      }
      // Mark out link as processed
      setOutLinkSegmentProcessed(mostRestrictingOutLinkSegmentData.getSecond());
    }    
    return incomingLinkSegmentFlowAcceptanceFactors;
  }

}
