package org.planit.userclass.creators;

import java.lang.reflect.Type;

import org.planit.userclass.Mode;

import com.google.gson.InstanceCreator;

public class ModeCreator implements InstanceCreator<Mode> {

	@Override
	public Mode createInstance(Type type) {
		return new Mode(0, "", 0.0);
	}

}