package org.planit.network.converter;

/**
 * Factory class for creating converters for specific reader/writer combinations
 * 
 * @author markr
 *
 */
public class NetworkConverterFactory {

  /**
   * Create the converter
   * 
   * @param reader the reader to use
   * @param writer the writer to use
   * @return the converter that is created
   */
  public static NetworkConverter createConverter(NetworkReader reader, NetworkWriter writer) {
    return new NetworkConverter(reader, writer);
  }
}
