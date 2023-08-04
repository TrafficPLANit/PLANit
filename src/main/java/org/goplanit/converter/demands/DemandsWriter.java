package org.goplanit.converter.demands;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.demands.Demands;
import org.goplanit.zoning.Zoning;

/**
 * Interface to write a PLANit demands to disk
 * 
 * @author markr
 *
 */
public interface DemandsWriter extends ConverterWriter<Demands> {

  /** each demands writer is expected to ensure that its demand relate to a zoning
   * this reference zoning can be set. To avoid the user having to do this manualy when
   * using a converter, the converter will do this for the user. This in turn requires a mechanism on
   * each demands writer to provide the reference zoning to the demands writer. This is what this method does.
   * 
   * @param referenceZoning to supply demands writer with (before invoking {@link #write(Object)}
   */
  public abstract void setReferenceZoning(Zoning referenceZoning);

  public abstract Zoning getReferenceZoning();

  /**
   * {@inheritDoc}
   */
  @Override
  public default String getTypeDescription() {
    return "DEMANDS";
  }

}
