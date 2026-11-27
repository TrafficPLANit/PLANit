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

  /** each demands writer is expected to ensure that its demand relates to a zoning, which this method sets. When
   * using a converter, the converter sets it, so the user does not have to do so manually.
   * 
   * @param referenceZoning to supply demands writer with (before invoking {@link #write(Object)}
   */
  public abstract void setReferenceZoning(Zoning referenceZoning);

  /**
   * Access to zoning
   * @return zoning
   */
  public abstract Zoning getReferenceZoning();

  /**
   * {@inheritDoc}
   */
  @Override
  public default String getTypeDescription() {
    return "DEMANDS";
  }

}
