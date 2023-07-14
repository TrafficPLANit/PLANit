package org.goplanit.converter;

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
public class FileBasedConverterWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(FileBasedConverterWriterSettings.class.getCanonicalName());

  /** directory to persist to */
  private String outputDirectory = null;

  /** destination country to persist for */
  private String countryName = DEFAULT_COUNTRY;

  /** the coordinate reference system used for writing entities of this network */
  protected CoordinateReferenceSystem destinationCoordinateReferenceSystem = null;

  /** Validate the settings
   *
   * @return true when valid, false otherwise
   */
  protected boolean validate() {
    if(StringUtils.isNullOrBlank(outputDirectory)) {
      LOGGER.severe("PLANit output directory is not provided, unable to continue");
      return false;
    }
    /* other settings are not always mandatory */
    return true;
  }

  /** default destination country to use if none is set */
  public static String DEFAULT_COUNTRY = CountryNames.GLOBAL;

  /**
   * Default constructor
   */
  protected FileBasedConverterWriterSettings() {
  }

  /**
   * Constructor
   *
   *  @param outputPathDirectory to use
   */
  protected FileBasedConverterWriterSettings(final String outputPathDirectory) {
    this.outputDirectory = outputPathDirectory;
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  protected FileBasedConverterWriterSettings(final String outputPathDirectory, final String countryName) {
    this.outputDirectory = outputPathDirectory;
    this.setCountry(countryName);
  }

  /** The outputPathDirectory used
   * 
   * @return directory used
   */
  public String getOutputDirectory() {
    return this.outputDirectory;
  }
  
  /** Set the outputDirectory used
   * 
   * @param outputDirectory to use
   */
  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  /** Collect country name used
   * 
   * @return country name
   */
  public String getCountry() {
    return countryName;
  }

  /** Set country name used
   * 
   * @param countryName to use
   */
  public void setCountry(String countryName) {
    this.countryName = countryName;
  }

  /**
   * Convenience method to log all the current settings
   */
  public void logSettings() {

    if(getDestinationCoordinateReferenceSystem() != null) {
      LOGGER.info(String.format("Destination Coordinate Reference System set to: %s", getDestinationCoordinateReferenceSystem().getName()));
    }
    
  }  

  /** Collect the destination Crs
   * 
   * @return destination Crs
   */
  public CoordinateReferenceSystem getDestinationCoordinateReferenceSystem() {
    return destinationCoordinateReferenceSystem;
  }

  /** Set the destination Crs to use (if not set, network's native Crs will be used, unless the user has specified a
   * specific country for which we have a more appropriate Crs registered) 
   * 
   * @param destinationCoordinateReferenceSystem to use
   */
  public void setDestinationCoordinateReferenceSystem(CoordinateReferenceSystem destinationCoordinateReferenceSystem) {
    this.destinationCoordinateReferenceSystem = destinationCoordinateReferenceSystem;
  }

  /**
   * Reset content
   */
  public void reset() {
    this.outputDirectory = null;
    this.destinationCoordinateReferenceSystem = null;
    this.countryName = DEFAULT_COUNTRY;
  }  
    
}
