package org.goplanit.cost.physical;

import java.io.Serializable;

import org.goplanit.component.PlanitComponent;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.UntypedPhysicalLayer;
import org.goplanit.utils.time.TimePeriod;

/**
 * Class for dynamic cost functions, which calculate link segment costs for each iteration and assumes an underlying macroscopic link segment approach
 *
 * @author markr, gman6028
 *
 */
public abstract class AbstractPhysicalCost extends PlanitComponent<AbstractPhysicalCost> implements PhysicalCost<MacroscopicLinkSegment>, Serializable {

  /** generated UID */
  private static final long serialVersionUID = 3657719270477537657L;

  /**
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected AbstractPhysicalCost(IdGroupingToken groupId) {
    super(groupId, AbstractPhysicalCost.class);
  }

  /**
   * Copy Constructor
   * 
   * @param abstractPhysicalCost to use
   */
  public AbstractPhysicalCost(AbstractPhysicalCost abstractPhysicalCost) {
    super(abstractPhysicalCost);
  }

  /**
   * Invoker expects (mode specific ) costs in passed in array to be filled, where each entry signifies a link segment by its id. This allows for more efficient implementations
   * than having to revert to one by one updates. It does however require network information hence its placement here where via the initialiseBeforeSimulation, the network is
   * provided
   * 
   * @param physicalLayer these cost pertain to
   * @param mode          the mode these costs pertain to
   * @param costToFill    array of link segment costs identified by the link segment's internal id
   */
  public void populateWithCost(final UntypedPhysicalLayer<?, ?, MacroscopicLinkSegment> physicalLayer, Mode mode, double[] costToFill) {
    for (var linkSegment : physicalLayer.getLinkSegments()) {
      costToFill[(int) linkSegment.getId()] = getGeneralisedCost(mode, linkSegment);
    }
  }

  /**
   * Initialize the cost parameter values in the network
   *
   * @param network the network
   * @throws PlanItException thrown if error
   */
  public abstract void initialiseBeforeSimulation(LayeredNetwork<?, ?> network) throws PlanItException;

  /**
   * Provide the cost calculation with information regarding the time period for which the cost is to be calculated
   * 
   * @param timePeriod to apply
   */
  public abstract void updateTimePeriod(final TimePeriod timePeriod);

}
