package org.goplanit.converter.network;

import java.util.logging.Logger;

import org.goplanit.converter.Converter;
import org.goplanit.network.LayeredNetwork;

/**
 * Network converter class able to convert a network from one type to another
 * 
 * @author markr
 *
 */
public class NetworkConverter extends Converter<LayeredNetwork<?,?>> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(NetworkConverter.class.getCanonicalName());

  /**
   * constructor
   * 
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected NetworkConverter(NetworkReader reader, NetworkWriter writer) {
    super(reader,writer);
  }

  /**
   * get the reader
   * 
   * @return the reader
   */
  public NetworkReader getReader() {
    return (NetworkReader) super.getReader();
  }

  /**
   * get the writer
   * 
   * @return the writer
   */
  public NetworkWriter getWriter() {
    return (NetworkWriter) super.getWriter();
  }

}
