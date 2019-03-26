package org.planit.userclass.creators;

import java.lang.reflect.Type;

import org.planit.userclass.UserClass;

import com.google.gson.InstanceCreator;

public class UserClassCreator implements InstanceCreator<UserClass> {

	@Override
	public UserClass createInstance(Type type) 
	{
		return new UserClass(0, "", 0, 0);
	}

}
