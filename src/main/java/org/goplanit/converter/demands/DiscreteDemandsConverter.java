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
   * {@inheritDoc}
   *
   * make sure the demands have access to the reference zoning by taking it from the reader and placing it on the writer
   * (if not already available)
   *
   * @param discreteDemands demands to write
   */
  @Override
  protected void write(DiscreteDemands discreteDemands) {
    var reader = getReader();
    var writer = getWriter();

    if(writer.getReferenceZoning() == null) {
      /* in case the zoning is not present, because it is not available before the reading of demands has been completed
       * the converter will populate it on the writer, so the user does not need to (and is not able to) */
      writer.setReferenceZoning(reader.getReferenceZoning());
    }

    super.write(discreteDemands);
  }

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
