package org.goplanit.converter.demands;

import java.util.logging.Logger;

import org.goplanit.converter.Converter;
import org.goplanit.converter.ConverterReader;
import org.goplanit.converter.ConverterWriter;
import org.goplanit.demands.Demands;
import org.goplanit.utils.exceptions.PlanItException;

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
   * {@inheritDoc}
   *
   * make sure the demands have access to the reference zoning by taking it from the reader and placing it on the writer
   * (if not already available)
   *
   * @param demands
   * @throws PlanItException
   */
  @Override
  protected void write(Demands demands) throws PlanItException {
    var reader = getReader();
    var writer = getWriter();

    if(writer.getReferenceZoning() == null) {
      /* in case the zoning is not present, because it is not available before the reading of demands has been completed
       * the converter will populate it on the writer, so the user does not need to (and is not able to) */
      writer.setReferenceZoning(reader.getReferenceZoning());
    }

    super.write(demands);
  }

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
