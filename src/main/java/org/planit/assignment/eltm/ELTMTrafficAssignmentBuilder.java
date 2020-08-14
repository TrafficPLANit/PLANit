package org.planit.assignment.eltm;

import org.planit.assignment.DynamicTrafficAssignmentBuilder;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * The eLTM traffic assignment builder. This is a dynamic traffic assignment builder specifically for eLTM
 *
 * @author markr
 *
 */
public class ELTMTrafficAssignmentBuilder extends DynamicTrafficAssignmentBuilder<ELTM> {

  /**
   * create the configurator for ELTM
   * 
   * @return eLTM configurator
   */
  @Override
  protected Configurator<ELTM> createConfigurator() throws PlanItException {
    return new ELTMConfigurator();
  }

  /**
   * Constructor
   * 
   * @param trafficAssignmentClass         the traffic assignment class we are building
   * @param groupId                        the id generation group this builder is part of
   * @param trafficComponentCreateListener the listeners registered on all traffic components this builder creates
   * @param demands                        the demands
   * @param zoning                         the zoning
   * @param physicalNetwork                the physical network
   * @throws PlanItException when triangular fundamental diagram cannot be instantiated
   */
  public ELTMTrafficAssignmentBuilder(IdGroupingToken groupId, final InputBuilderListener inputBuilderListener, final Demands demands, final Zoning zoning,
      final PhysicalNetwork<?, ?, ?> physicalNetwork) throws PlanItException {
    super(ELTM.class, groupId, inputBuilderListener, demands, zoning, physicalNetwork);
  }

}
