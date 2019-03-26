package org.planit.readers;

import java.io.File;
import java.io.IOException;

import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.planit.geo.utils.PlanitGeoJSONUtils;

public class GeoJsonNetworkSupplyReader implements NetworkSupplyReader {
	
	private String networkSupplyReaderLocation;

	public NetworkSupplyReader setNetworkSupplyReaderLocation(String networkSupplyReaderLocation) {
		this.networkSupplyReaderLocation = networkSupplyReaderLocation;
		return this;
	}

	public FeatureIterator<SimpleFeature> getNetworkSupply() throws IOException {
	    File geoJsonFile = new File(networkSupplyReaderLocation).getCanonicalFile();			   
	    return PlanitGeoJSONUtils.getFeatureIterator(geoJsonFile); 
	}

}
