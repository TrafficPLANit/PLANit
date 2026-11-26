package org.goplanit.converter;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.settings.annotations.CoveredBySettingsKey;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsRead;
import org.goplanit.utils.settings.annotations.SettingsWrite;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Settings relevant for file based converter writers, can be used as base implementation
 * <p>
 * Its keys sit at the top level of the settings file of a writer: for example {@code outputDirectory: out},
 * {@code country: Australia} and {@code destinationCrs: EPSG:28356} in {@code run/network_writer.yaml} write to the
 * folder {@code run/out}, in the projected coordinate reference system of Sydney. They are here so that every writer
 * of files takes where and in which coordinate reference system it writes with the same keys and methods, whatever
 * the format. The header of the files, which names the converter, sits on each converter's own settings.
 * </p>
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

  /** default destination country to use if none is set */
  public static final String DEFAULT_COUNTRY = CountryNames.GLOBAL;

  /** The outputPathDirectory used
   *
   * @return directory used
   */
  @SettingsRead(key = "outputDirectory")
  public String getOutputDirectory() {
    return this.outputDirectory;
  }

  /** Set the outputDirectory used
   *
   * @param outputDirectory to use
   */
  @CoveredBySettingsKey("outputDirectory")
  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  /**
   * Set the folder to write to as a path, which a settings file gives relative to itself
   *
   * @param outputDirectory to use
   */
  @SettingsWrite(key = "outputDirectory", level = SettingsKeyLevel.STANDARD,
      description = "The folder to write to, relative to this settings file")
  public void setOutputDirectory(Path outputDirectory) {
    setOutputDirectory(outputDirectory == null ? null : outputDirectory.toString());
  }

  /** Collect country name used
   *
   * @return country name
   */
  @SettingsRead(key = "country")
  public String getCountry() {
    return countryName;
  }

  /** Set country name used
   *
   * @param countryName to use
   */
  @SettingsWrite(key = "country", level = SettingsKeyLevel.STANDARD,
      description = "The country written for, by its full English name, such as Australia, whose projected " +
          "coordinate reference system is written in when destinationCrs is not given")
  public void setCountry(String countryName) {
    this.countryName = countryName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsValue("Output directory", getOutputDirectory(), level));
    LOGGER.info(LoggingUtils.settingsValue("Country", getCountry(), level));
    LOGGER.info(LoggingUtils.settingsValue(
        "Destination CRS",
        getDestinationCoordinateReferenceSystem() != null ? getDestinationCoordinateReferenceSystem().getName() : null,
        level));
  }  

  /** Collect the destination Crs
   * 
   * @return destination Crs
   */
  @SettingsRead(key = "destinationCrs")
  public CoordinateReferenceSystem getDestinationCoordinateReferenceSystem() {
    return destinationCoordinateReferenceSystem;
  }

  /** Set the destination Crs to use (if not set, network's native Crs will be used, unless the user has specified a
   * specific country for which we have a more appropriate Crs registered) 
   * 
   * @param destinationCoordinateReferenceSystem to use
   */
  @SettingsWrite(key = "destinationCrs", level = SettingsKeyLevel.STANDARD,
      description = "The coordinate reference system to write in, by its code, such as EPSG:28356; when not given, " +
          "that of the country, else that of the data read")
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
