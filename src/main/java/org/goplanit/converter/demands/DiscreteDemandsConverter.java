package org.goplanit.converter.demands;

import org.goplanit.converter.Converter;
import org.goplanit.demands.Demands;
import org.goplanit.demands.discrete.DiscreteDemands;

import java.util.logging.Logger;

/**
 * Discrete demands converter class able to convert discrete demands from one type to another
 * 
 * @author markr
 *
 */
public class DiscreteDemandsConverter extends Converter<DiscreteDemands> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DiscreteDemandsConverter.class.getCanonicalName());

  /**
   * constructor
   *
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected DiscreteDemandsConverter(DiscreteDemandsReader reader, DiscreteDemandsWriter writer) {
    super(reader, writer);
  }

  /**
   * get the reader
   * 
   * @return the reader
   */
  public DiscreteDemandsReader getReader() {
    return (DiscreteDemandsReader) super.getReader();
  }

  /**
   * get the writer
   * 
   * @return the writer
   */
  public DiscreteDemandsWriter getWriter() {
    return (DiscreteDemandsWriter) super.getWriter();
  }

}
