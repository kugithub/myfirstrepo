package com.apple.wwrc.service.customer.resolver;

import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.foundation.framework.service.ServiceFactory;
import com.apple.wwrc.foundation.security.builder.SecurityRequestBuilder;
import com.apple.wwrc.foundation.security.service.EncryptionType;
import com.apple.wwrc.foundation.security.service.SecurityInterface;

public class JamaicaProxy {
    public int getEncryptionType(String cipherText) throws FrameworkException {
        try {
            SecurityInterface remoteService = ServiceFactory.createClient(SecurityInterface.class);
            return remoteService.getEncryptionType(cipherText);
        } catch (Exception re) {
            throw new FrameworkException("Error in Jamaica-Security:" +re.getMessage());//This will allow remote exception to show where it is occurred.
        }
    }
    public String decryptText(String cipherText, int encryptionType) throws FrameworkException {
        try {
            SecurityInterface remoteService = ServiceFactory.createClient(SecurityInterface.class);
            return remoteService.decryptText(SecurityRequestBuilder.getEncryptDecryptText(
                    EncryptionType.typeLookup(encryptionType), null,
                    cipherText, "", "", ""));
        } catch (Exception re) {
            throw new FrameworkException("Error in Jamaica-Security:" +re.getMessage());//This will allow remote exception to show where it is occurred.
        }
    }
}
