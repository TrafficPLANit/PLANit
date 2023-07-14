package org.goplanit.converter;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.StringUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.logging.Logger;

/**
 * Settings relevant for file based converter writers, can be used as base implementation
 *
 * @author markr
 *
 */
public class SingleFileBasedConverterWriterSettings extends FileBasedConverterWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SingleFileBasedConverterWriterSettings.class.getCanonicalName());

  /** destination file name to persist to */
  private String fileName = null;

  /** Validate the settings
   *
   * @return true when valid, false otherwise
   */
  protected boolean validate() {

    if(StringUtils.isNullOrBlank(fileName)) {
      LOGGER.severe("PLANit output file name is not provided, unable to continue");
      return false;
    }

    return super.validate();
  }

  /**
   * Default constructor
   */
  protected SingleFileBasedConverterWriterSettings() {
    super();
  }

  /**
   * Constructor
   *
   *  @param outputPathDirectory to use
   */
  protected SingleFileBasedConverterWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  protected SingleFileBasedConverterWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, countryName);
  }

  /**
   * Constructor
   *
   *  @param outputPathDirectory to use
   *  @param fileName to use
   *  @param countryName to use
   */
  protected SingleFileBasedConverterWriterSettings(final String outputPathDirectory, final String fileName, final String countryName) {
    super(outputPathDirectory, countryName);
    this.setFileName(fileName);
  }

  /** Collect the file name to use
   *
   * @return file name to use
   */
  public String getFileName() {
    return fileName;
  }

  /** Set the file name to use
   *
   * @param fileName to use
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  /**
   * Convenience method to log all the current settings
   */
  public void logSettings() {
    super.logSettings();
  }

  /**
   * Reset content
   */
  public void reset() {
    super.reset();
    this.fileName = null;
  }

}