package com.apple.wwrc.service.customer.exception;

import com.apple.wwrc.foundation.framework.exception.FrameworkException;

public class InvalidInputException extends FrameworkException {
    private static final long serialVersionUID = -7046513378881290019L;

    public InvalidInputException(String errorMessage) {
        super(errorMessage);
    }
}
