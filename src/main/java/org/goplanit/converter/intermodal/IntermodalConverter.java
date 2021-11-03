package org.goplanit.converter.intermodal;

import org.goplanit.converter.MultiConverter;
import org.goplanit.converter.MultiConverterReader;
import org.goplanit.converter.MultiConverterWriter;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.zoning.Zoning;

/**
 * A converter that supports intermodal networks, i.e., a combination of both a network and a zoning with transfer zones
 * 
 * @author markr
 *
 */
public class IntermodalConverter extends MultiConverter<MacroscopicNetwork, Zoning> {

  /**
   * Constructor
   * 
   * @param reader for intermodal networks
   * @param writer for intermodal networks
   */
  protected IntermodalConverter(MultiConverterReader<MacroscopicNetwork, Zoning> reader, MultiConverterWriter<MacroscopicNetwork, Zoning> writer) {
    super(reader, writer);
  }

}
