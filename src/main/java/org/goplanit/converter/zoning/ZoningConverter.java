package org.goplanit.converter.zoning;

import java.util.logging.Logger;

import org.goplanit.converter.Converter;
import org.goplanit.zoning.Zoning;

/**
 * Zoning converter class able to convert a zoning network from one source to another
 * 
 * @author markr
 *
 */
public class ZoningConverter extends Converter<Zoning> {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(ZoningConverter.class.getCanonicalName());

  /**
   * {@inheritDoc}
   * <p>
   * The writer is given the network the reader's zoning refers to when it holds none, as it can be made before
   * anything is read
   * </p>
   *
   * @param zoning to write
   */
  @Override
  protected void write(Zoning zoning) {
    if (getWriter().getReferenceNetwork() == null) {
      getWriter().setReferenceNetwork(getReader().getReferenceNetwork());
    }
    super.write(zoning);
  }

  /**
   * constructor
   * 
   * @param reader to use for parsing
   * @param writer to use for persisting
   */
  protected ZoningConverter(ZoningReader reader, ZoningWriter writer) {
    super(reader,writer);
  }

  /**
   * get the reader
   * 
   * @return the reader
   */
  public ZoningReader getReader() {
    return (ZoningReader) super.getReader();
  }

  /**
   * get the writer
   * 
   * @return the writer
   */
  public ZoningWriter getWriter() {
    return (ZoningWriter) super.getWriter();
  }

}
