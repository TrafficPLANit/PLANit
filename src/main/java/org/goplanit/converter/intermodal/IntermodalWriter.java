package org.goplanit.converter.intermodal;

import org.goplanit.converter.MultiConverterWriter;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.zoning.Zoning;

/**
 * abstract base class implementation to write an intermodal PLANit network to disk
 * 
 * @author markr
 *
 */
public interface IntermodalWriter<T extends ServiceNetwork, U extends RoutedServices> extends MultiConverterWriter<MacroscopicNetwork, Zoning> {

  /**
   * {@inheritDoc}
   */
  @Override
  public default String getTypeDescription() {
    return "INTERMODAL NETWORK";
  }

  /**
   * Write a network to the writer's output format.
   *
   * @param physicalNetwork network to write
   * @param zoning to write
   * @param serviceNetwork to write
   * @param routedServices to write
   * @throws PlanItException thrown if error
   */
  public abstract void writeWithServices(MacroscopicNetwork physicalNetwork, Zoning zoning, T serviceNetwork, U routedServices) throws PlanItException;

}
