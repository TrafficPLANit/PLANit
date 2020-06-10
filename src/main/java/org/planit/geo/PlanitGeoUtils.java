package org.planit.geo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.geometry.GeometryBuilder;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.PositionFactory;
import org.opengis.geometry.coordinate.GeometryFactory;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.geometry.coordinate.Position;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.exceptions.PlanItException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;

/**
 * General geotools related utils
 * 
 * @author markr
 *
 */
public class PlanitGeoUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitGeoUtils.class.getCanonicalName());

  /**
   * Default Coordinate Reference System
   */
  private static final DefaultGeographicCRS DEFAULT_GEOGRAPHIC_CRS = DefaultGeographicCRS.WGS84;

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
  public PlanitGeoUtils() {
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
  public PlanitGeoUtils(CoordinateReferenceSystem coordinateReferenceSystem) {
    geometryBuilder = new GeometryBuilder(coordinateReferenceSystem);
    geodeticDistanceCalculator = new GeodeticCalculator(coordinateReferenceSystem);
    geometryFactory = geometryBuilder.getGeometryFactory();
    positionFactory = geometryBuilder.getPositionFactory();
  }

  /**
   * Compute the distance in metres between two positions in a geodetic coordinate reference system
   * 
   * @param startPosition location of the start point
   * @param endPosition   location of the end point
   * @return distance in metres between the points
   * @throws PlanItException thrown if there is an error
   */
  private double getDistanceInMetres(Position startPosition, Position endPosition) throws PlanItException {
    // not threadsafe
    try {
      geodeticDistanceCalculator.setStartingPosition(startPosition);
      geodeticDistanceCalculator.setDestinationPosition(endPosition);
      return geodeticDistanceCalculator.getOrthodromicDistance();
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when computing distance in meters between two Positions in GeoUtils", e);
    }
  }

  /**
   * Compute the distance in kilometres between two positions in a geodetic coordinate reference system
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
   * Create DirectPosition object from X- and Y-coordinates
   * 
   * @param xCoordinate X-coordinate
   * @param yCoordinate Y-coordinate
   * @return DirectPosition object representing the location
   * @throws PlanItException thrown if there is an error during processing
   */
  public DirectPosition getDirectPositionFromValues(double xCoordinate, double yCoordinate) throws PlanItException {
    Coordinate coordinate = new Coordinate(xCoordinate, yCoordinate);
    Coordinate[] coordinates = { coordinate };
    List<Position> positions = convertToDirectPositions(coordinates);
    return (DirectPosition) positions.get(0);
  }

  /**
   * Convert a JTS line string object to an OpenGis LineString instance by transferring the internal coordinates
   * 
   * @param jtsLineString JTS line string input
   * @return LineString GeoTools line string output object
   * @throws PlanItException thrown if there is an error
   */
  public LineString convertToOpenGisLineString(com.vividsolutions.jts.geom.LineString jtsLineString) throws PlanItException {
    Coordinate[] coordinates = jtsLineString.getCoordinates();
    List<Position> positionList = convertToDirectPositions(coordinates);
    return geometryFactory.createLineString(positionList);
  }

  /**
   * Converts a JTS MultiLineString with a single entry into an OpenGIS LineString instance
   * 
   * @param jtsMultiLineString JTS MultiLineString input object
   * @return LineString GeoTools MultiLineString output object
   * @throws PlanItException thrown if there is an error
   */
  public LineString convertToOpenGisLineString(MultiLineString jtsMultiLineString) throws PlanItException {
    if (((MultiLineString) jtsMultiLineString).getNumGeometries() > 1) {
      String errorMessage = "MultiLineString contains multiple LineStrings";
      throw new PlanItException(errorMessage);
    }
    return convertToOpenGisLineString((com.vividsolutions.jts.geom.LineString) ((MultiLineString) jtsMultiLineString).getGeometryN(0));
  }

  /**
   * Convert JTS coordinates to OpenGIS directPositions
   * 
   * @param coordinates array of JTS Coordinate objects
   * @return List of GeoTools Position objects
   * @throws PlanItException thrown if there is an error
   */
  public List<Position> convertToDirectPositions(com.vividsolutions.jts.geom.Coordinate[] coordinates) throws PlanItException {
    List<Position> positionList = new ArrayList<Position>(coordinates.length);
    for (Coordinate coordinate : coordinates) {
      DirectPosition newPosition = positionFactory.createDirectPosition(new double[] { coordinate.x, coordinate.y });
      positionList.add(newPosition);
    }
    return positionList;
  }

}
