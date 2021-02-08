package org.planit.converter.zoning;

import org.planit.converter.ConverterReader;
import org.planit.zoning.Zoning;

/**
 * interface to read a PLANit zoning
 * 
 * @author markr
 *
 */
public interface ZoningReader extends ConverterReader<Zoning> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "(OD/TRANSFER) ZONING";
  }

}
