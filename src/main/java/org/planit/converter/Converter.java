package org.planit.converter;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;

/**
 * Convert PLANit entities parsed by a reader from one format and persisted with a writer to another
 * 
 * @param <T> planit entity to convert 
 * 
 * @author markr
 *
 */
public abstract class Converter<T> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Converter.class.getCanonicalName());
  
  /**
   * the reader
   */
  private final ConverterReader<T> reader;

  /**
   * the writer
   */
  private final ConverterWriter<T> writer;
    
  /**
   * constructor
   * 
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected Converter(ConverterReader<T> reader, ConverterWriter<T> writer) {
    this.reader = reader;
    this.writer = writer;
  }
  
  /**
   * get the reader
   * 
   * @return the reader
   */
  protected ConverterReader<T> getReader() {
    return reader;
  }

  /**
   * get the writer
   * 
   * @return the writer
   */
  protected ConverterWriter<T> getWriter() {
    return writer;
  }   

  /**
   * Convert the reader's parsed content by passing it on to the writer. It is assumed both reader and writer are fully configured when this method is called
   * 
   * @throws PlanItException thrown if error
   */
  public void convert() throws PlanItException {

    LOGGER.info(String.format("****************** [START] CONVERTER: READ %s [START] ********************",reader.getTypeDescription()));
    T network = reader.read();
    LOGGER.info(String.format("****************** [END]   CONVERTER: READ %s [END]   ********************",reader.getTypeDescription()));

    LOGGER.info(String.format("****************** [START] NETWORK CONVERTER: WRITE %s [START] ********************",writer.getTypeDescription()));
    writer.write(network);
    LOGGER.info(String.format("****************** [END]   NETWORK CONVERTER: WRITE %s [END]   ********************",writer.getTypeDescription()));

  }

 
}
