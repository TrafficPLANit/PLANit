package org.planit.readers;

import java.io.File;
import java.io.IOException;

import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.planit.geo.utils.PlanitGeoJSONUtils;

public class GeoJsonTrafficAssignmentZonesReader implements TrafficAssignmentZonesReader {

	private String trafficAssignmentZonesReaderLocation;

	public TrafficAssignmentZonesReader setTrafficAssignmentZonesReaderLocation(String trafficAssignmentZonesReaderLocation) {
		this.trafficAssignmentZonesReaderLocation = trafficAssignmentZonesReaderLocation;
		return this;
	}

	public FeatureIterator<SimpleFeature> getTrafficAssignmentZones() throws IOException {
	    File geoJsonFile = new File(trafficAssignmentZonesReaderLocation).getCanonicalFile();			   
	    return PlanitGeoJSONUtils.getFeatureIterator(geoJsonFile);
	}

}


