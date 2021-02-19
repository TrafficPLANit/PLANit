package org.planit.converter.intermodal;

import org.planit.converter.MultiConverter;
import org.planit.converter.MultiConverterReader;
import org.planit.converter.MultiConverterWriter;
import org.planit.network.InfrastructureNetwork;
import org.planit.zoning.Zoning;

/**
 * A converter that supports intermodal networks, i.e., a combination of both a network and a zoning with transfer zones
 * 
 * @author markr
 *
 */
public class IntermodalNetworkConverter extends MultiConverter<InfrastructureNetwork<?>, Zoning> {

  /**
   * constructor
   * 
   * @param reader for intermodal networks
   * @param writer for intermodal networks
   */
  protected IntermodalNetworkConverter(MultiConverterReader<InfrastructureNetwork<?>, Zoning> reader, MultiConverterWriter<InfrastructureNetwork<?>, Zoning> writer) {
    super(reader, writer);
  }

}
