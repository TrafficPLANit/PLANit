package org.goplanit.assignment.ltm.sltm;

import org.goplanit.assignment.ltm.LtmTrafficAssignmentBuilder;
import org.goplanit.demands.Demands;
import org.goplanit.input.InputBuilderListener;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.zoning.Zoning;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * The sLTM traffic assignment builder. This is a capacity constrained traffic assignment builder specifically for sLTM. It user exposed options are made available through the
 * {@code StaticLtmConfigurator}.
 *
 * @author markr
 *
 */
public class StaticLtmTrafficAssignmentBuilder extends LtmTrafficAssignmentBuilder<StaticLtm> {

  /**
   * create the configurator for sLTM
   * 
   * @return sLTM configurator
   */
  @Override
  protected StaticLtmConfigurator createConfigurator() throws PlanItException {
    return new StaticLtmConfigurator();
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
  public StaticLtmTrafficAssignmentBuilder(IdGroupingToken groupId, final InputBuilderListener inputBuilderListener, final Demands demands, final Zoning zoning,
      final LayeredNetwork<?, ?> network) throws PlanItException {
    super(StaticLtm.class, groupId, inputBuilderListener, demands, zoning, network);
  }

}
