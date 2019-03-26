package org.planit.readers;

import java.io.IOException;

import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

public interface NetworkSupplyReader {

	public NetworkSupplyReader setNetworkSupplyReaderLocation(String networkSupplyReaderLocation);
	
	public FeatureIterator<SimpleFeature> getNetworkSupply() throws IOException;
	
}
