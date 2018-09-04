package gov.ca.cwds.util;

import gov.ca.cwds.UniversalUserToken;
import gov.ca.cwds.data.reissue.model.PerryTokenEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentAuthenticatedUserUtil {
  private CurrentAuthenticatedUserUtil() {}

  private static final String COUNTY_NAME_PARAM = "county_name";

  public static String getCurrentUserCountyName() {
    SecurityContext securityContext = getSecurityContext();
    UniversalUserToken userToken =
        (UniversalUserToken) securityContext.getAuthentication().getPrincipal();
    return (String) userToken.getParameter(COUNTY_NAME_PARAM);
  }

  public static String getSsoToken() {
    SecurityContext securityContext = getSecurityContext();
    return ((PerryTokenEntity) securityContext.getAuthentication().getDetails()).getToken();
  }

  public static String getUserId() {
    SecurityContext securityContext = getSecurityContext();
    return securityContext.getAuthentication().getPrincipal().toString();
  }

  private static SecurityContext getSecurityContext() {
    return SecurityContextHolder.getContext();
  }
}
