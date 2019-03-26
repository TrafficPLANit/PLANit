package org.planit.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.planit.configuration.Configuration;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.userclass.TravellerType;
import org.planit.userclass.UserClass;
import org.planit.userclass.creators.ModeCreator;
import org.planit.userclass.creators.TimePeriodCreator;
import org.planit.userclass.creators.TravellerTypeCreator;
import org.planit.userclass.creators.UserClassCreator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonConfigurationFileReader implements ConfigurationReader {

	private String configurationReaderLocation;

	public Configuration getConfiguration() throws IOException {
		File configurationFile = new File(configurationReaderLocation).getCanonicalFile();
		Reader jsonReader = new BufferedReader(new InputStreamReader(new FileInputStream(configurationFile)));
		Gson gson = new GsonBuilder().registerTypeAdapter(Mode.class, new ModeCreator())
		                             .registerTypeAdapter(TravellerType.class, new TravellerTypeCreator())
				                     .registerTypeAdapter(TimePeriod.class, new TimePeriodCreator())
		                             .registerTypeAdapter(UserClass.class, new UserClassCreator()).create();
		Configuration configuration = gson.fromJson(jsonReader, Configuration.class);

		for (Mode mode : configuration.getModes()) {
			Mode.putById(mode);
		}
		for (TravellerType travellerType : configuration.getTravellerTypes()) {
			TravellerType.putById(travellerType);
		}
		
		List<UserClass> userClasses = new ArrayList<UserClass>();
		for (UserClass userClass : configuration.getUserClasses()) {
			UserClass updatedUserClass = new UserClass(userClass.getId(), userClass.getName(), userClass.getModeId(), userClass.getTravellerTypeId());
			userClasses.add(updatedUserClass);
		}
		configuration.setUserClasses(userClasses);
		
		for (TimePeriod timePeriod : configuration.getTimeperiods()) {
			TimePeriod.putById(timePeriod);
		}

		return configuration;
	}

	@Override
	public ConfigurationReader setConfigurationReaderLocation(String configurationReaderLocation) {
		this.configurationReaderLocation = configurationReaderLocation;
		return this;
	};
}
