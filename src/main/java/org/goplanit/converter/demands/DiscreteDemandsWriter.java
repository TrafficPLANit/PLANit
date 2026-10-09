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

  /** each writer is expected to ensure that it relates to a referenceNetwork
   * this reference referenceNetwork can be set. To avoid the user having to do this manually when
   * using a converter, the converter will do this for the user. This in turn requires a mechanism on
   * each demands writer to provide the reference referenceNetwork to the writer. This is what this method does.
   *
   * @param referenceNetwork to supply writer with (before invoking {@link #write(Object)}
   */
  public abstract void setReferenceNetwork(MacroscopicNetwork referenceNetwork);

  /** access to network
   *
   * @return zoning
   */
  public abstract MacroscopicNetwork getReferenceNetwork();

  /** each writer is expected to ensure that it relates to a zoning
   * this reference zoning can be set. To avoid the user having to do this manually when
   * using a converter, the converter will do this for the user. This in turn requires a mechanism on
   * each demands writer to provide the reference zoning to the writer. This is what this method does.
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
