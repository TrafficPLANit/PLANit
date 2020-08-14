package org.planit.assignment.eltm.event;

import java.io.Serializable;

import org.djutils.event.Event;
import org.djutils.event.EventType;
import org.planit.utils.cumulatives.CumulativePoint;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * A cumulative event represents a cumulative pcu point in space-time with (potential) additional
 * information.
 * The content of the event is stored in a raw object array which is initialised here. The first two
 * entries
 * are populated with the unique id of the event and the cumulative information.
 * 
 * @author markr
 *
 */
public class CumulativeEvent extends Event {

  /** generate UID */
  private static final long serialVersionUID = 8047592696489714652L;
  
  /**
   * Event ids are generated with unique ids across all cumulative events in the JVM
   * 
   * @return geenerated event id
   */
  protected static long generateEventId() {
    return IdGenerator.generateId(IdGroupingToken.collectGlobalToken(),CumulativeEvent.class);
  }

  /**
   * Collect any entry from the event content object array by index and cast to generic type T
   * 
   * @param <T> content type
   * @param index of the content object array to retrieve
   * @return entry type cast to T
   */
  @SuppressWarnings("unchecked")
  protected <T> T getEventContentByIndex(int index) {
    return (T) ((Object[]) getContent())[index];
  }

  /**
   * set content entry on the event explicitly
   * 
   * @param index location in the content object array
   * @param contentEntry the entry
   */
  protected void setEventContentByIndex(int index, Serializable contentEntry) {
    ((Object[]) getContent())[index] = contentEntry;
  }

  /**
   * Constructor of an LTM cumulative event.
   * 
   * 
   * @param type of the event
   * @param sourceId of the event (where does it originate from)
   * @param cumulativePoint contains the cumulative information
   * @param singleParameter create room in the content array for additional content of derived event
   *          classes
   */
  protected CumulativeEvent(EventType type, Serializable sourceId, CumulativePoint cumulativePoint,
      Serializable singleParameter) {
    super(type, sourceId, new Object[] {generateEventId(), cumulativePoint, singleParameter});
  }

  /**
   * Constructor of an LTM cumulative event.
   * 
   * 
   * @param type of the event
   * @param sourceId of the event (where does it originate from)
   * @param cumulativePoint contains the cumulative information
   * @param additionalContent create room in the content array for additional content of derived
   *          event classes
   */
  protected CumulativeEvent(
      EventType type,
      Serializable sourceId,
      CumulativePoint cumulativePoint,
      Serializable... additionalContent) {
    super(type, sourceId, new Object[additionalContent.length + 2]);
    // place fixed content in object array structure
    setEventContentByIndex(0, generateEventId());
    setEventContentByIndex(1, cumulativePoint);
    // place variable content in object array structure
    int index = 1;
    for (Serializable contentEntry : additionalContent) {
      setEventContentByIndex(++index, contentEntry);
    }
  }

  // getters - setters

  /**
   * @return the unique id
   */
  public long getId() {
    return getEventContentByIndex(0);
  }

  /**
   * @return the cumulative point
   */
  public final CumulativePoint getCumulativePoint() {
    return getEventContentByIndex(1);
  }

}
