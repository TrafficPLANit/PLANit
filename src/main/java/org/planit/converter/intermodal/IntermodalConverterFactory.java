package org.planit.converter.intermodal;

/**
 * Factory class for creating intermodal network converters for specific reader/writer combinations
 * 
 * @author markr
 *
 */
public class IntermodalConverterFactory {

  /**
   * Create the intermodal converter
   * 
   * @param reader the reader to use
   * @param writer the writer to use
   * @return the converter that is created
   */
  public static IntermodalConverter createConverter(IntermodalReader reader, IntermodalWriter writer) {
    return new IntermodalConverter(reader, writer);
  }
}
