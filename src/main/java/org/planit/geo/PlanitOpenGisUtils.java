package org.planit.geo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.PositionFactory;
import org.opengis.geometry.coordinate.GeometryFactory;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.geometry.coordinate.PointArray;
import org.opengis.geometry.coordinate.Position;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
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
public class PlanitOpenGisUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitOpenGisUtils.class.getCanonicalName());

  /**
   * Default Coordinate Reference System: WGS84
   */
  public static final DefaultGeographicCRS DEFAULT_GEOGRAPHIC_CRS = PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS;

  /**
   * In absence of a geographic crs we can also use cartesian: GENERIC_2D
   */
  public static final CoordinateReferenceSystem CARTESIANCRS = PlanitJtsCrsUtils.CARTESIANCRS;

  /*
   * the geotools gt-epsg-hsql dependency tries to take over the logging and the formatting of the logging. It is initialised whenever {@code CRS.decode} is invoked from some of
   * this class' static methods. Therefore, here we programmatically disable this unwanted behaviour
   */
  static {
    Logger.getLogger("org.hsqldb").setLevel(Level.WARNING);
    System.setProperty("hsqldb.reconfig_logging", "false");
  }

  /**
   * Geodetic calculator to construct distances between points. It is assumed the network CRS is geodetic in nature.
   */
  private GeodeticCalculator geodeticDistanceCalculator;
  private GeometryBuilder geometryBuilder;
  private GeometryFactory geometryFactory;
  private PositionFactory positionFactory;

  /**
   * Constructor
   * 
   * Uses default coordinate reference system
   */
  public PlanitOpenGisUtils() {
    CoordinateReferenceSystem coordinateReferenceSystem = new DefaultGeographicCRS(DEFAULT_GEOGRAPHIC_CRS);
    geometryBuilder = new GeometryBuilder(coordinateReferenceSystem);
    geodeticDistanceCalculator = new GeodeticCalculator(coordinateReferenceSystem);
    geometryFactory = geometryBuilder.getGeometryFactory();
    positionFactory = geometryBuilder.getPositionFactory();
  }

  /**
   * Constructor
   * 
   * @param coordinateReferenceSystem OpenGIS CoordinateReferenceSystem object containing geometry
   */
  public PlanitOpenGisUtils(CoordinateReferenceSystem coordinateReferenceSystem) {
    geometryBuilder = new GeometryBuilder(coordinateReferenceSystem);
    geometryFactory = geometryBuilder.getGeometryFactory();
    positionFactory = geometryBuilder.getPositionFactory();

    // geodetic only works on ellipsoids
    if (!coordinateReferenceSystem.equals(CARTESIANCRS)) {
      geodeticDistanceCalculator = new GeodeticCalculator(coordinateReferenceSystem);
    }
  }

  /**
   * Compute the distance in metres between two positions assuming the positions are provided in the same crs as registered on this class instance
   * 
   * @param startPosition location of the start point
   * @param endPosition   location of the end point
   * @return distance in metres between the points
   * @throws PlanItException thrown if there is an error
   */
  public double getDistanceInMetres(Position startPosition, Position endPosition) throws PlanItException {
    // not threadsafe
    try {
      if (geodeticDistanceCalculator != null) {
        // ellipsoid crs
        geodeticDistanceCalculator.setStartingGeographicPoint(startPosition.getDirectPosition().getOrdinate(0), startPosition.getDirectPosition().getOrdinate(1));
        geodeticDistanceCalculator.setDestinationGeographicPoint(endPosition.getDirectPosition().getOrdinate(0), endPosition.getDirectPosition().getOrdinate(1));
        return geodeticDistanceCalculator.getOrthodromicDistance();
      } else {
        // cartesian in meters
        double deltaCoordinate0 = startPosition.getDirectPosition().getOrdinate(0) - endPosition.getDirectPosition().getOrdinate(0);
        double deltaCoordinate1 = startPosition.getDirectPosition().getOrdinate(1) - endPosition.getDirectPosition().getOrdinate(1);
        double distanceInMeters = Math.sqrt(Math.pow(deltaCoordinate0, 2) + Math.pow(deltaCoordinate1, 2));
        return distanceInMeters;
      }
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when computing distance in meters between two Positions in GeoUtils", e);
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
  public double getDistanceInKilometres(Position startPosition, Position endPosition) throws PlanItException {
    return getDistanceInMetres(startPosition, endPosition) / 1000.0;
  }

  /**
   * Compute the distance in kilometres between two vertices assuming the positions are set and based on the same crs as registered on this class instance
   * 
   * @param vertexA vertex with location
   * @param vertexB vertex with location
   * @return distance in kilometres between the points
   * @throws PlanItException thrown if there is an error
   */
  public double getDistanceInKilometres(Vertex vertexA, Vertex vertexB) throws PlanItException {
    DirectPosition positionA = JTS.toDirectPosition(vertexA.getPosition().getCoordinate(), geometryBuilder.getCoordinateReferenceSystem());
    DirectPosition positionB = JTS.toDirectPosition(vertexB.getPosition().getCoordinate(), geometryBuilder.getCoordinateReferenceSystem());
    return getDistanceInMetres(positionA, positionB) / 1000.0;
  }

  /**
   * Create DirectPosition object from X- and Y-coordinates
   * 
   * @param xCoordinate X-coordinate (longitude assumed)
   * @param yCoordinate Y-coordinate (latitude assumed)
   * @return DirectPosition object representing the location
   * @throws PlanItException thrown if there is an error during processing
   */
  public DirectPosition createDirectPosition(double xCoordinate, double yCoordinate) throws PlanItException {
    Coordinate coordinate = new Coordinate(xCoordinate, yCoordinate);
    DirectPosition newPosition = positionFactory.createDirectPosition(new double[] { coordinate.x, coordinate.y });
    return newPosition;
  }

  /**
   * Convert a JTS line string object to an OpenGis LineString instance by transferring the internal coordinates
   * 
   * @param jtsLineString JTS line string input
   * @return LineString GeoTools line string output object
   * @throws PlanItException thrown if there is an error
   */
  @SuppressWarnings("unchecked")
  public LineString convertToOpenGisLineString(org.locationtech.jts.geom.LineString jtsLineString) throws PlanItException {
    Coordinate[] coordinates = jtsLineString.getCoordinates();
    List<? extends Position> positionList = (List<? extends Position>) convertToDirectPositions(coordinates);
    return geometryFactory.createLineString((List<Position>) positionList);
  }

  /**
   * Converts a JTS MultiLineString with a single entry into an OpenGIS LineString instance
   * 
   * @param jtsMultiLineString JTS MultiLineString input object
   * @return LineString GeoTools MultiLineString output object
   * @throws PlanItException thrown if there is an error
   */
  public LineString convertToOpenGisLineString(MultiLineString jtsMultiLineString) throws PlanItException {
    PlanItException.throwIf(((MultiLineString) jtsMultiLineString).getNumGeometries() > 1, "MultiLineString contains multiple LineStrings");

    return convertToOpenGisLineString((org.locationtech.jts.geom.LineString) ((MultiLineString) jtsMultiLineString).getGeometryN(0));
  }

  /**
   * Create a line string from the doubles passed in (list of doubles containing x1,y1,x2,y2,etc. coordinates
   * 
   * @param coordinateList source
   * @return created line string
   * @throws PlanItException thrown if error
   */
  public LineString createLineString(List<Double> coordinateList) throws PlanItException {
    PlanItException.throwIf(coordinateList.size() % 2 != 0, "coordinate list must contain an even number of entries to correctly identify (x,y) pairs");
    Iterator<Double> iter = coordinateList.iterator();
    List<Position> positionList = new ArrayList<Position>(coordinateList.size() / 2);
    while (iter.hasNext()) {
      positionList.add(createDirectPosition(iter.next(), iter.next()));
    }
    return geometryFactory.createLineString(positionList);
  }

  /**
   * Based on the csv string construct a line string
   * 
   * @param value the values containing the x,y coordinates in the crs of this instance
   * @param ts    tuple separating character
   * @param cs    comma separating character
   * @return the LineString created from the String
   * @throws PlanItException thrown if error
   */
  public LineString createLineString(String value, char ts, char cs) throws PlanItException {
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
   * Create a line string from the passed in positions
   * 
   * @param positionList source
   * @return created line string
   * @throws PlanItException thrown if error
   */
  public LineString createLineStringFromPositions(List<Position> positionList) throws PlanItException {
    return geometryFactory.createLineString(positionList);
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
  public LineString createLineStringFromCsvString(String value, String ts, String cs) throws PlanItException {
    if (ts.length() > 1 || cs.length() > 1) {
      PlanItException.throwIf(ts.length() > 1, String.format("tuple separating string to create LineString is not a single character but %s", ts));
      PlanItException.throwIf(cs.length() > 1, String.format("comma separating string to create LineString is not a single character but %s", cs));
    }
    return createLineString(value, ts.charAt(0), cs.charAt(0));
  }

  /**
   * Convert JTS coordinates to OpenGIS directPositions
   * 
   * @param coordinates array of JTS Coordinate objects
   * @return List of GeoTools Position objects
   * @throws PlanItException thrown if there is an error
   */
  public List<DirectPosition> convertToDirectPositions(Coordinate[] coordinates) throws PlanItException {
    List<DirectPosition> positionList = new ArrayList<DirectPosition>(coordinates.length);
    for (Coordinate coordinate : coordinates) {
      positionList.add(createDirectPosition(coordinate.x, coordinate.y));
    }
    return positionList;
  }

  /**
   * Compute the length of the line string by traversing all nodes and computing the segment by segment distances TODO: find out if a faster way is possible
   * 
   * @param geometry to extract length from
   * @return length in km
   * @throws PlanItException thrown if error
   */
  public double getDistanceInKilometres(LineString geometry) throws PlanItException {

    PointArray pointArray = geometry.getControlPoints();
    int numberOfPoints = pointArray.size();

    if (numberOfPoints > 1) {

      double computedLengthInKm = 0;
      Position previousPoint = pointArray.get(0);
      for (int index = 1; index < numberOfPoints; ++index) {
        Position currentPoint = pointArray.get(index);
        computedLengthInKm += getDistanceInKilometres(previousPoint, currentPoint);
        previousPoint = currentPoint;
      }

      return computedLengthInKm;
    }
    throw new PlanItException("unable to compute distance for less than two points");
  }

  /**
   * Find the closest explicit sample point registered on the line string compared to the passed in position
   * 
   * @param toMatch    position to egt closest to
   * @param lineString to sample ordinates from to check
   * @return closest ordinate (position) on line string to passed in toMatch position
   * @throws PlanItException thrown if error
   */
  public Position getClosestSamplePointOnLineString(Position toMatch, LineString lineString) throws PlanItException {
    if (lineString != null && toMatch != null) {
      double minDistance = Double.POSITIVE_INFINITY;
      Position minDistancePosition = null;
      for (Position samplePoint : lineString.getSamplePoints()) {
        double currDistance = getDistanceInMetres(toMatch, samplePoint);
        if (getDistanceInMetres(toMatch, samplePoint) < minDistance) {
          minDistance = currDistance;
          minDistancePosition = samplePoint;
        }
      }

      return minDistancePosition;
    }
    throw new PlanItException(" closest orindate position to lines tring could not be computed since either the line string or reference position is null");
  }

  /**
   * Convenience method that wraps the CRS.findMathTransform by catching exceptions and producing a planit excepion only as well as allowing for lenient transformer
   * 
   * @param sourceCRS      the source
   * @param destinationCRS the destination
   * @return transformer
   * @throws PlanItException thrown if error
   */
  public static MathTransform findMathTransform(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem destinationCRS) throws PlanItException {
    PlanItException.throwIfNull(sourceCRS, "source coordinate reference system null when creating math transform");
    PlanItException.throwIfNull(destinationCRS, "destination coordinate reference system null when creating math transform");

    try {
      /* allows for some lenience in transformation due to different datums */
      boolean lenient = true;
      return CRS.findMathTransform(sourceCRS, destinationCRS, lenient);
    } catch (Exception e) {
      throw new PlanItException(String.format("error during creation of transformer from CRS %s to CRS %s", sourceCRS.toString(), destinationCRS.toString()), e);
    }

  }

  /**
   * create a coordinate reference system instance based on String representation, e.g. "EPSG:4326" for WGS84", using the underlying geotools hsql authority factory. see also
   * {@code https://docs.geotools.org/latest/userguide/library/referencing/crs.html} on some context on why we include the hsql dependency in the planit build to ensure that the
   * provided crs codes here can actually be transformed into a viable CRS and why it makes sense to provide this simple wrapper method in this utility class
   * <p>
   * always make sure you lookup the CRS via this method as it ensures the logging of PLANit is not messed up by the geotools-HSQL dependency since we programmatically disallow it
   * to overwrite our logging configuration in the static initialiser of this class.
   * </p>
   * 
   * @param code for the CRS
   * @return the created coordinate reference system
   */
  public static CoordinateReferenceSystem createCoordinateReferenceSystem(String code) {
    CoordinateReferenceSystem crs = null;
    if (code != null) {
      try {
        /* decode lookup is performed using the gt hsql database which is loaded as dependency in pom */
        crs = CRS.decode(code);
      } catch (Exception e1) {
        try {
          crs = CRS.decode(code, true);
        } catch (Exception e2) {
          LOGGER.warning(String.format("unable to find coordinate reference system for %s", code));
        }
      }
    }
    return crs;
  }

}
