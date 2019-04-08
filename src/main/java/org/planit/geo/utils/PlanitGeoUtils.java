package org.planit.geo.utils;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.GeometryBuilder;
import org.geotools.referencing.GeodeticCalculator;
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
 * @author markr
 *
 */
public class PlanitGeoUtils {
	
	/**
	 * Geodetic calculator to construct distances between points. It is assumed the network CRS is
	 * geodetic in nature.
	 */
	private GeodeticCalculator geodeticDistanceCalculator;
	private GeometryBuilder geometryBuilder;
	private GeometryFactory geometryFactory;
	private PositionFactory positionFactory;
	
	/**
	 * Collect a GeometryBuilder to create factories based on the available coordinate reference system 
	 * @return GeometryBuilder
	 * @throws PlanItException 
	 */
	
	public PlanitGeoUtils(CoordinateReferenceSystem coordinateReferenceSystem) {
		geometryBuilder = new GeometryBuilder(coordinateReferenceSystem);
		geodeticDistanceCalculator = new GeodeticCalculator(coordinateReferenceSystem);
		geometryFactory = geometryBuilder.getGeometryFactory();
		positionFactory = geometryBuilder.getPositionFactory();
	}

	/**
	 * Compute the distance in meters between two positions in a geodetic coordinate reference system
	 * @param startPosition
	 * @param endPosition
	 * @return distance in meters
	 * @throws PlanItException 
	 */
	public double getDistanceInMeters(Position startPosition, Position endPosition) throws PlanItException {
		// not threadsafe
		try {
			geodeticDistanceCalculator.setStartingPosition(startPosition);
			geodeticDistanceCalculator.setDestinationPosition(endPosition);		
			return geodeticDistanceCalculator.getOrthodromicDistance();
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}

	/**
	 * Convert a JTS line string object to an OpenGis LineString instance by transferring the internal coordinates
	 * @param jtsLineString
	 * @return LineString
	 * @throws PlanItException 
	 */
	public LineString convertToOpenGisLineString(com.vividsolutions.jts.geom.LineString jtsLineString) throws PlanItException {
		Coordinate[] coordinates = jtsLineString.getCoordinates();
		List<Position> positionList = convertToDirectPositions(coordinates);
		return geometryFactory.createLineString(positionList);	
	}
	
	/** Converts a JTS MultiLineString with a single entry into an OpenGIS LineString instance
	 * @param jtsMultiLineString
	 * @return LineString
	 * @throws PlanItException
	 */
	public LineString convertToOpenGisLineString(MultiLineString jtsMultiLineString) throws PlanItException{
		if(((MultiLineString) jtsMultiLineString).getNumGeometries()>1){
			throw new PlanItException("MultiLineString contains multiple LineStrings");	
		}
		return convertToOpenGisLineString((com.vividsolutions.jts.geom.LineString)((MultiLineString)jtsMultiLineString).getGeometryN(0));
	}

	/**
	 * Convert JTS coordinates to OpenGIS directPositions
	 * @param coordinates
	 * @return ArrayList<Position> positionList
	 * @throws PlanItException 
	 */
	public List<Position> convertToDirectPositions(com.vividsolutions.jts.geom.Coordinate[] coordinates) throws PlanItException {
		List<Position> positionList = new ArrayList<Position>(coordinates.length);		
		for (Coordinate coordinate : coordinates) {
			DirectPosition newPosition = positionFactory.createDirectPosition(new double[]{coordinate.x,coordinate.y});
			positionList.add(newPosition);
		}
		return positionList;
	}
	
}
