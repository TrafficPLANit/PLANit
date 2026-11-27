package org.goplanit.converter;

import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.goplanit.utils.math.TypeConversionUtil;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.settings.annotations.CoveredBySettingsKey;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsRead;
import org.goplanit.utils.settings.annotations.SettingsWrite;

/**
 * Settings of a converter reader that reads files: the file or folder it reads, the key {@code inputSource} at the top
 * level of its settings file
 * <p>
 * For example, reader settings made from {@code settings/reader.yaml} holding {@code inputSource: network} read the
 * folder {@code settings/network}, and reader settings given {@code setInputSource("https://example.org/network.xml")}
 * in code read that file from the web.
 * </p>
 * <p>
 * It exists so that every reader of files says where it reads with the same key and methods, whatever the format, as
 * {@link FileBasedConverterWriterSettings} does for where a writer writes. It holds nothing else, as readers share
 * nothing else: a country, which the defaults of some readers depend on, goes on those readers only. The header of the
 * files, which names the converter, sits on each converter's own settings.
 * </p>
 *
 * @author markr
 */
public abstract class FileBasedConverterReaderSettings implements ConverterReaderSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(FileBasedConverterReaderSettings.class.getCanonicalName());

  /** the file or folder to read, null when not given */
  private URL inputSource;

  /**
   * Constructor, without an input source
   */
  protected FileBasedConverterReaderSettings() {
  }

  /**
   * Constructor
   *
   * @param inputSource to read, null when not known yet
   */
  protected FileBasedConverterReaderSettings(final URL inputSource) {
    this.inputSource = inputSource;
  }

  /**
   * The file or folder to read
   *
   * @return the input source, null when not given
   */
  @SettingsRead(key = "inputSource")
  public final URL getInputSource() {
    return this.inputSource;
  }

  /**
   * Set the file or folder to read
   *
   * @param inputSource to read
   */
  @SettingsWrite(key = "inputSource", level = SettingsKeyLevel.STANDARD,
      description = "The file or folder to read, as a path relative to this settings file or a URL")
  public void setInputSource(final URL inputSource) {
    this.inputSource = inputSource;
  }

  /**
   * Set the file or folder to read from text, as a settings file gives it: a URL, such as
   * {@code https://example.org/network.xml}, or a local path, a relative one read against the working folder. The file
   * or folder need not exist yet
   *
   * @param inputSource to read, as text, null for none
   * @throws IllegalArgumentException when the text is neither a URL of a known protocol nor a path
   */
  @CoveredBySettingsKey("inputSource")
  public void setInputSource(final String inputSource) {
    setInputSource(inputSource == null ? null : TypeConversionUtil.fromString(inputSource, URL.class, Path.of("")));
  }

  /**
   * Log the input source; a reader's settings log what they add and call this for the rest
   *
   * @param level indentation level to apply
   */
  @Override
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsValue("Input source", getInputSource(), level));
  }

  /**
   * Clear the input source; a reader's settings put back what they add and call this for the rest
   */
  @Override
  public void reset() {
    this.inputSource = null;
  }
}
