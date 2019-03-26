package org.planit.geo.utils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.GeoJSONUtil;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
	 * @throws IOException
	 */
	public static CoordinateReferenceSystem parseCoordinateReferenceSystem(File crsFile) throws IOException	{	
		CoordinateReferenceSystem parsedCRS = null;
		Reader gJSONReader = GeoJSONUtil.toReader(crsFile);
		// read coordinate reference system (otherwise default to WGS84 which is default for GEOJSON)
		try {
			// CRS is expected to be listed as a property with the prefix: name
			parsedCRS = featureJson.readCRS(gJSONReader);
		} catch (IOException e) {
			// TODO: add logging 
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
	 * @throws IOException in case we can't read the file
	 */
	public static FeatureIterator<SimpleFeature> getFeatureIterator(File featurecollectionFile) throws IOException	{
		Reader gJsonReader = GeoJSONUtil.toReader(featurecollectionFile);
		return featureJson.streamFeatureCollection(gJsonReader);
	}
		
}
