package org.goplanit.converter.zoning;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.zoning.Zoning;

/**
 * Interface to write a PLANit zoning to disk
 * 
 * @author markr
 *
 */
public interface ZoningWriter extends ConverterWriter<Zoning> {
  
  /**
   * {@inheritDoc}
   */  
  @Override
  default String getTypeDescription() {
    return "(OD/TRANSFER) ZONING";
  }

}
