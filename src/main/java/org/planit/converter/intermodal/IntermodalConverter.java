package org.planit.converter.intermodal;

import org.planit.converter.MultiConverter;
import org.planit.converter.MultiConverterReader;
import org.planit.converter.MultiConverterWriter;
import org.planit.network.TransportLayerNetwork;
import org.planit.zoning.Zoning;

/**
 * A converter that supports intermodal networks, i.e., a combination of both a network and a zoning with transfer zones
 * 
 * @author markr
 *
 */
public class IntermodalConverter extends MultiConverter<TransportLayerNetwork<?, ?>, Zoning> {

  /**
   * Constructor
   * 
   * @param reader for intermodal networks
   * @param writer for intermodal networks
   */
  protected IntermodalConverter(MultiConverterReader<TransportLayerNetwork<?, ?>, Zoning> reader, MultiConverterWriter<TransportLayerNetwork<?, ?>, Zoning> writer) {
    super(reader, writer);
  }

}
