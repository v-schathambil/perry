package gov.ca.cwds.idm.service.cognito.util;

import gov.ca.cwds.idm.service.cognito.StandardUserAttribute;
import gov.ca.cwds.idm.service.cognito.dto.CognitoUsersSearchCriteria;

public class CognitoUsersSearchCriteriaUtil {

  private CognitoUsersSearchCriteriaUtil() {}

  public static final int DEFAULT_PAGESIZE = 60;

  public static CognitoUsersSearchCriteria composeToGetPage(String paginationToken) {
    return CognitoUsersSearchCriteria.SearchParameterBuilder.aSearchParameters()
            .withPaginationToken(paginationToken)
            .withPageSize(DEFAULT_PAGESIZE)
            .build();
  }

  public static CognitoUsersSearchCriteria composeToGetByRacfid(String racfid) {
    return CognitoUsersSearchCriteria.SearchParameterBuilder.aSearchParameters()
        .withPageSize(DEFAULT_PAGESIZE)
        .withSearchAttr(StandardUserAttribute.RACFID_STANDARD, racfid)
        .build();
  }
}
