package org.planit.converter.intermodal;

/**
 * Factory class for creating intermodal network converters for specific reader/writer combinations
 * 
 * @author markr
 *
 */
public class IntermodalNetworkConverterFactory {

  /**
   * Create the intermodal converter
   * 
   * @param reader the reader to use
   * @param writer the writer to use
   * @return the converter that is created
   */
  public static IntermodalNetworkConverter createConverter(IntermodalReader reader, IntermodalWriter writer) {
    return new IntermodalNetworkConverter(reader, writer);
  }
}
