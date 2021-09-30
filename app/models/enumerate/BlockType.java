package models.enumerate;

public enum BlockType {

  electronic("Elettronico"),
  papery("Cartaceo");
  
  public String description;
  
  BlockType(String description) {
    this.description = description;
  }
  
  public String getDescription() {
    return description;
  }
}
