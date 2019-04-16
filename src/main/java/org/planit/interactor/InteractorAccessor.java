package org.planit.interactor;

/**
 * Interactor accessor 
 * 
 * @author gman6028
 *
 */
public interface InteractorAccessor {

 /**
  * Gets the requested accessee class
  * 
  * @return         requested accessee class
  */
	Class<? extends InteractorAccessee> getRequestedAccessee();
	
/**
 * Sets the accessee object
 * 
 * @param accessee        accessee object to be set
 */
	void setAccessee(InteractorAccessee accessee);
	
}
