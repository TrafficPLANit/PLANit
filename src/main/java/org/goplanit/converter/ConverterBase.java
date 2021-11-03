package org.goplanit.converter;

import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItException;

/**
 * Base converter for PLANit entities
 * 
 * 
 * @author markr
 *
 */
public abstract class ConverterBase {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ConverterBase.class.getCanonicalName());

  /**
   * the reader
   */
  private final ConverterEntity reader;

  /**
   * the writer
   */
  private final ConverterEntity writer;

  /**
   * get the reader
   * 
   * @return the reader
   */
  protected ConverterEntity getReader() {
    return reader;
  }

  /**
   * get the writer
   * 
   * @return the writer
   */
  protected ConverterEntity getWriter() {
    return writer;
  }

  /**
   * constructor
   * 
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected ConverterBase(ConverterEntity reader, ConverterEntity writer) {
    this.reader = reader;
    this.writer = writer;
  }

  /**
   * Convert the reader's content and pass it on to the writer to convert it. It is assumed both reader and writer are fully configured when this method is called
   * 
   * @throws PlanItException thrown if error
   */
  public abstract void convert() throws PlanItException;

}
