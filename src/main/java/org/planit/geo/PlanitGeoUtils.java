package org.planit.geo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.factory.epsg.CartesianAuthorityFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.PositionFactory;
import org.opengis.geometry.coordinate.GeometryFactory;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.geometry.coordinate.Position;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;

/**
 * General geotools related utils. Uses geodetic distance when possible. In case the CRS is not based on an ellipsoid (2d plane) it will simply compute the distance between
 * coorindates using Pythagoras with the unit distance in meters, consistent with the {@code CartesianAuthorityFactory.GENERIC_2D}
 * 
 * It is assumed that x coordinate refers to latitude and y coordinate refers to longitude
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
  public static final DefaultGeographicCRS DEFAULT_GEOGRAPHIC_CRS = DefaultGeographicCRS.WGS84;

  public static final CoordinateReferenceSystem CARTESIANCRS = CartesianAuthorityFactory.GENERIC_2D;

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
  private double getDistanceInMetres(Position startPosition, Position endPosition) throws PlanItException {
    // not threadsafe
    try {
      if (geodeticDistanceCalculator != null) {
        // ellipsoid crs
        geodeticDistanceCalculator.setStartingPosition(startPosition);
        geodeticDistanceCalculator.setDestinationPosition(endPosition);
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
    return getDistanceInMetres(vertexA.getCentrePointGeometry(), vertexA.getCentrePointGeometry()) / 1000.0;
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
    PlanItException.throwIf(((MultiLineString) jtsMultiLineString).getNumGeometries() > 1, "MultiLineString contains multiple LineStrings");

    return convertToOpenGisLineString((com.vividsolutions.jts.geom.LineString) ((MultiLineString) jtsMultiLineString).getGeometryN(0));
  }
  
  /**
   * Create a line string from the line string type passed in 
   * @param lineStringType source
   * @return created line string (with zero offset and zero spacing compared to original)
   * @throws PlanItException 
   */
  public LineString createLineStringFromDoubleCoordinateList(List<Double> coordinateList) throws PlanItException {
    PlanItException.throwIf(coordinateList.size()%2!=0, "coordinate list must contain an even number of entries to correctly identify (x,y) pairs");
    Iterator<Double> iter = coordinateList.iterator();
    List<Position> positionList = new ArrayList<Position>(coordinateList.size()/2);
    while(iter.hasNext()) {
      positionList.add(createDirectPosition(iter.next(), iter.next()));
    }
    return geometryFactory.createLineString(positionList);    
  }
  
  /**
   * Based on the csv string construct a line string
   * 
   * @param value the values containing the x,y coordinates in the crs of this instance
   * @param ts tuple separating character
   * @param cs comma separating character
   * @return the LineString created from the String
   * @throws PlanItException 
   */
  public LineString createLineStringFromCsvString(String value, char ts, char cs) throws PlanItException {
    List<Double> coordinateDoubleList = new ArrayList<Double>();
    String[] tupleString = value.split("["+ts+"]");
    for(int index=0; index < tupleString.length ; ++index) {
      String xyCoordinateString = tupleString[index];
      String[] coordinateString = xyCoordinateString.split("["+cs+"]");
      if(coordinateString.length != 2) {
        throw new PlanItException(String.format("invalid coordinate encountered, expected two coordinates in tuple, but found %d",coordinateString.length));
      }
      coordinateDoubleList.add(Double.parseDouble(coordinateString[0]));
      coordinateDoubleList.add(Double.parseDouble(coordinateString[1]));
    }    
    return createLineStringFromDoubleCoordinateList(coordinateDoubleList);
  }  
  
  /**
   * Based on the csv string construct a line string
   * 
   * @param value the values containing the x,y coordinates in the crs of this instance
   * @param ts tuple separating string (which must be a a character)
   * @param cs comma separating string (which must be a a character)
   * @return the LineString created from the String
   * @throws PlanItException 
   */
  public LineString createLineStringFromCsvString(String value, String ts, String cs) throws PlanItException {
    if(ts.length() > 1 || cs.length() > 1) {
      PlanItException.throwIf(ts.length() > 1, String.format("tuple separating string to create LineString is not a single character but %s",ts));
      PlanItException.throwIf(cs.length() > 1, String.format("comma separating string to create LineString is not a single character but %s",cs));
    }
    return createLineStringFromCsvString(value,ts.charAt(0),cs.charAt(0));
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
      positionList.add(createDirectPosition(coordinate.x, coordinate.y));
    }
    return positionList;
  }



}
