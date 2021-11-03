package org.goplanit.converter.intermodal;

import org.goplanit.converter.MultiConverterReader;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.zoning.Zoning;

/**
 * Abstract base class implementation to parser an intermodal PLANit network
 * 
 * @author markr
 *
 */
public interface IntermodalReader extends MultiConverterReader<MacroscopicNetwork, Zoning> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "INTERMODAL NETWORK";
  }

}
