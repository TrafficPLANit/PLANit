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

  /** each DiscreteDemands reader is expected to ensure that its demand relates to a zoning, which can be obtained here
   * once reading is complete. The converter uses this to pass the zoning on to the writer, which requires the same
   * zoning, so the user does not have to do so manually
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
