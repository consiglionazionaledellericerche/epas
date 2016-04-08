package synch.perseoconsumers.people;


public class PerseoPerson {
  public Long id;
  public String firstname;
  public String surname;
  public String email;
  public Integer number;
  public Integer qualification;
  public Integer departmentId;
  public String updatedAt;
  
  public void setId(String id) {
    try {
      this.id = Long.parseLong(id); 
    } catch(Exception e) {}
    this.id = null;
  }
  
  public void setNumber(String number) {
    try {
      this.number = Integer.parseInt(number); 
    } catch(Exception e) {}
    this.number = null;
  }
  
  public void setQualification(String qualification) {
    try {
      this.qualification = Integer.parseInt(qualification); 
    } catch(Exception e) {}
    this.qualification = null;
  }
}
