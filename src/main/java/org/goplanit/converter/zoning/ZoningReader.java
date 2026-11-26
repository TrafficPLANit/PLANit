package org.goplanit.converter.zoning;

import org.goplanit.converter.ConverterReader;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.zoning.Zoning;

/**
 * interface to read a PLANit zoning
 * 
 * @author markr
 *
 */
public interface ZoningReader extends ConverterReader<Zoning> {

  /**
   * The network the zoning read refers to, read by this reader or given to it. The zoning converter gives it to the
   * writer, so a writer can be made before anything is read
   *
   * @return the reference network, available after {@link #read()}
   */
  public abstract MacroscopicNetwork getReferenceNetwork();

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "(OD/TRANSFER) ZONING";
  }

}
