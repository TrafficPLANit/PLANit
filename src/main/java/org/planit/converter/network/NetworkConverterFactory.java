package org.planit.converter.network;

/**
 * Factory class for creating network converters
 * 
 * @author markr
 *
 */
public class NetworkConverterFactory {

  /**
   * Create the network converter
   * 
   * @param reader the reader to use
   * @param writer the writer to use
   * @return the converter that is created
   */
  public static NetworkConverter createConverter(NetworkReader reader, NetworkWriter writer) {
    return new NetworkConverter(reader, writer);
  }
}
