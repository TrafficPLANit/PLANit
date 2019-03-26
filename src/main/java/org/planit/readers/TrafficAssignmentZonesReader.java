package org.planit.readers;

import java.io.IOException;

import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

public interface TrafficAssignmentZonesReader {

	public TrafficAssignmentZonesReader setTrafficAssignmentZonesReaderLocation(String trafficAssignmentZonesReaderLocation);
	
	public FeatureIterator<SimpleFeature> getTrafficAssignmentZones() throws IOException;
	
}
