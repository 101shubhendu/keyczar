package org.keyczar.interop.operations;

import org.keyczar.Signer;
import org.keyczar.Verifier;
import org.keyczar.exceptions.KeyczarException;

import java.util.Map;

/**
 * Tests functionality of signing
 */
public class SignOperation extends Operation {

  public SignOperation(String keyPath, String testData) {
    super(keyPath, testData);
  }

  @Override
  public byte[] generate(String algorithm, Map<String, String> generateParams)
      throws KeyczarException{
    Signer signer = new Signer(
        getReader(algorithm, generateParams.get("cryptedKeySet"), ""));
    if (generateParams.get("encoding").equals("encoded")) {
      String signature = signer.sign(testData);
      return signature.getBytes();
    } else if (generateParams.get("encoding").equals("unencoded")) {
      byte[] signature = signer.sign(testData.getBytes());
      return signature;
    } else {
      throw new KeyczarException("Expects encoded or unencoded in parameters");
    }
  }

  @Override
  public void test(
      Map<String, String> output, String algorithm, Map<String, String> generateParams,
      Map<String, String> testParams) throws KeyczarException {
    if (testParams.get("class").equals("signer")) {
      Signer verifier = new Signer(
          getReader(algorithm, generateParams.get("cryptedKeySet"), testParams.get("pubKey")));
      if (generateParams.get("encoding").equals("encoded")) {
        assert(verifier.verify(testData, new String(readOutput(output))));
      } else if (generateParams.get("encoding").equals("unencoded")) {
        assert(verifier.verify(testData.getBytes(), readOutput(output)));
      } else {
        throw new KeyczarException("Expects encoded or unencoded in parameters");
      }
    } else if (testParams.get("class").equals("verifier")) {
      Verifier verifier = new Verifier(
          getReader(algorithm, generateParams.get("cryptedKeySet"), testParams.get("pubKey")));
      if (generateParams.get("encoding").equals("encoded")) {
        assert(verifier.verify(testData, new String(readOutput(output))));
      } else if (generateParams.get("encoding").equals("unencoded")) {
        assert(verifier.verify(testData.getBytes(), readOutput(output)));
      } else {
        throw new KeyczarException("Expects encoded or unencoded in parameters");
      }
    } else {
      throw new KeyczarException("Expects signer or verifier in parameters");
    }
  }

}
