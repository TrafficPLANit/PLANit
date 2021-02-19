package org.planit.converter.intermodal;

import org.planit.converter.MultiConverterReader;
import org.planit.network.InfrastructureNetwork;
import org.planit.zoning.Zoning;

/**
 * abstract base class implementation to parser an intermodal PLANit network
 * 
 * @author markr
 *
 */
public interface IntermodalReader extends MultiConverterReader<InfrastructureNetwork<?>, Zoning> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "INTERMODAL NETWORK";
  }

}
