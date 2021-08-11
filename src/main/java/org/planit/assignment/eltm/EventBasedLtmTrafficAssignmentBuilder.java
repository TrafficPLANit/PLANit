package org.planit.assignment.eltm;

import org.planit.assignment.DynamicTrafficAssignmentBuilder;
import org.planit.assignment.TrafficAssignmentConfigurator;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.TransportLayerNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.zoning.Zoning;

/**
 * The eLTM traffic assignment builder. This is a dynamic traffic assignment builder specifically for eLTM
 *
 * @author markr
 *
 */
public class EventBasedLtmTrafficAssignmentBuilder extends DynamicTrafficAssignmentBuilder<EventBasedLtm> {

  /**
   * create the configurator for ELTM
   * 
   * @return eLTM configurator
   */
  @Override
  protected TrafficAssignmentConfigurator<EventBasedLtm> createConfigurator() throws PlanItException {
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
      final TransportLayerNetwork<?, ?> network) throws PlanItException {
    super(EventBasedLtm.class, groupId, inputBuilderListener, demands, zoning, network);
  }

}
