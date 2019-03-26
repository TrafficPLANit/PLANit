package org.planit.userclass.creators;

import java.lang.reflect.Type;

import org.planit.userclass.TravellerType;

import com.google.gson.InstanceCreator;

public class TravellerTypeCreator implements InstanceCreator<TravellerType> {

	@Override
	public TravellerType createInstance(Type type) {
		return new TravellerType(0, "");
	}

}
