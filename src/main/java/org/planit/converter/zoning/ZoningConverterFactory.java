package org.planit.converter.zoning;

/**
 * Factory class for creating zoning converters
 * 
 * @author markr
 *
 */
public class ZoningConverterFactory {

  /**
   * Create the zoning converter
   * 
   * @param reader the reader to use
   * @param writer the writer to use
   * @return the converter that is created
   */
  public static ZoningConverter createConverter(ZoningReader reader, ZoningWriter writer) {
    return new ZoningConverter(reader, writer);
  }
}
