package org.goplanit.assignment.ltm.eltm;

import org.goplanit.assignment.TrafficAssignmentBuilder;
import org.goplanit.demands.Demands;
import org.goplanit.input.InputBuilderListener;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.zoning.Zoning;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * The eLTM traffic assignment builder. This is a dynamic traffic assignment builder specifically for eLTM
 *
 * @author markr
 *
 */
public class EventBasedLtmTrafficAssignmentBuilder extends TrafficAssignmentBuilder<EventBasedLtm> {

  /**
   * create the configurator for ELTM
   * 
   * @return eLTM configurator
   */
  @Override
  protected EventBasedLtmConfigurator createConfigurator() throws PlanItException {
    return new EventBasedLtmConfigurator();
  }

  /**
   * Constructor
   * 
   * @param groupId              the id generation group this builder is part of
   * @param inputBuilderListener the listeners registered on all traffic components this builder creates
   * @param demands              the demands
   * @param zoning               the zoning
   * @param network              the network
   * @throws PlanItException when triangular fundamental diagram cannot be instantiated
   */
  public EventBasedLtmTrafficAssignmentBuilder(IdGroupingToken groupId, final InputBuilderListener inputBuilderListener, final Demands demands, final Zoning zoning,
      final LayeredNetwork<?, ?> network) throws PlanItException {
    super(EventBasedLtm.class, groupId, inputBuilderListener, demands, zoning, network);
  }

}
