package keyczar;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

public class SignerTest {
  private static final String TEST_DATA = "./testdata";
  private String input = "This is some test input";
  private byte[] inputBytes = input.getBytes();
  
  // This is a signature on 'input' by the primary key (version 2)
  private String hmacPrimarySig = "Ab1PQ5IXUGHSnqcRGCLopEwt8KTdj+qw6g==";

  // This is a signature on 'input' by an active key (version 1)
  private String hmacActiveSig = "AeM3GRZPlBcQRB/gJo49PLN0BwCWQ7X2rA==";

  private String dsaPrimarySig = 
    "AUiZdiAwLAIUeMQRssGRPUAMC1JmzqepUg2gl2wCFCGL6sii5pXPhF+DhhUz7IB0kZJf";

  private String dsaActiveSig = 
    "AQlauKIwLQIVAIx1iicAQ0D2QWt7gJryY9YOx/LAAhRCoB8GuNACGjygWoE8KtD0tYOzpA==";
  @Test
  public final void testHmacSignAndVerify() throws KeyczarException {
    Signer hmacSigner = new Signer(TEST_DATA + "/hmac");
    String sig = hmacSigner.sign(input);
    assertTrue(hmacSigner.verify(input, sig));
    System.out.println("Hmac Sig: " + sig);
    // Try signing and verifying directly in a buffer
    ByteBuffer buffer = ByteBuffer.allocate(inputBytes.length + hmacSigner.digestSize());
    buffer.put(inputBytes);
    ByteBuffer sigBuffer = buffer.slice();
    buffer.limit(buffer.position());
    buffer.rewind();
    hmacSigner.sign(buffer, sigBuffer);
    buffer.rewind();
    sigBuffer.rewind();
    assertTrue(hmacSigner.verify(buffer, sigBuffer));
  }
  
  @Test
  public final void testDsaSignAndVerify() throws KeyczarException {
    Signer dsaSigner = new Signer(TEST_DATA + "/dsa");
    String sig = dsaSigner.sign(input);
    System.out.println("Dsa Sig: " + sig);
    assertTrue(dsaSigner.verify(input, sig));
    System.out.println(sig);
  }
  
  @Test
  public final void testVerify() throws KeyczarException {
    Signer dsaSigner = new Signer(TEST_DATA + "/dsa");
    assertTrue(dsaSigner.verify(input, dsaPrimarySig));
    assertTrue(dsaSigner.verify(input, dsaActiveSig));
  }

  @Test
  public final void testHmacVerify() throws KeyczarException {
    Signer hmacSigner = new Signer(TEST_DATA + "/hmac");
    String sig = hmacSigner.sign(input);
    assertEquals(sig, hmacPrimarySig);
    assertTrue(hmacSigner.verify(input, hmacPrimarySig));
    assertTrue(hmacSigner.verify(input, hmacActiveSig));
    
    
    byte[] sigBytes = hmacSigner.sign(inputBytes);
    ByteBuffer buffer = ByteBuffer.allocate(inputBytes.length + hmacSigner.digestSize());
    buffer.put(inputBytes);
    ByteBuffer sigBuffer = buffer.slice();
    buffer.limit(buffer.position());
    buffer.rewind();
    sigBuffer.put(keyczar.internal.Util.base64Decode(hmacPrimarySig));
    sigBuffer.rewind();
    assertTrue(hmacSigner.verify(buffer, sigBuffer));
  }

  @Test
  public final void testHmacBadSigs() throws KeyczarException {
    Signer hmacSigner = new Signer(TEST_DATA + "/hmac");
    byte[] sig = hmacSigner.sign(inputBytes);

    // Another input string should not verify
    assertFalse(hmacSigner.verify("Some other string".getBytes(), sig));

    try {
      hmacSigner.verify(inputBytes, new byte[0]);
    } catch (ShortSignatureException e) {
      // Expected
    }
    // Munge the signature version
    sig[0] ^= 23;
    try {
      hmacSigner.verify(inputBytes, sig);
    } catch (BadVersionException e) {
      // Expected
    }
    // Reset the version
    sig[0] ^= 23;
    // Munge the key identifier
    sig[1] ^= 45;
    try {
      hmacSigner.verify(inputBytes, sig);
    } catch (KeyNotFoundException e) {
      // Expected
    }
    // Reset the key identifier
    sig[1] ^= 45;
     
  }

}
