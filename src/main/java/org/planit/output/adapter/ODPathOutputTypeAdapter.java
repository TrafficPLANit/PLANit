package org.planit.output.adapter;

import org.planit.od.odpath.ODPath;
import org.planit.od.odpath.ODPathIterator;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

public interface ODPathOutputTypeAdapter extends OutputTypeAdapter {

   /**
    * Retrieve an OD path object for a specified mode
    * 
    * @param mode the specified mode
    * @return the OD path object
    */
	public ODPath getODPath(Mode mode);
	
   /**
    * Returns the specified output property values for the current cell in the ODPathIterator
    * 
    * @param outputProperty the specified output property
    * @param odPathIterator the iterator through the current ODPath object
    * @param mode the current mode
    * @param timePeriod the current time period
    * @return the value of the specified property (or an Exception if an error has occurred)
    */
	public Object getODPathOutputPropertyValue(OutputProperty outputProperty, ODPathIterator odPathIterator, Mode mode, TimePeriod timePeriod);
}