package org.planit.cost.physical;

import org.planit.component.PlanitComponent;
import org.planit.network.TransportLayerNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.UntypedPhysicalLayer;

/**
 * Class for dynamic cost functions, which calculate link segment costs for each iteration
 *
 * @author markr, gman6028
 *
 */
public abstract class AbstractPhysicalCost extends PlanitComponent<AbstractPhysicalCost> implements PhysicalCost {

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
   * @throws PlanItException thrown if error
   */
  public void populateWithCost(final UntypedPhysicalLayer<?, ?, ?, ?, ?, ?> physicalLayer, Mode mode, double[] costToFill) throws PlanItException {
    for (LinkSegment linkSegment : physicalLayer.getLinkSegments()) {
      costToFill[(int) linkSegment.getId()] = getSegmentCost(mode, (MacroscopicLinkSegment) linkSegment);
    }
  }

  /**
   * Initialize the cost parameter values in the network
   *
   * @param network the network
   * @throws PlanItException thrown if error
   */
  public abstract void initialiseBeforeSimulation(TransportLayerNetwork<?, ?> network) throws PlanItException;

}
