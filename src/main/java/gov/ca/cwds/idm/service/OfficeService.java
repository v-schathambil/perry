package gov.ca.cwds.idm.service;

import static gov.ca.cwds.config.api.idm.Roles.COUNTY_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.STATE_ADMIN;
import static gov.ca.cwds.service.messages.MessageCode.NOT_AUTHORIZED_TO_GET_MANAGED_OFFICES_LIST;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUser;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUserCountyName;

import gov.ca.cwds.UniversalUserToken;
import gov.ca.cwds.idm.dto.Office;
import gov.ca.cwds.idm.persistence.cwscms.repository.OfficeRepository;
import gov.ca.cwds.idm.service.authorization.UserRolesService;
import gov.ca.cwds.service.messages.MessagesService;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("idm")
public class OfficeService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OfficeService.class);

  static final Comparator<Office> OFFICE_NAME_COMPARATOR =
      Comparator.comparing(Office::getOfficeName, Comparator.nullsLast(String::compareToIgnoreCase));

  private OfficeRepository officeRepository;

  private MessagesService messagesService;

  @Transactional(value = "transactionManager")
  public List<Office> getOffices() {
    List<Office> offices = officeRepository.findOffices();
    offices.sort(OFFICE_NAME_COMPARATOR);
    return offices;
  }

  @Transactional(value = "transactionManager")
  public List<Office> getOfficesByAdmin() {
    UniversalUserToken currentUser = getCurrentUser();

    List<Office> offices;

    switch (UserRolesService.getStrongestAdminRole(currentUser)) {
      case STATE_ADMIN:
        offices = officeRepository.findOffices();
        break;
      case OFFICE_ADMIN:
      case COUNTY_ADMIN:
        offices = officeRepository.findCountyOffices(getCurrentUserCountyName());
        break;
      default:
        String msg = messagesService
            .getTechMessage(NOT_AUTHORIZED_TO_GET_MANAGED_OFFICES_LIST,
                currentUser.getUserId(), currentUser.getRoles());
        LOGGER.error(msg);
        throw new AccessDeniedException(msg);
    }

    offices.sort(OFFICE_NAME_COMPARATOR);
    return offices;
  }

  @Autowired
  public void setOfficeRepository(
      OfficeRepository officeRepository) {
    this.officeRepository = officeRepository;
  }

  @Autowired
  public void setMessagesService(MessagesService messagesService) {
    this.messagesService = messagesService;
  }
}
