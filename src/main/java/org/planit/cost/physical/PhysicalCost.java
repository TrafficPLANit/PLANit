package org.planit.cost.physical;

import org.planit.cost.Cost;
import org.planit.network.physical.LinkSegment;

/**
 * Interface to classify costs of physical links
 * 
 * Physical links can be either InitialPhysicalCosts (which are read in at the start and are constant) or DynamicPhysicalCosts (which are derived from other inputs and are recalculated after each iteration).
 * 
 * @author markr
 *
 */
public interface PhysicalCost extends Cost<LinkSegment> {

 }
