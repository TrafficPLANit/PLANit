package org.planit.converter.intermodal;

import org.planit.converter.MultiConverterReader;
import org.planit.network.MacroscopicNetwork;
import org.planit.zoning.Zoning;

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
