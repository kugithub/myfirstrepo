package com.apple.wwrc.service.customer;

public final class Constants {
    public static final String VAR_1 = "DS.PASSWORD";
    public static final String I3_ENV = "I3_ENV";
    public static final String SSO_ENV = "SSO_ENV";
    public static final String POS_APP_ID = "pos.app.id";

    private Constants() {
        // prevent instantiation of the class
        throw new IllegalStateException("Constants is not instantiatable.");
    }
}
