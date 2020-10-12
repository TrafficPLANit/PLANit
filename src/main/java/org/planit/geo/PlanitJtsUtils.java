package org.planit.geo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.factory.epsg.CartesianAuthorityFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.coordinate.PointArray;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;

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
  public static final DefaultGeographicCRS DEFAULT_GEOGRAPHIC_CRS = DefaultGeographicCRS.WGS84;

  /**
   * In absence of a geographic crs we can also use cartesian: GENERIC_2D
   */
  public static final CoordinateReferenceSystem CARTESIANCRS = CartesianAuthorityFactory.GENERIC_2D;

  /** the crs to use */
  private final CoordinateReferenceSystem crs;

  /** jts geometry factory, jts geometry differs from opengis implementation by not carrying the crs and being more lightweight */
  private GeometryFactory jtsGeometryFactory;

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
    jtsGeometryFactory = JTSFactoryFinder.getGeometryFactory();
  }

  COPY ALL
  THE STUFF
  FROM THE
  OPENGIS UTILS
  THAT WE NEED

}
