package org.planit.network.converter;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;

/**
 * Network converter class able to convert a network from one type to another
 * 
 * @author markr
 *
 */
public class NetworkConverter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NetworkConverter.class.getCanonicalName());

  /**
   * the reader
   */
  protected final NetworkReader reader;

  /**
   * the writer
   */
  protected final NetworkWriter writer;

  /**
   * verify if mode conversion between reader and writer is valid
   * 
   * @return true if valid, false otherwise
   */
  protected boolean isModeConversionValid() {
    LOGGER.info("validating proposed mode conversion validity...");
    return false;
  }

  /**
   * constructor
   * 
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected NetworkConverter(NetworkReader reader, NetworkWriter writer) {
    this.reader = reader;
    this.writer = writer;
  }

  /**
   * Convert the network parsed by the reader by passing it on to the writer. It is assumed both reader and writer are fully configured when this method is called
   * 
   * @throws PlanItException thrown if error
   */
  public void convert() throws PlanItException {
    /* verify mode compatibility */
    if (isModeConversionValid()) {
      writer.write(reader.read());
    } else {
      LOGGER.severe("unable to convert network, modes mapping between reader and writer is incompatible");
    }
  }

  /**
   * get the reader
   * 
   * @return the reader
   */
  public NetworkReader getReader() {
    return reader;
  }

  /**
   * get the writer
   * 
   * @return the writer
   */
  public NetworkWriter getWriter() {
    return writer;
  }

}
