package org.planit.converter;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.misc.Pair;

/**
 *  Convert multiple PLANit entities parsed by a reader from one format and persisted with a writer to another
 *  
 * @param <T> planit entity to convert
 * @param <U> planit entity to convert  
 * 
 * @author markr
 *
 */
public abstract class MultiConverter<T, U> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MultiConverter.class.getCanonicalName());
  
  /**
   * the reader
   */
  private final MultiConverterReader<T,U> reader;

  /**
   * the writer
   */
  private final MultiConverterWriter<T,U> writer;
  
  /**
   * verify if mode conversion between reader and writer is valid
   * 
   * @return true if valid, false otherwise
   */
  protected abstract boolean isModeConversionValid();
  
  /**
   * constructor
   * 
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected MultiConverter(MultiConverterReader<T,U> reader, MultiConverterWriter<T,U> writer) {
    this.reader = reader;
    this.writer = writer;
  }
  
  /**
   * get the reader
   * 
   * @return the reader
   */
  protected MultiConverterReader<T,U> getReader() {
    return reader;
  }

  /**
   * get the writer
   * 
   * @return the writer
   */
  protected MultiConverterWriter<T,U> getWriter() {
    return writer;
  }   

  /**
   * Convert the reader's parsed content by passing it on to the writer. It is assumed both reader and writer are fully configured when this method is called
   * 
   * @throws PlanItException thrown if error
   */
  public void convert() throws PlanItException {
    /* verify mode compatibility */
    if (isModeConversionValid()) {

      LOGGER.info(String.format("****************** [START] CONVERTER: READ %s [START] ********************",reader.getTypeDescription()));
      Pair<T,U> multiResult = reader.read();
      LOGGER.info(String.format("****************** [END]   CONVERTER: READ %s [END]   ********************",reader.getTypeDescription()));

      LOGGER.info(String.format("****************** [START] NETWORK CONVERTER: WRITE %s [START] ********************",writer.getTypeDescription()));
      writer.write(multiResult.first(), multiResult.second());
      LOGGER.info(String.format("****************** [END]   NETWORK CONVERTER: WRITE %s [END]   ********************",writer.getTypeDescription()));

    } else {
      LOGGER.severe("unable to convert, modes mapping between reader and writer is incompatible");
    }
  }

 
}
