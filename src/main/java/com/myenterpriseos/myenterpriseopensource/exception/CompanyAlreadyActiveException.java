package com.myenterpriseos.myenterpriseopensource.exception;

public class CompanyAlreadyActiveException extends RuntimeException {
    public CompanyAlreadyActiveException(Long id) {
        super("Company already active with id: " + id);
    }
}
