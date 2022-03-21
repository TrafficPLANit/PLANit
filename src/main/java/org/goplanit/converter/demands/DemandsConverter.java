package org.goplanit.converter.demands;

import java.util.logging.Logger;

import org.goplanit.converter.Converter;
import org.goplanit.demands.Demands;

/**
 * Network converter class able to convert demands from one type to another
 * 
 * @author markr
 *
 */
public class DemandsConverter extends Converter<Demands> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DemandsConverter.class.getCanonicalName());

  /**
   * constructor
   * 
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected DemandsConverter(DemandsReader reader, DemandsWriter writer) {
    super(reader, writer);
  }

  /**
   * get the reader
   * 
   * @return the reader
   */
  public DemandsReader getReader() {
    return (DemandsReader) super.getReader();
  }

  /**
   * get the writer
   * 
   * @return the writer
   */
  public DemandsWriter getWriter() {
    return (DemandsWriter) super.getWriter();
  }

}
