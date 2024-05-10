package org.goplanit.cost.physical;

import org.goplanit.cost.Cost;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;

import java.util.logging.Logger;

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

  static final Logger LOGGER = Logger.getLogger(PhysicalCost.class.getCanonicalName());

  /** short-hand for configuring physical cost with BPR function instance */
  public static final String BPR = BprLinkTravelTimeCost.class.getCanonicalName();

  /** short hand for configuring physical cost with free flow function instance */
  public static final String FREEFLOW = FreeFlowLinkTravelTimeCost.class.getCanonicalName();

  /**
   * short hand for configuring physical cost compatible with steady state assignment methods, e.g., static methods with both inflow and outflow rates that can differ such as sLTM.
   * Based on the work of Raadsen and Bliemer (2019): Steady-state link travel time methods: Formulation, derivation, classification, and unification
   */
  public static final String STEADY_STATE = SteadyStateTravelTimeCost.class.getCanonicalName();

  /**
   * Invoker expects (mode specific ) costs in passed in array to be filled, where each entry signifies a link segment by its id. This allows for more efficient implementations
   * than having to revert to one by one updates. It does however require network information hence its placement here where via the initialiseBeforeSimulation, the network is
   * provided
   *
   * @param layer these cost pertain to
   * @param mode          the mode these costs pertain to
   * @param costToFill    array of link segment costs identified by the link segment's internal id
   */
  public default void populateWithCost(final UntypedPhysicalLayer<?, ?, LS> layer, Mode mode, double[] costToFill) {
    if(layer == null){
      LOGGER.warning("Network layer not available, unable to populate link segment costs");
      return;
    }
    populateWithCost(layer.getLinkSegments(), mode, costToFill);
  }

  /**
   * Invoker expects (mode specific ) costs in passed in array to be filled, where each entry signifies a link segment by its id. This allows for more efficient implementations
   * than having to revert to one by one updates. It does however require network information hence its placement here where via the initialiseBeforeSimulation, the network is
   * provided
   *
   * @param linkSegments these cost pertain to
   * @param mode         the mode these costs pertain to
   * @param costToFill   array of link segment costs identified by the link segment's internal id
   */
  public default void populateWithCost(final GraphEntities<LS> linkSegments, Mode mode, double[] costToFill) {
    if(linkSegments.isEmpty()){
      LOGGER.warning("No link segments available, unable to populate link segment costs");
    }
    for (var linkSegment : linkSegments) {
      var generalisedCost = getGeneralisedCost(mode, linkSegment);
      if (generalisedCost < 0.0) {
        LOGGER.warning(String.format("Link segment cost is negative %.2f for link segment %s ", generalisedCost, linkSegment.getIdsAsString()));
      }
      costToFill[(int) linkSegment.getId()] = generalisedCost;
    }
  }

}
