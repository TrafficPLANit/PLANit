package org.goplanit.converter.demands;

import org.goplanit.converter.ConverterReader;
import org.goplanit.demands.Demands;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.zoning.Zoning;

/**
 * Interface to read Discrete demands
 * 
 * @author markr
 *
 */
public interface DiscreteDemandsReader extends ConverterReader<DiscreteDemands> {

  /** each DiscreteDemands reader is expected to ensure that its demand relates to a zoning
   * this reference zoning can be obtained (after reading is complete). the converter uses this to avoid the user
   * having to manually transfer this zoning to the writer which also requires this same zoning consistency
    *  This is what this method enables
   *
   * @return referenceZoning to supply demands writer with (after invoking {@link #read()}
   */
  public abstract Zoning getReferenceZoning();

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "DISCRETE DEMANDS";
  }

}
