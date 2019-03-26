package org.planit.readers;

import java.io.IOException;

import org.planit.configuration.Configuration;

public interface ConfigurationReader {

	public ConfigurationReader setConfigurationReaderLocation(String configurationReaderLocation);
	
	public Configuration getConfiguration() throws IOException; 
	
}
