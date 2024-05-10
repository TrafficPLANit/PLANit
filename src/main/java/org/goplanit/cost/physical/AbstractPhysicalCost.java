package org.goplanit.cost.physical;

import java.io.Serializable;
import java.util.logging.Logger;

import org.goplanit.component.PlanitComponent;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.GraphEntities;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.LinkSegment;
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

  /** Logger ot use */
  private static final Logger LOGGER = Logger.getLogger(AbstractPhysicalCost.class.getCanonicalName());

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
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public AbstractPhysicalCost(AbstractPhysicalCost abstractPhysicalCost, boolean deepCopy) {
    super(abstractPhysicalCost, deepCopy);
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
