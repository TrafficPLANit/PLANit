package org.goplanit.converter.demands;

/**
 * Factory class for creating demands converters
 * 
 * @author markr
 *
 */
public class DemandsConverterFactory {

  /**
   * Create the demands converter
   * 
   * @param reader the reader to use
   * @param writer the writer to use
   * @return the converter that is created
   */
  public static DemandsConverter create(DemandsReader reader, DemandsWriter writer) {
    return new DemandsConverter(reader, writer);
  }
}
