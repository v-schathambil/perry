package gov.ca.cwds.idm.persistence.ns.entity;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Cacheable
@Table(name = "permission")
public class Permission  implements Serializable {

  private static final long serialVersionUID = -1871160666991025171L;

  @Id
  @NotNull
  @Column(name = "name")
  private String name;

  @NotNull
  @Column(name = "description")
  private String description;

  public Permission() {
    //empty
  }

  public Permission(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Permission)) {
      return false;
    }
    Permission permission = (Permission) o;
    return Objects.equals(getName(), permission.getName()) &&
        Objects.equals(getDescription(), permission.getDescription());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getDescription());
  }
}
