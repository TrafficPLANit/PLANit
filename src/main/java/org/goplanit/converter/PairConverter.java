package org.goplanit.converter;

import java.util.logging.Logger;

import org.goplanit.converter.intermodal.IntermodalReader;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.misc.Pair;

/**
 * Convert multiple PLANit entities parsed by a reader from one format and persisted with a writer to another
 * 
 * @param <T> planit entity to convert
 * @param <U> planit entity to convert
 * 
 * @author markr
 *
 */
public abstract class PairConverter<T, U> extends ConverterBase {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PairConverter.class.getCanonicalName());

  /**
   * constructor
   * 
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected PairConverter(PairConverterReader<T, U> reader, PairConverterWriter<T, U> writer) {
    super(reader, writer);
  }

  /**
   * Convert the reader's parsed content by passing it on to the writer. It is assumed both reader and writer are fully configured when this method is called
   * 
   * @throws PlanItException thrown if error
   */
  @Override
  public void convert() throws PlanItException {
    PairConverterReader<T, U> reader = (PairConverterReader<T, U>) getReader();
    LOGGER.info(String.format("****************** [START] CONVERTER: READ %s [START] ********************", reader.getTypeDescription()));
    Pair<T, U> multiResult = reader.read();
    reader.reset();
    LOGGER.info(String.format("****************** [END]   CONVERTER: READ %s [END]   ********************", reader.getTypeDescription()));

    PairConverterWriter<T, U> writer = (PairConverterWriter<T, U>) getWriter();
    LOGGER.info(String.format("****************** [START] CONVERTER: WRITE %s [START] ********************", writer.getTypeDescription()));
    writer.write(multiResult.first(), multiResult.second());
    writer.reset();
    LOGGER.info(String.format("****************** [END]   CONVERTER: WRITE %s [END]   ********************", writer.getTypeDescription()));
  }
}
