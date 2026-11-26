package org.goplanit.converter.zoning;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.zoning.Zoning;

/**
 * Interface to write a PLANit zoning to disk
 * 
 * @author markr
 *
 */
public interface ZoningWriter extends ConverterWriter<Zoning> {

  /**
   * Set the network the zoning refers to. The zoning converter sets the reader's network when the writer has none
   *
   * @param referenceNetwork the zoning refers to, set before invoking {@link #write(Object)}
   */
  public abstract void setReferenceNetwork(MacroscopicNetwork referenceNetwork);

  /**
   * The network the zoning refers to
   *
   * @return the reference network, null when none is set yet
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
