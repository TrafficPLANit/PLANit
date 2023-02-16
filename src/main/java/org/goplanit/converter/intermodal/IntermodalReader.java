package org.goplanit.converter.intermodal;

import org.goplanit.converter.PairConverterReader;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.misc.Quadruple;
import org.goplanit.zoning.Zoning;

/**
 * Abstract base class implementation to parser an intermodal PLANit network. In addition, implementations may choose
 * to also support the parsing of additional service networks and routed services.
 * 
 * @author markr
 *
 */
public interface IntermodalReader<T extends ServiceNetwork,U extends RoutedServices> extends PairConverterReader<MacroscopicNetwork, Zoning> {

  /**
   * {@inheritDoc}
   */
  @Override
  public default String getTypeDescription() {
    return "INTERMODAL NETWORK";
  }

  /**
   * Verify if services can be read as part of the intermodal reader exercise, only when true {@link #readWithServices()} can be called
   * @return true when services can be parsed, false otherwise and only the regular {@link #read()} is available
   */
  public abstract boolean supportServiceConversion();

  public abstract Quadruple<MacroscopicNetwork, Zoning, T, U> readWithServices() throws PlanItException;

}
