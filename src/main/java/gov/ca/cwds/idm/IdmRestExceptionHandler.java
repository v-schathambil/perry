package gov.ca.cwds.idm;

import static gov.ca.cwds.idm.IdmResource.DATETIME_FORMAT_PATTERN;
import static gov.ca.cwds.idm.IdmResource.getNewUserLocationUri;
import static gov.ca.cwds.idm.persistence.ns.OperationType.CREATE;
import static gov.ca.cwds.service.messages.MessageCode.INVALID_DATE_FORMAT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import gov.ca.cwds.idm.dto.IdmApiCustomError;
import gov.ca.cwds.idm.dto.IdmApiCustomError.IdmApiCustomErrorBuilder;
import gov.ca.cwds.rest.api.domain.IdmException;
import gov.ca.cwds.rest.api.domain.PartialSuccessException;
import gov.ca.cwds.rest.api.domain.UserAlreadyExistsException;
import gov.ca.cwds.rest.api.domain.UserIdmValidationException;
import gov.ca.cwds.rest.api.domain.UserNotFoundPerryException;
import gov.ca.cwds.service.messages.MessagesService;
import java.net.URI;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Profile("idm")
@ControllerAdvice(assignableTypes = {IdmResource.class})
public class IdmRestExceptionHandler extends ResponseEntityExceptionHandler {

  @Autowired
  private MessagesService messages;

  @ExceptionHandler(value = {UserNotFoundPerryException.class})
  ResponseEntity<Object> handleUserNotFound() {
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(value = {UserAlreadyExistsException.class})
  ResponseEntity<Object> handleUserAlreadyExists(UserAlreadyExistsException e) {
    return buildResponseEntity(HttpStatus.CONFLICT, e);
  }

  @ExceptionHandler(value = {UserIdmValidationException.class})
  ResponseEntity<Object> handleUserValidationException(UserIdmValidationException e) {
    return buildResponseEntity(HttpStatus.BAD_REQUEST, e);
  }

  @ExceptionHandler(value = {PartialSuccessException.class})
  ResponseEntity<Object> handlePartialSuccess(PartialSuccessException e) {

    HttpStatus httpStatus = INTERNAL_SERVER_ERROR;
    List<Exception> causes = e.getCauses();

    if (e.getOperationType() == CREATE) {
      URI locationUri = getNewUserLocationUri(e.getUserId());
      HttpHeaders headers = new HttpHeaders();
      headers.setLocation(locationUri);
      return buildResponseEntity(httpStatus, e, causes, headers);
    } else {
      return buildResponseEntity(httpStatus, e, causes);
    }
  }

  @ExceptionHandler(value = {DateTimeParseException.class})
  ResponseEntity<Object> handleDateTimeParseException(DateTimeParseException e) {
    String msg = messages.getTechMessage(INVALID_DATE_FORMAT, DATETIME_FORMAT_PATTERN);
    String userMessage = messages.getUserMessage(INVALID_DATE_FORMAT, DATETIME_FORMAT_PATTERN);
    logger.error(msg, e);
    HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
    IdmApiCustomError apiError =
        IdmApiCustomError.IdmApiCustomErrorBuilder.anIdmApiCustomError()
            .withStatus(httpStatus)
            .withErrorCode(INVALID_DATE_FORMAT)
            .withTechnicalMessage(msg)
            .withUserMessage(userMessage)
            .build();
    return new ResponseEntity<>(apiError, httpStatus);
  }

  private ResponseEntity<Object> buildResponseEntity(HttpStatus httpStatus, IdmException e) {
    return new ResponseEntity<>(buildApiCustomError(httpStatus, e), httpStatus);
  }

  private ResponseEntity<Object> buildResponseEntity(HttpStatus httpStatus, IdmException e,
      List<Exception> causes) {
    return new ResponseEntity<>(buildApiCustomError(httpStatus, e, causes), httpStatus);
  }

  private ResponseEntity<Object> buildResponseEntity(HttpStatus httpStatus, IdmException e,
      List<Exception> causes, HttpHeaders headers) {
    return new ResponseEntity<>(buildApiCustomError(httpStatus, e, causes), headers, httpStatus);
  }

  private IdmApiCustomError buildApiCustomError(HttpStatus httpStatus, IdmException e) {
    return createErrorBuilder(e, httpStatus).build();
  }

  private IdmApiCustomError buildApiCustomError(HttpStatus httpStatus, IdmException e,
      List<Exception> causes) {
    return createErrorBuilder(e, httpStatus).withCauses(causes).build();
  }

  private IdmApiCustomErrorBuilder createErrorBuilder(IdmException e, HttpStatus httpStatus) {
    return IdmApiCustomErrorBuilder.anIdmApiCustomError()
        .withStatus(httpStatus)
        .withErrorCode(e.getErrorCode())
        .withTechnicalMessage(e.getMessage())
        .withUserMessage(e.getUserMessage())
        .withCause(e.getCause());
  }
}
