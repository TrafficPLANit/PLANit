package org.planit.cost.physical;

import org.planit.cost.Cost;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;

/**
 * Interface to classify costs of physical links
 * 
 * Physical links can be either InitialPhysicalCosts (which are read in at the start and are constant) or PhysicalCosts (which are derived from other inputs and are recalculated
 * after each iteration).
 * 
 * @author markr
 *
 */
public interface PhysicalCost extends Cost<MacroscopicLinkSegment> {

  /** short hand for configuring physical cost with BPR function instance */
  public static final String BPR = BPRLinkTravelTimeCost.class.getCanonicalName();

}
