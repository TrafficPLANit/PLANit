package org.planit.geo.utils;

import java.io.File;
import java.io.Reader;

import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.exceptions.PlanItException;

/**
 * Utility class for general GeoJSON tasks
 * 
 * @author markr
 *
 */
public class PlanitGeoJSONUtils {
	
    private static FeatureJSON featureJson = new FeatureJSON();
    
    /**
	 * Parse the coordinate reference system for a given GeoJSON file
	 * @param fileLocation
	 * @return
	 * @throws PlanItException
	 */
	public static CoordinateReferenceSystem parseCoordinateReferenceSystem(File crsFile) throws PlanItException	{	
		CoordinateReferenceSystem parsedCRS = null;
		// read coordinate reference system (otherwise default to WGS84 which is default for GEOJSON)
		try {
			Reader gJSONReader = GeoJSONUtil.toReader(crsFile);
			// CRS is expected to be listed as a property with the prefix: name
			parsedCRS = featureJson.readCRS(gJSONReader);
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
		if (parsedCRS == null) {
			// attempt default WGS84 in case no explicit CRS is present (We assume all data is in the same format)
			parsedCRS = DefaultGeographicCRS.WGS84;			
		}
		return parsedCRS;
	}
	
	/**
	 * Parse geometry feature collection of the file
	 * @param fileLocation
	 * @return FeatureIterator with geometry
	 * @throws PlanItException in case we can't read the file
	 */
	public static FeatureIterator<SimpleFeature> getFeatureIterator(File featurecollectionFile) throws PlanItException {
		try {
			Reader gJsonReader = GeoJSONUtil.toReader(featurecollectionFile);
			return featureJson.streamFeatureCollection(gJsonReader);
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}
		
}
