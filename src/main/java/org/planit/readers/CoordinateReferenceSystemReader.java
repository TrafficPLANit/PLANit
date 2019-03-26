package org.planit.readers;

import java.io.IOException;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

public interface CoordinateReferenceSystemReader {
	
	public CoordinateReferenceSystemReader setCoordinateReferenceSystemReaderLocation(String coordinateReferenceSystemReaderLocation);
	
	public CoordinateReferenceSystem getCoordinateReferenceSystem() throws IOException;
	
}
