package org.goplanit.test.converter;

import org.goplanit.converter.FileBasedConverterReaderSettings;
import org.goplanit.converter.SingleFileBasedConverterWriterSettings;
import org.goplanit.logging.Logging;
import org.goplanit.utils.geo.PlanitCrsUtils;
import org.goplanit.utils.settings.SettingsFileContentType;
import org.goplanit.utils.settings.SettingsSchema;
import org.goplanit.utils.settings.SettingsSource;
import org.goplanit.utils.settings.SettingsYamlLoader;
import org.goplanit.utils.settings.SettingsYamlWriter;
import org.goplanit.utils.settings.annotations.SettingsHeader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the keys every file based reader and writer takes from its settings files, on the core bases: where a reader
 * reads, and where and how a writer writes, read from files, given in code and written out again
 *
 * @author markr
 */
public class FileBasedConverterSettingsTest {

  /** Settings of a reader of files, filling no section, so the key of the reader base sits at the top level */
  @SettingsHeader(key = "planitTestReaderSettings", version = 1)
  public static final class TestReaderSettings extends FileBasedConverterReaderSettings {

    /** Constructor, without an input source */
    public TestReaderSettings() {
      super();
    }

    /** {@inheritDoc} */
    @Override
    public void logSettings(int level) {
      /* nothing of their own to log */
    }
  }

  /** Settings of a writer of one file, filling no section, so the keys of both writer bases sit at the top level */
  @SettingsHeader(key = "planitTestWriterSettings", version = 1)
  public static final class TestWriterSettings extends SingleFileBasedConverterWriterSettings {

    /** Constructor, with the defaults of the bases */
    public TestWriterSettings() {
      super();
    }
  }

  /** the header of the test reader settings */
  private static final String READER_HEADER = "planitTestReaderSettings: 1\n";

  /** the header of the test writer settings */
  private static final String WRITER_HEADER = "planitTestWriterSettings: 1\n";

  /** layout of the files of the test reader settings */
  private static final SettingsSchema READER_SCHEMA = new SettingsSchema(TestReaderSettings.class);

  /** layout of the files of the test writer settings */
  private static final SettingsSchema WRITER_SCHEMA = new SettingsSchema(TestWriterSettings.class);

  /** an input source on the web */
  private static final String SYDNEY_URL = "https://example.org/sydney.osm.pbf";

  /** the code of the projected coordinate reference system of Sydney */
  private static final String SYDNEY_CRS_CODE = "EPSG:28356";

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /**
   * Settings made from one source
   *
   * @param <T> type of the settings
   * @param schema layout of the source
   * @param settingsClass of the settings
   * @param source to make them from
   * @return the settings
   */
  private static <T> T create(SettingsSchema schema, Class<T> settingsClass, SettingsSource source) {
    return new SettingsYamlLoader(schema, List.of(source)).create(settingsClass);
  }

  /**
   * Settings written out, the keys differing from the defaults, without descriptions
   *
   * @param schema layout of the file
   * @param settings to write
   * @return the text of the file
   */
  private static String writtenOut(SettingsSchema schema, Object settings) {
    return new SettingsYamlWriter(schema, settings, SettingsFileContentType.KEYS_DIFFERING_FROM_DEFAULTS, false)
        .toString();
  }

  /**
   * A source of a file written in the test folder
   *
   * @param relativePath of the file in the test folder
   * @param text of the file, its header included
   * @return the source
   * @throws IOException when the file cannot be written
   */
  private SettingsSource writeFile(String relativePath, String text) throws IOException {
    Path path = tempDir.resolve(relativePath);
    Files.createDirectories(path.getParent());
    Files.writeString(path, text);
    return SettingsSource.ofFile(path);
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(FileBasedConverterSettingsTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  /** folder for the test files */
  @TempDir
  public Path tempDir;

  /** A relative input source is read against the folder of the settings file giving it: here a folder next to it */
  @Test
  public void inputSourceRelativeToTheSettingsFile() throws IOException, URISyntaxException {
    var settings = create(READER_SCHEMA, TestReaderSettings.class,
        writeFile("settings/planit.yaml", READER_HEADER + "inputSource: planit_osm\n"));
    assertEquals(tempDir.resolve("settings/planit_osm"), Path.of(settings.getInputSource().toURI()));
  }

  /** An input source given as a URL is that URL */
  @Test
  public void inputSourceByUrl() {
    var settings = create(READER_SCHEMA, TestReaderSettings.class,
        SettingsSource.ofText(READER_SCHEMA, "inputSource: " + SYDNEY_URL + "\n"));
    assertEquals(SYDNEY_URL, settings.getInputSource().toString());
  }

  /**
   * In code, the input source is given as text as in a file: a URL, a local file, or a folder, a relative path read
   * against the working folder
   */
  @Test
  public void inputSourceFromText() throws IOException, URISyntaxException {
    var settings = new TestReaderSettings();

    settings.setInputSource(SYDNEY_URL);
    assertEquals(SYDNEY_URL, settings.getInputSource().toString());

    var localFile = Files.writeString(tempDir.resolve("sydney.osm.pbf"), "");
    settings.setInputSource(localFile.toString());
    assertEquals(localFile, Path.of(settings.getInputSource().toURI()));

    settings.setInputSource("planit_osm");
    assertEquals(Path.of("planit_osm").toAbsolutePath(), Path.of(settings.getInputSource().toURI()));
  }

  /** Reader settings written out and read back give the same input source */
  @Test
  public void readerSettingsWrittenOutReadBack() {
    var settings = new TestReaderSettings();
    settings.setInputSource(SYDNEY_URL);

    var readBack = create(READER_SCHEMA, TestReaderSettings.class,
        SettingsSource.ofText(writtenOut(READER_SCHEMA, settings)));
    assertEquals(SYDNEY_URL, readBack.getInputSource().toString());
  }

  /** A reset of reader settings clears the input source, as the reader base does for every reader */
  @Test
  public void readerSettingsResetClearTheInputSource() {
    var settings = new TestReaderSettings();
    settings.setInputSource(SYDNEY_URL);
    settings.reset();
    assertNull(settings.getInputSource());
  }

  /**
   * A writer's keys from a file: the output directory read against the folder of the file, the country, the
   * destination CRS by its code, and the file name of the single file base
   */
  @Test
  public void writerKeysFromAFile() throws IOException {
    var settings = create(WRITER_SCHEMA, TestWriterSettings.class, writeFile("run/network_writer.yaml",
        WRITER_HEADER + "outputDirectory: out\ncountry: Australia\ndestinationCrs: " + SYDNEY_CRS_CODE + "\n" +
            "fileName: roads.xml\n"));

    assertEquals(tempDir.resolve("run/out"), Path.of(settings.getOutputDirectory()));
    assertEquals("Australia", settings.getCountry());
    assertEquals(PlanitCrsUtils.createCoordinateReferenceSystem(SYDNEY_CRS_CODE),
        settings.getDestinationCoordinateReferenceSystem());
    assertEquals("roads.xml", settings.getFileName());
  }

  /** Writer settings written out give the destination CRS by its code, and read back give the same settings */
  @Test
  public void writerSettingsWrittenOutReadBack() {
    var settings = new TestWriterSettings();
    settings.setOutputDirectory(tempDir.resolve("out"));
    settings.setCountry("Australia");
    settings.setDestinationCoordinateReferenceSystem(PlanitCrsUtils.createCoordinateReferenceSystem(SYDNEY_CRS_CODE));
    settings.setFileName("roads.xml");

    var written = writtenOut(WRITER_SCHEMA, settings);
    assertTrue(written.lines().anyMatch(("destinationCrs: " + SYDNEY_CRS_CODE)::equals));

    var readBack = create(WRITER_SCHEMA, TestWriterSettings.class, SettingsSource.ofText(written));
    assertEquals(Path.of(settings.getOutputDirectory()), Path.of(readBack.getOutputDirectory()));
    assertEquals(settings.getCountry(), readBack.getCountry());
    assertEquals(settings.getDestinationCoordinateReferenceSystem(), readBack.getDestinationCoordinateReferenceSystem());
    assertEquals(settings.getFileName(), readBack.getFileName());
  }
}
