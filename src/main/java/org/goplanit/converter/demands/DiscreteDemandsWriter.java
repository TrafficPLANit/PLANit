package org.goplanit.converter.demands;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.demands.Demands;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.zoning.Zoning;

/**
 * Interface to write DiscreteDemands to disk
 * 
 * @author markr
 *
 */
public interface DiscreteDemandsWriter extends ConverterWriter<DiscreteDemands> {

  /** access to network
   *
   * @return zoning
   */
  public abstract MacroscopicNetwork getReferenceNetwork();

  /** access to zoning
   *
   * @return zoning
   */
  public abstract Zoning getReferenceZoning();

  /**
   * {@inheritDoc}
   */
  @Override
  public default String getTypeDescription() {
    return "DISCRETE DEMANDS";
  }

}
