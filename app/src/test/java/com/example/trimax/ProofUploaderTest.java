package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Rules of the drop-off photo queue (what is retried, what the server is sent). */
public class ProofUploaderTest {

    @Test
    public void fileNameFitsTheServerRule() {
        String name = ProofUploader.objectName(12, 1729850000000L, "ab12cd34-0000-4000-8000-000000000000");
        assertEquals("proof_12_1729850000000_ab12cd34.jpg", name);
        // public.trimax_driver_attach_proof: <uid>/[A-Za-z0-9_.-]{1,120}\.jpg
        assertTrue(name.matches("[A-Za-z0-9_.-]{1,120}\\.jpg"));
        assertTrue(ProofUploader.objectName(5, 1L, "--").matches("[A-Za-z0-9_.-]{1,120}\\.jpg"));
    }

    @Test
    public void takenAtIsUtcIso() {
        assertEquals("1970-01-01T00:00:00Z", ProofUploader.iso(0));
        assertEquals("2024-10-25T10:13:20Z", ProofUploader.iso(1729851200000L));
    }

    @Test
    public void offlineOrServerNotReadyIsAlwaysRetried() {
        assertEquals(ProofUploader.Next.RETRY, ProofUploader.whatNext(ApiException.network()));
        assertEquals(ProofUploader.Next.RETRY, ProofUploader.whatNext(ApiException.timeout()));
        assertEquals(ProofUploader.Next.RETRY, ProofUploader.whatNext(new ApiException(ApiException.Kind.SERVER, "not set up")));
        assertEquals(ProofUploader.Next.RETRY, ProofUploader.whatNext(ApiException.sessionExpired()));
    }

    @Test
    public void storageRefusalIsRetriedAFewTimesAndRuleErrorsStop() {
        assertEquals(ProofUploader.Next.RETRY_COUNTED, ProofUploader.whatNext(new ApiException(ApiException.Kind.FORBIDDEN, "no")));
        assertEquals(ProofUploader.Next.DROP, ProofUploader.whatNext(new ApiException(ApiException.Kind.NOT_FOUND, "not yours")));
        assertEquals(ProofUploader.Next.DROP, ProofUploader.whatNext(new ApiException(ApiException.Kind.CONFLICT, "5 photos")));
        assertEquals(ProofUploader.Next.DROP, ProofUploader.whatNext(new ApiException(ApiException.Kind.INVALID, "bad")));
    }
}
