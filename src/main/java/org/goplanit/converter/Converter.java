package org.goplanit.converter;

import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItException;

/**
 * Convert PLANit entities parsed by a reader from one format and persisted with a writer to another
 * 
 * @param <T> planit entity to convert
 * 
 * @author markr
 *
 */
public abstract class Converter<T> extends ConverterBase {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Converter.class.getCanonicalName());

  /**
   * constructor
   * 
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected Converter(ConverterReader<T> reader, ConverterWriter<T> writer) {
    super(reader, writer);
  }

  /**
   * Convert the reader's parsed content by passing it on to the writer. It is assumed both reader and writer are fully configured when this method is called
   * 
   * @throws PlanItException thrown if error
   */
  @SuppressWarnings("unchecked")
  public void convert() throws PlanItException {

    var reader = ((ConverterReader<T>) getReader());
    LOGGER.info(String.format("****************** [START] CONVERTER: READ %s [START] ********************", reader.getTypeDescription()));
    T entity = reader.read();
    reader.reset();
    LOGGER.info(String.format("****************** [END]   CONVERTER: READ %s [END]   ********************", reader.getTypeDescription()));

    ConverterWriter<T> writer = ((ConverterWriter<T>) getWriter());
    LOGGER.info(String.format("****************** [START] CONVERTER: WRITE %s [START] ********************", writer.getTypeDescription()));
    writer.write(entity);
    writer.reset();
    LOGGER.info(String.format("****************** [END]   CONVERTER: WRITE %s [END]   ********************", writer.getTypeDescription()));
  }

}
