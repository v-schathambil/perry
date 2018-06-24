package gov.ca.cwds.idm.service;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.cognitoidp.model.UserType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import liquibase.util.StringUtils;
import org.apache.commons.collections.CollectionUtils;

public class CognitoUtils {

  public static final String PERMISSIONS_ATTR_NAME = "custom:Permission";
  static final String COUNTY_ATTR_NAME = "custom:County";
  static final String PERMISSIONS_DELIMITER = ":";

  private CognitoUtils() {
  }

  public static Optional<AttributeType> getAttribute(UserType cognitoUser, String attrName) {
    List<AttributeType> attributes = cognitoUser.getAttributes();

    if(CollectionUtils.isEmpty(attributes)) {
      return Optional.empty();
    } else {
      return attributes.stream().filter(attr -> attr.getName().equalsIgnoreCase(attrName))
          .findFirst();
    }
  }

  public static String getAttributeValue(UserType cognitoUser, String attributeName) {
    return getAttribute(cognitoUser, attributeName)
        .map(AttributeType::getValue).orElse(null);
  }

  public static String getCountyName(UserType cognitoUser) {
    return getAttributeValue(cognitoUser, COUNTY_ATTR_NAME);
  }

  public static Set<String> getPermissions(UserType cognitoUser) {

    Optional<AttributeType> permissionsAttrOpt = getAttribute(cognitoUser, PERMISSIONS_ATTR_NAME);

    if(! permissionsAttrOpt.isPresent()) {
      return new HashSet<>();
    }

    AttributeType permissionsAttr = permissionsAttrOpt.get();
    String permissionsStr = permissionsAttr.getValue();

    if(StringUtils.isEmpty(permissionsStr)){
      return new HashSet<>();
    }

    return new HashSet<>(Arrays.asList(permissionsStr.split(PERMISSIONS_DELIMITER)));
  }

  public static String getPermissionsAttributeValue(Set<String> permissions) {
    if(CollectionUtils.isNotEmpty(permissions)) {
      return String.join(PERMISSIONS_DELIMITER, permissions);
    } else {
      return "";
    }
  }

  public static AttributeType createPermissionsAttribute(Set<String> permissions) {
    AttributeType permissionsAttr = new AttributeType();
    permissionsAttr.setName(PERMISSIONS_ATTR_NAME);
    permissionsAttr.setValue(getPermissionsAttributeValue(permissions));
    return permissionsAttr;
  }
}