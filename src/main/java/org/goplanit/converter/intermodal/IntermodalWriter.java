package org.goplanit.converter.intermodal;

import org.goplanit.converter.MultiConverterWriter;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.zoning.Zoning;

/**
 * abstract base class implementation to write an intermodal PLANit network to disk
 * 
 * @author markr
 *
 */
public interface IntermodalWriter extends MultiConverterWriter<MacroscopicNetwork, Zoning> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "INTERMODAL NETWORK";
  }

}
