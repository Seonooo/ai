package personal.ai.core.user.domain.exception;

import personal.ai.common.exception.EntityNotFoundException;
import personal.ai.common.exception.ErrorCode;

/**
 * User Not Found Exception
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
public class UserNotFoundException extends EntityNotFoundException {
    private final Long userId;
    private final String email;

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "User not found with id: " + userId);
        this.userId = userId;
        this.email = null;
    }

    public UserNotFoundException(String email) {
        super(ErrorCode.USER_NOT_FOUND, "User not found with email: " + email);
        this.userId = null;
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
