package org.planit.readers;

import java.io.File;
import java.io.IOException;

import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.planit.geo.utils.PlanitGeoJSONUtils;

public class GeoJsonDemandsReader implements DemandsReader {

	private String demandsReaderLocation;

	public DemandsReader setDemandsReaderLocation(String demandsReaderLocation) {
		this.demandsReaderLocation = demandsReaderLocation;
		return this;
	}

	public FeatureIterator<SimpleFeature> getDemands() throws IOException {
	    File geoJsonFile = new File(demandsReaderLocation).getCanonicalFile();			   
	    return PlanitGeoJSONUtils.getFeatureIterator(geoJsonFile);
	}

}

