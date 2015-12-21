package cnr.sync.dto;

public class PersonDto {

  public int id;
  public String createdAt;
  public String updatedAt;
  public String firstname;
  public String otherNames;
  public String surname;
  public String otherSurnames;
  public String taxCode;
  public String birthDate;
  public String email;
  public String emailCnr;
  public String uidCnr;
  public Address domicile;
  public Address residence;
  public int number;
  public String department;

  protected class Address {
    public String street;
    public String city;
    public String state;
    public int postalcode;
    public String country;
  }
}