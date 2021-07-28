package org.planit.converter.intermodal;

import org.planit.converter.MultiConverterWriter;
import org.planit.network.MacroscopicNetwork;
import org.planit.zoning.Zoning;

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
