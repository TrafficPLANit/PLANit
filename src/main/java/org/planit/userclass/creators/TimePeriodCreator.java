package org.planit.userclass.creators;

import java.lang.reflect.Type;

import org.planit.time.TimePeriod;

import com.google.gson.InstanceCreator;

public class TimePeriodCreator implements InstanceCreator<TimePeriod> {

	@Override
	public TimePeriod createInstance(Type type) {
		try {
			return new TimePeriod(0, "", "0000", 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}