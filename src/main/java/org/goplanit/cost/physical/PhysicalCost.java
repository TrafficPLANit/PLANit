package org.goplanit.cost.physical;

import org.goplanit.cost.Cost;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Interface to classify costs of physical links
 * 
 * Physical links can be either InitialPhysicalCosts (which are read in at the start and are constant) or PhysicalCosts (which are derived from other inputs and are recalculated
 * after each iteration).
 * 
 * @author markr
 *
 */
public interface PhysicalCost<LS extends LinkSegment> extends Cost<LS> {

  /** short hand for configuring physical cost with BPR function instance */
  public static final String BPR = BPRLinkTravelTimeCost.class.getCanonicalName();

  /** short hand for configuring physical cost with free flow function instance */
  public static final String FREEFLOW = FreeFlowLinkTravelTimeCost.class.getCanonicalName();

  /**
   * short hand for configuring physical cost compatible with steady state assignment methods, e.g., static methods with both inflow and outflow rates that can differ such as sLTM.
   * Based on the work of Raadsen and Bliemer (2019): Steady-state link travel time methods: Formulation, derivation, classification, and unification
   */
  public static final String STEADY_STATE = SteadyStateTravelTimeCost.class.getCanonicalName();

}
