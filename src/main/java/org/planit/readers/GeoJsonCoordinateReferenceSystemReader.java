package org.planit.readers;

import java.io.File;
import java.io.IOException;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.utils.PlanitGeoJSONUtils;

public class GeoJsonCoordinateReferenceSystemReader implements CoordinateReferenceSystemReader {

	private String coordinateReferenceSystemReaderLocation;

	public CoordinateReferenceSystem getCoordinateReferenceSystem() throws IOException {
		File geoJsonFile = new File(coordinateReferenceSystemReaderLocation).getCanonicalFile();
	    return PlanitGeoJSONUtils.parseCoordinateReferenceSystem(geoJsonFile);
	}

	@Override
	public CoordinateReferenceSystemReader setCoordinateReferenceSystemReaderLocation(String coordinateReferenceSystemReaderLocation) {
		this.coordinateReferenceSystemReaderLocation = coordinateReferenceSystemReaderLocation;
		return this;
	}

}
