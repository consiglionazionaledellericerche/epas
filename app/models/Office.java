package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
 
 
@Entity
@Table(name = "office")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
    name="discriminator",
    discriminatorType=DiscriminatorType.STRING
)
@DiscriminatorValue(value="O")
public class Office {
 
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;
     
    @Column(name = "name")
    private String name;
     
    @Column(name = "address")
    private String address;
    
    @Column(name = "code")
    private Integer code;
     
    @OneToMany(mappedBy="id", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
    public List<Person> persons = new ArrayList<Person>();
}