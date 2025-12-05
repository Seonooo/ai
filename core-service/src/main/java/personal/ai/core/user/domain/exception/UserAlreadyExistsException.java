package personal.ai.core.user.domain.exception;

import personal.ai.common.exception.BusinessException;
import personal.ai.common.exception.ErrorCode;

public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException(String email) {
        super(ErrorCode.USER_ALREADY_EXISTS, "User already exists with email: " + email);
    }
}
