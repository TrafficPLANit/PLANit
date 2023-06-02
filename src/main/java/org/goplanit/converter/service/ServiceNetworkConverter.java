package org.goplanit.converter.service;

import org.goplanit.converter.Converter;
import org.goplanit.converter.network.NetworkReader;
import org.goplanit.converter.network.NetworkWriter;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.ServiceNetwork;

import java.util.logging.Logger;

/**
 * Service network converter class able to convert a service network from one type to another
 * 
 * @author markr
 *
 */
public class ServiceNetworkConverter extends Converter<ServiceNetwork> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ServiceNetworkConverter.class.getCanonicalName());

  /**
   * constructor
   *
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected ServiceNetworkConverter(ServiceNetworkReader reader, ServiceNetworkWriter writer) {
    super(reader, writer);
  }

  /**
   * get the reader
   * 
   * @return the reader
   */
  public ServiceNetworkReader getReader() {
    return (ServiceNetworkReader) super.getReader();
  }

  /**
   * get the writer
   * 
   * @return the writer
   */
  public ServiceNetworkWriter getWriter() {
    return (ServiceNetworkWriter) super.getWriter();
  }

}
