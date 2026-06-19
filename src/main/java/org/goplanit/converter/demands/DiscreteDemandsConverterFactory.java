package org.goplanit.converter.demands;

/**
 * Factory class for creating discrete demands converters
 * 
 * @author markr
 *
 */
public class DiscreteDemandsConverterFactory {

  /** dummy constructor */
  private DiscreteDemandsConverterFactory(){}

  /**
   * Create the demands converter
   * 
   * @param reader the reader to use
   * @param writer the writer to use
   * @return the converter that is created
   */
  public static DiscreteDemandsConverter create(DiscreteDemandsReader reader, DiscreteDemandsWriter writer) {
    return new DiscreteDemandsConverter(reader, writer);
  }
}
