package org.planit.output.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.planit.output.Column;
import org.planit.trafficassignment.TrafficAssignment;

/**
 *Adapter providing properties specific to in LinkOutputTypeConfiguration
 *
 * @author gman6028
 *
 */
public abstract class LinkOutputAdapter extends OutputAdapter {

    private static final Logger LOGGER = Logger.getLogger(LinkOutputAdapter.class.getName());

    /**
     * 
     * Columns to be included in the CSV output files
     */
	protected List<Column> columns;
	
	public LinkOutputAdapter(TrafficAssignment trafficAssignment) {
		super(trafficAssignment);
        columns = new ArrayList<Column>();
	}

	public void addColumn(Column column) {
		columns.add(column);
	}

	public List<Column> getColumns() {
		return columns;
	}

}
