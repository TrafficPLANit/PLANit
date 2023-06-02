package org.goplanit.converter.service;

/**
 * Factory class for creating service network converters
 * 
 * @author markr
 *
 */
public class ServiceNetworkConverterFactory {

  /**
   * Create the service network converter
   * 
   * @param reader the reader to use
   * @param writer the writer to use
   * @return the converter that is created
   */
  public static ServiceNetworkConverter create(ServiceNetworkReader reader, ServiceNetworkWriter writer) {
    return new ServiceNetworkConverter(reader, writer);
  }
}
