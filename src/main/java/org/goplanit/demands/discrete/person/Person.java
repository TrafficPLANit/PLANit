package org.goplanit.demands.discrete.person;

import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.tour.ActivitySchedule;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;

import java.util.logging.Logger;

/**
 * Represents a person.
 * 
 * @author markr
 *
 */
public class Person extends ExternalIdAbleImpl implements ManagedId {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(Person.class.getCanonicalName());

  /** household the person resides in */
  private Household household;

  /** the top-level schedule for this person, containing tours (and/or nested sub-tours), or trips, or both in
   * sequential chronological order */
  private ActivitySchedule activitySchedule;

  /** the purpose that carried over from the pre-modelled period that the person is "conducting" before any new
   * activities are undertaken */
  private String initialPurpose;

  /** age in whole years, null when not known */
  private Integer age;

  /** sex, null when not known */
  private Sex sex;

  /** employment status, null when not known */
  private EmploymentStatus employmentStatus;

  /** study status, null when not known */
  private StudyStatus studyStatus;

  /** whether the person holds a licence to drive a car, null when not known */
  private Boolean carLicence;

  /**
   * Generate id for instances of this class based on the token and class identifier
   *
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, Person.PERSON_ID_CLASS);
  }

  /** id class for generating ids */
  public static final Class<Person> PERSON_ID_CLASS = Person.class;

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends Person> getIdClass() {
    return PERSON_ID_CLASS;
  }

  /**
   * Constructor
   *
   * @param groupId          contiguous id generation within this group for instances of this class
   */
  public Person(IdGroupingToken groupId) {
    super(IdGenerator.generateId(groupId, PERSON_ID_CLASS));
    this.activitySchedule = new ActivitySchedule();
  }

  /**
   * Copy constructor
   *
   * @param person to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public Person(Person person, boolean deepCopy /* no impact yet */) {
    super(person);
    this.household = person.household;
    this.activitySchedule = deepCopy ? person.activitySchedule.deepClone() : person.activitySchedule.shallowClone();
    this.initialPurpose = person.initialPurpose;
    this.age = person.age;
    this.sex = person.sex;
    this.employmentStatus = person.employmentStatus;
    this.studyStatus = person.studyStatus;
    this.carLicence = person.carLicence;
  }

  /**
   * Access to household person resides in
   * @return household
   */
  public Household getHousehold() {
    return household;
  }

  /**
   * household person resides in
   * @param household to set
   */
  public void setHousehold(Household household) {
    this.household = household;
  }

  /**
   * Schedule
   * @return the schedule
   */
  public ActivitySchedule getSchedule() {
    return activitySchedule;
  }

  /**
   * Schedule to use
   * @param activitySchedule to set
   */
  public void setSchedule(ActivitySchedule activitySchedule) {
    this.activitySchedule = activitySchedule;
  }

  /**
   * Get the value of initialPurpose.
   *
   * @return value of initialPurpose
   */
  public String getInitialPurpose() {
    return initialPurpose;
  }

  /**
   * Set the value of initialPurpose.
   *
   * @param initialPurpose value of initialPurpose
   */
  public void setInitialPurpose(String initialPurpose) {
    this.initialPurpose = initialPurpose;
  }

  /**
   * Age of the person in whole years
   *
   * @return age, null when not known
   */
  public Integer getAge() {
    return age;
  }

  /**
   * Set the age of the person in whole years
   *
   * @param age to set, null when not known
   */
  public void setAge(Integer age) {
    this.age = age;
  }

  /**
   * Sex of the person
   *
   * @return sex, null when not known
   */
  public Sex getSex() {
    return sex;
  }

  /**
   * Set the sex of the person
   *
   * @param sex to set, null when not known
   */
  public void setSex(Sex sex) {
    this.sex = sex;
  }

  /**
   * Whether the person works and how much
   *
   * @return employment status, null when not known
   */
  public EmploymentStatus getEmploymentStatus() {
    return employmentStatus;
  }

  /**
   * Set whether the person works and how much
   *
   * @param employmentStatus to set, null when not known
   */
  public void setEmploymentStatus(EmploymentStatus employmentStatus) {
    this.employmentStatus = employmentStatus;
  }

  /**
   * Whether the person studies and where
   *
   * @return study status, null when not known
   */
  public StudyStatus getStudyStatus() {
    return studyStatus;
  }

  /**
   * Set whether the person studies and where
   *
   * @param studyStatus to set, null when not known
   */
  public void setStudyStatus(StudyStatus studyStatus) {
    this.studyStatus = studyStatus;
  }

  /**
   * Whether the person holds a licence to drive a car
   *
   * @return true when holding one, false when not, null when not known
   */
  public Boolean getCarLicence() {
    return carLicence;
  }

  /**
   * Set whether the person holds a licence to drive a car
   *
   * @param carLicence to set, null when not known
   */
  public void setCarLicence(Boolean carLicence) {
    this.carLicence = carLicence;
  }

  /**
   * Whether the person may drive: true unless known to hold no car licence or known to be younger than the given age
   *
   * @param minimumDrivingAge age from which a person may drive
   * @return false when known not to be allowed to drive, true otherwise
   */
  public boolean mayDrive(int minimumDrivingAge) {
    return !Boolean.FALSE.equals(carLicence) && (age == null || age >= minimumDrivingAge);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Person shallowClone() {
    return new Person(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Person deepClone() {
    return new Person(this, true);
  }

  /**
   * Output this object as a String
   * 
   * @return String containing the value of this
   */
  @Override
  public String toString() {
    return getIdsAsString();
  }

}
