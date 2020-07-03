package org.planit.algorithms.nodemodel;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.ojalgo.constant.PrimitiveMath;
import org.ojalgo.function.BigFunction;
import org.ojalgo.function.BinaryFunction;
import org.ojalgo.function.NullaryFunction;
import org.ojalgo.function.PrimitiveFunction;
import org.ojalgo.function.PrimitiveFunction.Unary;
import org.ojalgo.function.UnaryFunction;
import org.ojalgo.function.aggregator.Aggregator;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.random.Deterministic;
import org.planit.exceptions.PlanItException;
import org.planit.math.Precision;
import org.planit.math.function.NullaryDoubleSupplier;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentImpl;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex.EdgeSegments;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;

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
  
  /**
   * Inner class that holds the mapping of the inputs to/from the underlying physical network. Currently we only support
   * the PLANit network format for this mapping, but one can replace the initialisation of this mapping which would allow one to use the 
   * Tampere node model without modification for any other format/algorithm
   * 
   * By default we:
   * <li>map incoming link segments in order of appearance to index a=0,...,|A^in|-1</li>
   * <li>map outgoing link segments in order of appearance to index b=0,...,|B^out|-1/li>
   * <li>extract incoming link capacities C_a</li>
   * <li>extract receiving flow capacities such that R_b=C_b</li>
   * 
   *  Note that in case the receiving flows are not fixed at the outgoing link's capacity this can be omitted and provided as a separate input
   *  via the TampereNodeModelInput
   * 
   */
  public class TampereNodeModelFixedInput{
    
    /** Map link segments to local data mapping structure in order of appearance, i.e., first incoming link segment is placed in 
     * index 0, second in index 1 etc.
     * 
     * @param linkSegments, to map to
     * @param edgeSegments to map from
     * @throws PlanItException 
     */
    private void mapLinkSegments(ArrayList<MacroscopicLinkSegment> linkSegments, EdgeSegments edgeSegments) throws PlanItException {
      PlanItException.throwIf(edgeSegments == null, "edge segments to map are null");
      
      for(EdgeSegment incomingLinkSegment : edgeSegments) {
        if(incomingLinkSegment instanceof MacroscopicLinkSegment) {
          linkSegments.add((MacroscopicLinkSegment)incomingLinkSegment);
        }else {
          throw new PlanItException("Edges of node are not of type MacroScopicLinkSegment when mapping in Tampere node model");
        }
      }
    }
    
    /**
     * initialise array with link segment capacities. It is assumed the incoming link segments are available
     * 
     * @param arrayToInitialise the array to initialise
     * @param linkSegments to extract capacities from
     * @throws PlanItException thrown if link segments are null
     */
    private void initialiseWithCapacity(Array1D<Double> arrayToInitialise, ArrayList<MacroscopicLinkSegment> linkSegments) throws PlanItException {
      PlanItException.throwIf(linkSegments == null, "link segments to extract capacity from are null");

      arrayToInitialise = Array1D.PRIMITIVE64.makeZero(linkSegments.size());
      for(MacroscopicLinkSegment linkSegment : linkSegments) {
        arrayToInitialise.add(linkSegment.computeCapacity());
      }
    }    
    
    /** Map incoming link segments to node model compatible index in order of appearance, i.e., first incoming link segment is placed in 
     * index 0, second in index 1 etc.
     * @param incomingEdgeSegments to map
     * @throws PlanItException 
     */
    private void mapIncomingLinkSegments(EdgeSegments incomingEdgeSegments) throws PlanItException {
      this.incomingLinkSegments = new ArrayList<MacroscopicLinkSegment>(incomingEdgeSegments.getNumberOfEdges());
      mapLinkSegments(incomingLinkSegments, incomingEdgeSegments);
    }
    
    /** Map outgoingEdgeSegments link segments to node model compatible index in order of appearance, i.e., first incoming link segment is placed in 
     * index 0, second in index 1 etc.
     * @param outgoingEdgeSegments to map
     * @throws PlanItException 
     */
    private void mapOutgoingLinkSegments(EdgeSegments outgoingEdgeSegments) throws PlanItException {
      this.outgoingLinkSegments = new ArrayList<MacroscopicLinkSegment>(outgoingEdgeSegments.getNumberOfEdges());      
      mapLinkSegments(outgoingLinkSegments, outgoingEdgeSegments);
    }    
    
    /**
     * Extract the available incoming link capacities. It is assumed the incoming link segments are available
     * @throws PlanItException thrown if error
     */
    private void initialiseIncomingLinkSegmentCapacities() throws PlanItException {
      initialiseWithCapacity(incomingLinkSegmentCapacities,incomingLinkSegments);
    }
    
    /**
     * Extract the available outgoing link receiving flows by setting them to capacity. It is assumed the incoming link segments are available.
     * Note that assignment methods that support storage constraints, i.e., spillback do not always have receiving flows equal to capacity. In that
     * case receiving flows are not fixed and should not be initialised here but instead by provided on-the-fly for each tampere node model update
     * @param initialiseReceivingFlowsAtCapacity 
     * @throws PlanItException thrown if error
     */
    private void initialiseOutoingLinkSegmentReceivingFlows(boolean initialiseReceivingFlowsAtCapacity) throws PlanItException {
      if(initialiseReceivingFlowsAtCapacity) {
        initialiseWithCapacity(outgoingLinkSegmentReceivingFlows,outgoingLinkSegments);
      }else {
        outgoingLinkSegmentReceivingFlows = null;
      }
    }    
       
    /** mapping of incoming link index to link segment (if any), i.e., a=1,...|A^in|-1*/
    protected ArrayList<MacroscopicLinkSegment> incomingLinkSegments;
    /** mapping of outgoing link index to link segment (if any), i.e.e, b=1,...|B^out|-1 */
    protected ArrayList<MacroscopicLinkSegment> outgoingLinkSegments;
    
    /** store the capacities of each incoming link segment, i.e., C_a */
    protected Array1D<Double> incomingLinkSegmentCapacities;    
    /** store the receiving flows of each outgoing link segment at capacity, i.e., R_b=C_b*/
    protected Array1D<Double> outgoingLinkSegmentReceivingFlows;    
       
    /** Constructor. The network mapping class is meant to be created once for each node where the node model is applied
     * more than once. All fixed inputs conditioned on the network infrastructure are stored here and therefore can be 
     * reused with every update of the node model throughout a simulation.
     * 
     *  
     * @param node to use for extracting static inputs
     * @param initialiseReceivingFlowsAtCapacity indicate to initialise receiving flows at capacity (true), or not initialise them at all 
     * @param initialiseReceivingFlowsAtCapacity
     * @throws PlanItException thrown when error occurs
     */
    public TampereNodeModelFixedInput(Node node, boolean initialiseReceivingFlowsAtCapacity) throws PlanItException {
      // Set A^in
      mapIncomingLinkSegments(node.getEntryEdgeSegments());
      // Set A^out
      mapOutgoingLinkSegments(node.getEntryEdgeSegments());
      // Set C_a
      initialiseIncomingLinkSegmentCapacities();
      // Set R_b
      initialiseOutoingLinkSegmentReceivingFlows(initialiseReceivingFlowsAtCapacity);
    }
    
    /**
     * Collect number of incoming link segments
     * @return number of incoming link segments
     */
    public int getNumberOfIncomingLinkSegments() {
      return incomingLinkSegments.size();
    }
    
    /**
     * Collect number of outgoing link segments
     * @return number of outgoing link segments
     */
    public int getNumberOfOutgoingLinkSegments() {
      return outgoingLinkSegments.size();
    }    
           
  }
     
  /**
   * Inner class that allows the user to set all inputs for the TampereNodeModel, it takes fixed inputs and supplements it with the information
   * of the variable inputs, meaning inputs that can vary during the simulation such as sending flows, and potentially receiving flows
   * 
   */
  public class TampereNodeModelInput {
    
    /**
     * Verify if the provided inputs are compatible and populated
     * @param networkMapping
     * @param turnSendingFlows
     * @throws PlanItException 
     */
    private void verifyInputs(TampereNodeModelFixedInput networkMapping, Array2D<Double> turnSendingFlows) throws PlanItException {
        PlanItException.throwIf(networkMapping == null, "network mapping is null");
        PlanItException.throwIf(turnSendingFlows == null, "turn sending flows are null");
        PlanItException.throwIf(turnSendingFlows.countRows()    != networkMapping.getNumberOfIncomingLinkSegments() || 
                                turnSendingFlows.countColumns() != networkMapping.getNumberOfOutgoingLinkSegments(),
                                "Number of rows and/or columns in turn sending flows do not match the number of incoming and/or outgoing links in the node model mapping");              
    }
    
    /**
     * Compute the capacity scaling factors for each in link segment
     */
    private void computeInLinkSegmentCapacityScalingFactors() {
      // copy existing turn sending flows as starting point for scaled flows
      capacityScalingFactors = Array1D.PRIMITIVE64.makeZero(fixedInput.getNumberOfIncomingLinkSegments());
      
      for(int inIndex=0 ; inIndex < fixedInput.getNumberOfIncomingLinkSegments(); ++inIndex) {
        double inLinkSegmentCapacity = fixedInput.incomingLinkSegmentCapacities.get(inIndex);
        // Sum_b(t_ab)
        double inLinkSendingFlow = turnSendingFlows.aggregateRow(inIndex, Aggregator.SUM).doubleValue();
        // lambda_a = C_a/Sum_b(t_ab)        
        double lambdaIncomingLinkScalingFactor = Double.POSITIVE_INFINITY;
        if(Precision.isGreaterEqual(inLinkSendingFlow,0)) {
          lambdaIncomingLinkScalingFactor = inLinkSegmentCapacity/turnSendingFlows.aggregateRow(inIndex, Aggregator.SUM).doubleValue();
        }
        capacityScalingFactors.set(inIndex, lambdaIncomingLinkScalingFactor);                 
      }     
    }
    
    /** fixed inputs to use */
    protected final TampereNodeModelFixedInput fixedInput;
    
    /** the turn sending flows offered to the node model t_ab*/
    protected Array2D<Double> turnSendingFlows;
    
    /** store the available receiving flows of each outgoing link segment */
    protected Array1D<Double> outgoingLinkSegmentReceivingFlows;
        
    /** the scaling factor to scale sending flows up to capacity per in link segment */
    protected Array1D<Double> capacityScalingFactors;
    
        
    /** Constructor for a particular node model run
     * @param fixedInput the fixed inputs to use
     * @throws PlanItException  thrown if error
     */
    public TampereNodeModelInput(TampereNodeModelFixedInput fixedInput, Array2D<Double> turnSendingFlows) throws PlanItException {
      verifyInputs(fixedInput,turnSendingFlows);
      this.fixedInput = fixedInput;
      this.turnSendingFlows = turnSendingFlows;
      this.outgoingLinkSegmentReceivingFlows = fixedInput.outgoingLinkSegmentReceivingFlows;

      // determine all lambda_a*t_ab , with lambda_a=C_a/Sum_b(t_ab)
      computeInLinkSegmentCapacityScalingFactors();      
    }
    
    /** Constructor for a particular node model run. Here the receiving flows are provided explicitly, overriding the fixed receiving flows (if any)
     * from the networkMapping
     * 
     * @param networkMapping the fixed inputs to use
     * @throws PlanItException  thrown if error
     */
    public TampereNodeModelInput(TampereNodeModelFixedInput fixedInput, Array2D<Double> turnSendingFlows, Array1D<Double> outgoingLinkSegmentReceivingFlows) throws PlanItException {
      this(fixedInput, turnSendingFlows);
      this.outgoingLinkSegmentReceivingFlows = outgoingLinkSegmentReceivingFlows;      
    }    
    
   
  }
  
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
  
  /** run the Tampere node model
   * @throws PlanItException thrown if error
   */
  public void run() throws PlanItException {
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
    
  }



}
