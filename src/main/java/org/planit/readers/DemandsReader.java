package org.planit.readers;

import java.io.IOException;

import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

public interface DemandsReader {

	public DemandsReader setDemandsReaderLocation(String demandsReaderLocation);
	
	public FeatureIterator<SimpleFeature> getDemands() throws IOException;
	
}
