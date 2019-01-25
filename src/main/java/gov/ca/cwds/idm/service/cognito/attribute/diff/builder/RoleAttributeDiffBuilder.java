package gov.ca.cwds.idm.service.cognito.attribute.diff.builder;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.idm.service.cognito.attribute.CustomUserAttribute;
import gov.ca.cwds.idm.service.cognito.attribute.diff.RolesUserAttributeDiff;
import gov.ca.cwds.idm.service.cognito.attribute.diff.UserAttributeDiff;
import java.util.Set;

/**
 * Created by Alexander Serbin on 1/15/2019
 */
public class RoleAttributeDiffBuilder extends CollectionAttributeDiffBuilder {

  public RoleAttributeDiffBuilder(UserType userType, Set<String> newValue) {
    super(CustomUserAttribute.ROLES, userType, newValue);
  }

  @Override
  public UserAttributeDiff<Set<String>> buildDiff() {
    return new RolesUserAttributeDiff(getUserType(), getNewValue());
  }

}
