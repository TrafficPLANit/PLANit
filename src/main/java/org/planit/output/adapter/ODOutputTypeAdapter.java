package org.planit.output.adapter;

import java.util.Set;

import org.planit.od.odmatrix.skim.ODSkimMatrix;
import org.planit.output.enums.ODSkimOutputType;
import org.planit.userclass.Mode;

/**
 * Interface defining the methods required for an Origin-Destination output adapter
 * 
 * @author gman6028
 *
 */
public interface ODOutputTypeAdapter extends OutputTypeAdapter {

	/**
	 * Returns a Set of OD skim output types which have been activated
	 * 
	 * @return Set of OD skim output types which have been activated
	 */
    public Set<ODSkimOutputType> getActiveSkimOutputTypes();

    /**
     * Retrieve an OD skim matrix for a specified OD skim output type and mode
     * 
     * @param odSkimOutputType the specified OD skim output type
     * @param mode the specified mode
     * @return the OD skim matrix
     */
    public  ODSkimMatrix getODSkimMatrix(ODSkimOutputType odSkimOutputType, Mode mode);
}
