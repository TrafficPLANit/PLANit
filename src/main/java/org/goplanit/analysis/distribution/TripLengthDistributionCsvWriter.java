package org.goplanit.analysis.distribution;

import org.goplanit.userclass.UserClass;
import org.goplanit.utils.csv.SimpleCsvWriter;
import org.goplanit.utils.id.IdAble;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.unit.DistanceUnit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Writes a trip length distribution to CSV: one row per key and bin, with the bin's bounds, its trips and the distance
 * they add up to, in the distribution's unit, and one row per key for its trips out of range
 * <p>
 * A distribution is compared with others, such as a sample with its full demand, in a spreadsheet or a plot, so it
 * goes to a plain table rather than to the PLANit XML format. The columns name the mode, the user class and the
 * traveller type, the latter two empty for a distribution per mode, so distributions of either kind read alike.
 * </p>
 *
 * @author markr
 */
public final class TripLengthDistributionCsvWriter {

  /** label of the row of the trips in no bin */
  private static final String OUT_OF_RANGE = "out of range";

  /**
   * A row: the key's columns followed by the bin's
   *
   * @param keyColumns the mode, user class and traveller type columns
   * @param binColumns the bin, its bounds, trips and distance
   * @return the row
   */
  private static Object[] rowOf(List<String> keyColumns, Object... binColumns) {
    var row = new ArrayList<Object>(keyColumns);
    row.addAll(Arrays.asList(binColumns));
    return row.toArray();
  }

  /**
   * Write a distribution, one row per key and bin and one per key for its trips out of range
   *
   * @param <K> type of the key
   * @param distribution to write
   * @param keyColumns the mode, user class and traveller type columns of a key
   * @param file to write to
   */
  private static <K extends IdAble> void write(
      TripLengthDistribution<K> distribution, Function<K, List<String>> keyColumns, Path file) {
    var bins = distribution.getBins();
    var headers = createHeaders(distribution.getDistanceUnit());
    try (var csvWriter = SimpleCsvWriter.create(file, headers.toArray(new String[0]))) {
      for (var key : distribution.getKeys()) {
        for (int binIndex = 0; binIndex < bins.size(); ++binIndex) {
          var bin = bins.getBin(binIndex);
          csvWriter.writeRow(rowOf(keyColumns.apply(key), bin.getLabel(), bin.getLowerBound(), bin.getUpperBound(),
              distribution.getTrips(key, binIndex), distribution.getDistance(key, binIndex)));
        }
        csvWriter.writeRow(rowOf(keyColumns.apply(key), OUT_OF_RANGE, null, null,
            distribution.getOutOfRangeTrips(key), distribution.getOutOfRangeDistance(key)));
      }
    }
  }

  /** static writer, not to be instantiated */
  private TripLengthDistributionCsvWriter() {
  }

  /**
   * The columns, in order, the bounds and the distance named by their unit, such as {@code lower_km} and {@code km}
   *
   * @param distanceUnit of the distribution written
   * @return the columns
   */
  public static List<String> createHeaders(DistanceUnit distanceUnit) {
    var unitName = distanceUnit.toString().toLowerCase();
    return List.of("mode", "user_class", "traveller_type", "bin", "lower_" + unitName, "upper_" + unitName, "trips",
        unitName);
  }

  /**
   * Write a distribution per mode, the user class and traveller type columns left empty
   *
   * @param distribution to write
   * @param file to write to
   */
  public static void writeByMode(TripLengthDistribution<Mode> distribution, Path file) {
    write(distribution, mode -> Arrays.asList(mode.getXmlId(), null, null), file);
  }

  /**
   * Write a distribution per user class, with its mode and traveller type
   *
   * @param distribution to write
   * @param file to write to
   */
  public static void writeByUserClass(TripLengthDistribution<UserClass> distribution, Path file) {
    write(distribution, userClass -> Arrays.asList(userClass.getMode().getXmlId(), userClass.getXmlId(),
        userClass.getTravelerType() == null ? null : userClass.getTravelerType().getXmlId()), file);
  }
}
