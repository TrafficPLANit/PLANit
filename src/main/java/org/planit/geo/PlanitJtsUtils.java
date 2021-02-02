package org.planit.geo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.coordinate.PointArray;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Vertex;

/**
 * General geotools related utils. Uses geodetic distance when possible. In case the CRS is not based on an ellipsoid (2d plane) it will simply compute the distance between
 * coordinates using Pythagoras with the unit distance in meters, consistent with the {@code CartesianAuthorityFactory.GENERIC_2D}
 * 
 * It is assumed that x coordinate refers to latitude and y coordinate refers to longitude
 * 
 * @author markr
 *
 */
public class PlanitJtsUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitJtsUtils.class.getCanonicalName());

  /**
   * Default Coordinate Reference System: WGS84
   */
  public static final DefaultGeographicCRS DEFAULT_GEOGRAPHIC_CRS = PlanitOpenGisUtils.DEFAULT_GEOGRAPHIC_CRS;

  /**
   * In absence of a geographic crs we can also use cartesian: GENERIC_2D
   */
  public static final CoordinateReferenceSystem CARTESIANCRS = PlanitOpenGisUtils.CARTESIANCRS;

  /** the crs to use */
  private final CoordinateReferenceSystem crs;

  /** jts geometry factory, jts geometry differs from opengis implementation by not carrying the crs and being more lightweight */
  private static final GeometryFactory jtsGeometryFactory = JTSFactoryFinder.getGeometryFactory();

  /**
   * Constructor
   * 
   * Uses default coordinate reference system
   */
  public PlanitJtsUtils() {
    this(DEFAULT_GEOGRAPHIC_CRS);
  }

  /**
   * Constructor
   * 
   * @param coordinateReferenceSystem OpenGIS CoordinateReferenceSystem object containing geometry
   */
  public PlanitJtsUtils(CoordinateReferenceSystem coordinateReferenceSystem) {
    this.crs = coordinateReferenceSystem;
  }

  /**
   * Compute the distance in metres between two (JTS) points assuming the positions are provided in the same crs as registered on this class instance
   * 
   * @param startPosition location of the start point
   * @param endPosition   location of the end point
   * @return distance in metres between the points
   * @throws PlanItException thrown if there is an error
   */
  public double getDistanceInMetres(Point startPosition, Point endPosition) throws PlanItException {
    return getDistanceInMetres(startPosition.getCoordinate(), endPosition.getCoordinate());
  }

  /**
   * Compute the distance in metres between two (JTS) coordinates assuming the positions are provided in the same crs as registered on this class instance
   * 
   * @param startCoordinate location of the start point
   * @param endCoordinate   location of the end point
   * @return distance in metres between the points
   * @throws PlanItException thrown if there is an error
   */
  public double getDistanceInMetres(Coordinate startCoordinate, Coordinate endCoordinate) throws PlanItException {
    try {
      if (crs.equals(CARTESIANCRS)) {
        // cartesian in meters
        double deltaCoordinate0 = startCoordinate.x - endCoordinate.x;
        double deltaCoordinate1 = startCoordinate.y - endCoordinate.y;
        return Math.sqrt(Math.pow(deltaCoordinate0, 2) + Math.pow(deltaCoordinate1, 2));
      } else {
        return JTS.orthodromicDistance(startCoordinate, endCoordinate, crs);
      }
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when computing distance in meters between two Positions in JtsUtils", e);
    }
  }

  /**
   * Compute the distance in kilometres between two positions assuming the positions are provided in the same crs as registered on this class instance
   * 
   * @param startPosition location of the start point
   * @param endPosition   location of the end point
   * @return distance in kilometres between the points
   * @throws PlanItException thrown if there is an error
   */
  public double getDistanceInKilometres(Point startPosition, Point endPosition) throws PlanItException {
    return getDistanceInMetres(startPosition, endPosition) / 1000.0;
  }

  /**
   * Compute the distance in kilometres between two vertices assuming the positions are set and based on the same crs as registered on this class instance
   * 
   * @param vertex1 vertex with location
   * @param vertex2 vertex with location
   * @return distance in kilometres between the points
   * @throws PlanItException thrown if there is an error
   */
  public double getDistanceInKilometres(Vertex vertex1, Vertex vertex2) throws PlanItException {
    return getDistanceInKilometres(vertex1.getPosition(), vertex2.getPosition());
  }

  /**
   * create a coordinate by mapping ordinate 0 to x and ordinate 1 to y on the open gis DirecPosition
   * 
   * @param position in opengis format
   * @return JTS coordinate created
   */
  public static Coordinate createCoordinate(DirectPosition position) {
    return new Coordinate(position.getOrdinate(0), position.getOrdinate(1));
  }

  /**
   * Create JTS point object from X- and Y-coordinates
   * 
   * @param xCoordinate X-coordinate (longitude assumed)
   * @param yCoordinate Y-coordinate (latitude assumed)
   * @return point object representing the location
   * @throws PlanItException thrown if there is an error during processing
   */
  public static Point createPoint(double xCoordinate, double yCoordinate) throws PlanItException {
    Coordinate coordinate = new Coordinate(xCoordinate, yCoordinate);
    Point newPoint = jtsGeometryFactory.createPoint(coordinate);
    return newPoint;
  }

  /**
   * Convert an open gis line string object to a JTS Gis LineString instance by copying the internal coordinates
   * 
   * @param openGisLineString to convert
   * @return jtsLineString created
   * @throws PlanItException thrown if there is an error
   */
  public static LineString convertToJtsLineString(org.opengis.geometry.coordinate.LineString openGisLineString) throws PlanItException {
    PointArray samplePoints = openGisLineString.getSamplePoints();
    List<Coordinate> coordinates = samplePoints.stream().map(point -> createCoordinate(point.getDirectPosition())).collect(Collectors.toList());
    return jtsGeometryFactory.createLineString((Coordinate[]) coordinates.toArray());
  }

  /**
   * Cast a JTS MultiLineString with a single entry into a JTS LineString instance if valid
   * 
   * @param jtsMultiLineString JTS MultiLineString input object
   * @return jts LineString output object
   * @throws PlanItException thrown if there is an error in casting
   */
  public static LineString convertToLineString(MultiLineString jtsMultiLineString) throws PlanItException {
    PlanItException.throwIf(((MultiLineString) jtsMultiLineString).getNumGeometries() > 1, "MultiLineString contains multiple LineStrings");
    return (LineString) jtsMultiLineString.getGeometryN(0);
  }

  /**
   * Create a JTS line string from the doubles passed in (list of doubles containing x1,y1,x2,y2,etc. coordinates
   * 
   * @param coordinateList source
   * @return created line string
   * @throws PlanItException thrown if error
   */
  public static LineString createLineString(List<Double> coordinateList) throws PlanItException {
    PlanItException.throwIf(coordinateList.size() % 2 != 0, "coordinate list must contain an even number of entries to correctly identify (x,y) pairs");
    Iterator<Double> iter = coordinateList.iterator();
    Coordinate[] coordinateArray = new Coordinate[coordinateList.size() / 2];
    int index = 0;
    while (iter.hasNext()) {
      coordinateArray[index++] = new Coordinate(iter.next(), iter.next());
    }
    return createLineStringFromCoordinates(coordinateArray);
  }

  /**
   * Based on the csv string construct a JTS line string
   * 
   * @param value the values containing the x,y coordinates in the crs of this instance
   * @param ts    tuple separating character
   * @param cs    comma separating character
   * @return the LineString created from the String
   * @throws PlanItException thrown if error
   */
  public static LineString createLineString(String value, char ts, char cs) throws PlanItException {
    List<Double> coordinateDoubleList = new ArrayList<Double>();
    String[] tupleString = value.split("[" + ts + "]");
    for (int index = 0; index < tupleString.length; ++index) {
      String xyCoordinateString = tupleString[index];
      String[] coordinateString = xyCoordinateString.split("[" + cs + "]");
      if (coordinateString.length != 2) {
        throw new PlanItException(String.format("invalid coordinate encountered, expected two coordinates in tuple, but found %d", coordinateString.length));
      }
      coordinateDoubleList.add(Double.parseDouble(coordinateString[0]));
      coordinateDoubleList.add(Double.parseDouble(coordinateString[1]));
    }
    return createLineString(coordinateDoubleList);
  }

  /**
   * Create a line string from the passed in coordinates
   * 
   * @param coordinates source
   * @return created line string
   * @throws PlanItException thrown if error
   */
  public static LineString createLineStringFromCoordinates(Coordinate[] coordinates) throws PlanItException {
    return jtsGeometryFactory.createLineString(coordinates);
  }

  /**
   * Based on the csv string construct a line string
   * 
   * @param value the values containing the x,y coordinates in the crs of this instance
   * @param ts    tuple separating string (which must be a a character)
   * @param cs    comma separating string (which must be a a character)
   * @return the LineString created from the String
   * @throws PlanItException thrown if error
   */
  public static LineString createLineStringFromCsvString(String value, String ts, String cs) throws PlanItException {
    if (ts.length() > 1 || cs.length() > 1) {
      PlanItException.throwIf(ts.length() > 1, String.format("tuple separating string to create LineString is not a single character but %s", ts));
      PlanItException.throwIf(cs.length() > 1, String.format("comma separating string to create LineString is not a single character but %s", cs));
    }
    return createLineString(value, ts.charAt(0), cs.charAt(0));
  }

  /**
   * Based on the line string construct a csv string
   * 
   * @param geometry the values containing the x,y coordinates in the crs of this instance
   * @param ts       tuple separating string to use
   * @param cs       comma separating string to use
   * @param df       decinal formatter to format the decimals of the coordinates
   * @return the LineString created from the String
   */
  public static String createCsvStringFromLineString(LineString geometry, Character ts, Character cs, DecimalFormat df) {
    Coordinate[] coordinates = geometry.getCoordinates();
    StringBuilder csvStringBuilder = new StringBuilder();
    for (int index = 0, lastIndex = coordinates.length - 1; index < coordinates.length; ++index) {
      Coordinate coordinate = coordinates[index];
      csvStringBuilder.append(df.format(coordinate.x)).append(cs).append(df.format(coordinate.y));
      if (index == lastIndex) {
        break;
      }
      csvStringBuilder.append(ts);
    }
    return csvStringBuilder.toString();
  }

  /**
   * Create an empty polygon geometry
   * 
   * @return polygon
   */
  public static Polygon createPolygon() {
    Polygon polygon = jtsGeometryFactory.createPolygon();
    return polygon;
  }

  /**
   * create a polygon based on the passed in 2d list of doubles
   * 
   * @param coordinateList2D to use
   * @return created polygon
   */
  public static Polygon create2DPolygon(List<Double> coordinateList2D) {
    return jtsGeometryFactory.createPolygon(listTo2DCoordinates(coordinateList2D));
  }

  /**
   * create a polygon based on the passed coordinate array
   * 
   * @param coords to use
   * @return created polygon
   */
  public static Polygon createPolygon(Coordinate[] coords) {
    return jtsGeometryFactory.createPolygon(jtsGeometryFactory.createLinearRing(coords));
  }

  /**
   * Convert OpenGIS directPosition to JTS coordinates
   * 
   * @param positions List of GeoTools Position objects
   * @return coordinates array of JTS Coordinate objects
   * @throws PlanItException thrown if there is an error
   */
  public static Coordinate[] directPositionsToCoordinates(List<DirectPosition> positions) throws PlanItException {
    Coordinate[] coordinates = new Coordinate[positions.size()];
    for (int index = 0; index < coordinates.length; ++index) {
      coordinates[index] = createCoordinate(positions.get(index));
    }
    return coordinates;
  }

  /**
   * Create an array of coordinates based on a list of some type that can be interpreted as strings and converted to doubles.
   * 
   * @param posList to extract coordinates from
   * @return coordinates array
   */
  public static Coordinate[] listTo2DCoordinates(List<?> posList) {
    int dimensions = 2;
    /* we parse as is, so we do not check for correct axis order (long/lat or lat/long for example) */
    Coordinate[] coordinates = new Coordinate[posList.size() / dimensions];

    int index = 0;
    while (index + dimensions - 1 < posList.size()) {
      coordinates[index / dimensions] = new Coordinate(Double.parseDouble(posList.get(index).toString()), Double.parseDouble(posList.get(index + 1).toString()));
      index += dimensions;
    }
    return coordinates;
  }

  /**
   * Compute the length of the line string by traversing all nodes and computing the segment by segment distances TODO: find out if a faster way is possible
   * 
   * @param geometry to extract length from
   * @return length in km
   * @throws PlanItException thrown if error
   */
  public double getDistanceInKilometres(LineString geometry) throws PlanItException {
    Coordinate[] coordinates = geometry.getCoordinates();
    int numberOfCoords = coordinates.length;

    if (numberOfCoords > 1) {

      double computedLengthInMetres = 0;
      Coordinate previousCoordinate = coordinates[0];
      for (int index = 1; index < numberOfCoords; ++index) {
        Coordinate currentCoordinate = coordinates[index];
        computedLengthInMetres += getDistanceInMetres(previousCoordinate, currentCoordinate);
        previousCoordinate = currentCoordinate;
      }

      return computedLengthInMetres / 1000.0;
    }
    throw new PlanItException("unable to compute distance for less than two points");
  }

  /**
   * Remove all coordinates in the line string up to but not including the first occurrence of the passed in position. In case the position cannot be found, an exception will be
   * thrown
   * 
   * @param position to use
   * @param geometry linestring
   * @return the line string created
   * @throws PlanItException thrown if position could not be located
   */
  public static LineString createCopyWithoutCoordinatesBefore(Point position, LineString geometry) throws PlanItException {
    Optional<Integer> offset = findFirstCoordinatePosition(position.getCoordinate(), geometry);

    if (!offset.isPresent()) {
      throw new PlanItException(String.format("point (%s) does not exist on line string (%s), unable to create copy from this location", position.toString(), geometry.toString()));
    }

    Coordinate[] coordinates = copyCoordinatesFrom(offset.get(), geometry);

    return createLineStringFromCoordinates(coordinates);
  }

  /**
   * Remove all coordinates in the line string up to but not including the passed in index.
   * 
   * @param startIndex start index
   * @param geometry   to apply to
   * @return the line string created
   * @throws PlanItException thrown if error
   */
  public static LineString createCopyWithoutCoordinatesBefore(int startIndex, LineString geometry) throws PlanItException {
    if (startIndex >= geometry.getNumPoints() || startIndex < 0) {
      throw new PlanItException("invalid start index for extracting coordinates from line string geometry");
    }
    return createLineStringFromCoordinates(copyCoordinatesFrom(startIndex, geometry));
  }

  /**
   * Remove all coordinates in the line string after but not including the passed in position. In case the position cannot be found, an exception will be thrown
   * 
   * @param position first location of this position in geometry is the last entry in the copied geometry
   * @param geometry linestring
   * @return copy of the line string without indicated coordinates
   * @throws PlanItException thrown if position could not be located
   */
  public static LineString createCopyWithoutCoordinatesAfter(Point position, LineString geometry) throws PlanItException {
    Optional<Integer> offset = findFirstCoordinatePosition(position.getCoordinate(), geometry);

    if (!offset.isPresent()) {
      throw new PlanItException(String.format("point (%s) does not exist on line string %s, unable to create copy from this location", position.toString(), geometry.toString()));
    }

    Coordinate[] coordinates = copyCoordinatesUpToNotIncluding(offset.get() + 1, geometry);
    return createLineStringFromCoordinates(coordinates);
  }

  /**
   * Remove all coordinates in the line string after but not including the passed in index.
   * 
   * @param endIndex last index to keep, after is removed
   * @param geometry to apply to
   * @return the line string created
   * @throws PlanItException thrown if error
   */
  public static LineString createCopyWithoutCoordinatesAfter(int endIndex, LineString geometry) throws PlanItException {
    if (geometry == null) {
      return null;
    }

    if (endIndex >= geometry.getNumPoints() || endIndex < 0) {
      throw new PlanItException("invalid end index for extracting coordinates from line string geometry");
    }
    return createLineStringFromCoordinates(copyCoordinatesUpToNotIncluding(endIndex + 1, geometry));
  }

  /**
   * create an identical copy, except that any adjacent duplicate coordinates in the line string are removed
   * 
   * @param geometry to remove duplicate coordinates from
   * @return geometry copy without duplicates
   */
  public static LineString createCopyWithoutAdjacentDuplicateCoordinates(LineString geometry) {
    if (geometry == null) {
      return null;
    }

    ArrayList<Coordinate> coordinateList = new ArrayList<>(geometry.getNumPoints());
    int numCoordinates = geometry.getNumPoints();
    int index = 0;
    int nextIndex = index + 1;
    for (; index < numCoordinates; ++index, ++nextIndex) {
      Coordinate coordinate = geometry.getCoordinateN(index);
      boolean isAdjacentDuplicate = false;
      if (nextIndex < numCoordinates) {
        Coordinate adjacentCoordinate = geometry.getCoordinateN(index + 1);
        isAdjacentDuplicate = coordinate.equals2D(adjacentCoordinate);
      }
      if (!isAdjacentDuplicate) {
        coordinateList.add(coordinate);
      }
    }
    return jtsGeometryFactory.createLineString(coordinateList.stream().toArray(Coordinate[]::new));
  }

  /**
   * find first position where the coordinate resides on the geometry.
   * 
   * @param coordinateToLocate the one to locate
   * @param offset             start searching from offset position
   * @param geometry           to locate from
   * @return the position if present
   */
  public static Optional<Integer> findFirstCoordinatePosition(Coordinate coordinateToLocate, int offset, LineString geometry) {
    if (geometry == null || coordinateToLocate == null) {
      return Optional.empty();
    }

    int numCoordinates = geometry.getNumPoints();
    for (int index = offset; index < numCoordinates; ++index) {
      Coordinate coordinate = geometry.getCoordinateN(index);
      if (coordinate.equals2D(coordinateToLocate)) {
        return Optional.of(index);
      }
    }
    return Optional.empty();
  }

  /**
   * find first position where the coordinate resides on the geometry.
   * 
   * @param coordinateToLocate the one to locate
   * @param geometry           to locate from
   * @return the position if present
   */
  public static Optional<Integer> findFirstCoordinatePosition(Coordinate coordinateToLocate, LineString geometry) {
    return findFirstCoordinatePosition(coordinateToLocate, 0, geometry);
  }

  /**
   * copy the coordinates in the line string starting at the given offset (included)
   * 
   * @param offset   to start at
   * @param geometry to copy from
   * @return coordinate array, when offset is out of bounds null is returned
   * @throws PlanItException thrown if error
   */
  public static Coordinate[] copyCoordinatesFrom(int offset, LineString geometry) throws PlanItException {
    return copyCoordinatesFromUpToNotIncluding(offset, geometry.getNumPoints(), geometry);
  }

  /**
   * copy the coordinates in the line string until the given location, the location is included
   * 
   * @param untilPoint to stop (not included)
   * @param geometry   to copy from
   * @return coordinate array, when offset is out of bounds null is returned
   * @throws PlanItException thrown if error
   */
  public static Coordinate[] copyCoordinatesUpToNotIncluding(int untilPoint, LineString geometry) throws PlanItException {
    return copyCoordinatesFromUpToNotIncluding(0, untilPoint, geometry);
  }

  /**
   * copy the coordinates in the line string from-to the given locations, the locations are included
   * 
   * @param offset     to start at (included)
   * @param untilPoint to end with (not included)
   * @param geometry   to copy from
   * @return coordinate array, when offset is out of bounds empty coordinate array is returned
   * @throws PlanItException thrown if error
   */
  public static Coordinate[] copyCoordinatesFromUpToNotIncluding(int offset, int untilPoint, LineString geometry) throws PlanItException {
    PlanItException.throwIfNull(geometry, "provided geometry to copy coordinates from is null");

    int numCoordinates = geometry.getNumPoints();
    if (offset > untilPoint || untilPoint > numCoordinates) {
      LOGGER.severe("unable to extract coordinates from line string, offset is larger than final point, and/or final point exceeds number of coordinates in geometry");
    }

    Coordinate[] coordinates = new Coordinate[untilPoint - offset];
    for (int index = offset; index < untilPoint; ++index) {
      Coordinate coordinate = geometry.getCoordinateN(index);
      coordinates[index - offset] = coordinate;
    }

    return coordinates;
  }

  /**
   * concatenate the passed in gemoetries (lines strings) by simply copying all the coorcinates in order and create a new line string from these points
   * 
   * @param geometries to concatenate
   * @return created concatenated linesString
   */
  public static LineString concatenate(LineString... geometries) {
    MultiLineString theMultiLineString = jtsGeometryFactory.createMultiLineString(geometries);
    return jtsGeometryFactory.createLineString(theMultiLineString.getCoordinates());
  }

  /**
   * collect the crs used by this instance of the utils class
   * 
   * @return crs used by instance
   */
  public CoordinateReferenceSystem getCoordinateReferenceSystem() {
    return crs;
  }

}
