package org.goplanit.converter;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsRead;
import org.goplanit.utils.settings.annotations.SettingsWrite;

import java.util.logging.Logger;

/**
 * Settings relevant for file based converter writers, can be used as base implementation
 * <p>
 * Besides the keys of {@link FileBasedConverterWriterSettings}, the settings file of a writer of one file gives its
 * name, such as {@code fileName: roads.xml}, written in the output directory. It is here so that every writer of one
 * file takes that name with the same key and methods, whatever the format.
 * </p>
 *
 * @author markr
 *
 */
public class SingleFileBasedConverterWriterSettings extends FileBasedConverterWriterSettings
    implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER =
      Logger.getLogger(SingleFileBasedConverterWriterSettings.class.getCanonicalName());

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
  protected SingleFileBasedConverterWriterSettings(
      final String outputPathDirectory, final String fileName, final String countryName) {
    super(outputPathDirectory, countryName);
    this.setFileName(fileName);
  }

  /** Collect the file name to use
   *
   * @return file name to use
   */
  @SettingsRead(key = "fileName")
  public String getFileName() {
    return fileName;
  }

  /** Set the file name to use
   *
   * @param fileName to use
   */
  @SettingsWrite(key = "fileName", level = SettingsKeyLevel.STANDARD,
      description = "The name of the file to write, in the output directory, such as roads.xml")
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsValue("File name", getFileName(), level));
    super.logSettings(level);
  }

  /**
   * Reset content
   */
  public void reset() {
    super.reset();
    this.fileName = null;
  }

}