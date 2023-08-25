package org.goplanit.converter.zoning;

import org.goplanit.converter.ConverterReader;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.zoning.Zoning;

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
