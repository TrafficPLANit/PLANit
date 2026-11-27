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

  /** each writer is expected to ensure that it relates to a reference network, which this method sets. When using a
   * converter, the converter sets it, so the user does not have to do so manually.
   *
   * @param referenceNetwork to supply writer with (before invoking {@link #write(Object)}
   */
  public abstract void setReferenceNetwork(MacroscopicNetwork referenceNetwork);

  /** access to network
   *
   * @return zoning
   */
  public abstract MacroscopicNetwork getReferenceNetwork();

  /** each writer is expected to ensure that it relates to a zoning, which this method sets. When using a converter,
   * the converter sets it, so the user does not have to do so manually.
   *
   * @param referenceZoning to supply writer with (before invoking {@link #write(Object)}
   */
  public abstract void setReferenceZoning(Zoning referenceZoning);

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
