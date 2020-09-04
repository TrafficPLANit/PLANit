package org.planit.assignment.eltm;

import org.djutils.event.EventType;
import org.planit.assignment.DynamicTrafficAssignment;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.enums.OutputType;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * event based Link Transmission Model implementation (eLTM) for network loading using a capacity constrained (Deterministic) assignment
 *
 * @author markr
 *
 */
public class ELTM extends DynamicTrafficAssignment {

  /** generated UID */
  private static final long serialVersionUID = 994316948946768870L;

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public ELTM(IdGroupingToken groupId) {
    super(groupId);
  }

  @Override
  protected void addRegisteredEventTypeListeners(EventType eventType) {
    // TODO Auto-generated method stub
  }

  @Override
  public OutputTypeAdapter createOutputTypeAdapter(OutputType outputType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void executeEquilibration() throws PlanItException {
    // TODO Auto-generated method stub

  }

  @Override
  public int getIterationIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

}
