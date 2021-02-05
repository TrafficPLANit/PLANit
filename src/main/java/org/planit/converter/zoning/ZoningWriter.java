package org.planit.converter.zoning;

import org.planit.converter.ConverterWriter;
import org.planit.zoning.Zoning;

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
