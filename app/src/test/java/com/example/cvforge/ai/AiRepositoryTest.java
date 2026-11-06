package com.example.cvforge.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;

public class AiRepositoryTest {

    @Test
    public void noInternetIsANetworkError() {
        assertEquals(AiRepository.Kind.NETWORK, AiRepository.classify(new UnknownHostException("no host")));
        assertEquals(AiRepository.Kind.NETWORK, AiRepository.classify(new IOException("timeout")));
    }

    @Test
    public void networkErrorInsideAnotherErrorIsFound() {
        Throwable wrapped = new RuntimeException("Unknown error", new UnknownHostException("no host"));
        assertEquals(AiRepository.Kind.NETWORK, AiRepository.classify(wrapped));
    }

    @Test
    public void limitReachedIsAQuotaError() {
        assertEquals(AiRepository.Kind.QUOTA, AiRepository.classify(new RuntimeException("HTTP 429: quota exceeded")));
        assertEquals(AiRepository.Kind.QUOTA, AiRepository.classify(new RuntimeException("RESOURCE_EXHAUSTED")));
    }

    @Test
    public void missingModelOrPermissionIsAServiceError() {
        assertEquals(AiRepository.Kind.SERVICE,
                AiRepository.classify(new RuntimeException("models/gemini-2.5-flash is not found (404)")));
        assertEquals(AiRepository.Kind.SERVICE, AiRepository.classify(new RuntimeException("403 permission denied")));
    }
}
