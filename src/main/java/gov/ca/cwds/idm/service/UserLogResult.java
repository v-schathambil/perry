package gov.ca.cwds.idm.service;

import gov.ca.cwds.idm.persistence.model.UserLog;
import java.util.Optional;

public class UserLogResult {
  private ResultType resultType = ResultType.WAS_NOT_EXECUTED;
  private UserLog userLog;
  private Exception exception;

  public ResultType getResultType() {
    return resultType;
  }

  public void setResultType(ResultType resultType) {
    this.resultType = resultType;
  }

  public Optional<UserLog> getUserLog() {
    return Optional.ofNullable(userLog);
  }

  public void setUserLog(UserLog userLog) {
    this.userLog = userLog;
  }

  public Optional<Exception> getException() {
    return Optional.ofNullable(exception);
  }

  public void setException(Exception exception) {
    this.exception = exception;
  }
}
