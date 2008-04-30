package keyczar.internal;

import java.security.DigestException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {
  private static MessageDigest md;
  static {
    try {
      md = MessageDigest.getInstance(Messages.getString("HashAlgorithm"));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(
          Messages.getString("Util.unsupportedHashAlgorithm") +
          Messages.getString("HashAlgorithm"), e);
    }
  }

  private Util() {
    // Don't new me.
  }
  
  /**
   * Hashes a variable number of inputs and returns a new byte array
   *  
   * @param inputs The inputs to hash
   * @return The hash output
   */
  static synchronized byte[] hash(byte[]... inputs) {
    for (byte[] array : inputs) {
      md.update(array);
    }
    return md.digest();
  }

  /**
   * Hashes a variable number of input arrays and writes them into the output
   * buffer starting from the given offset.
   * 
   * @param dest The destination array to write the hash into
   * @param offset The offset to start writing the hash from
   * @param len The length allocated for the hash.
   * @param inputs The inputs to hash
   * @throws GeneralSecurityException If the allocated length is not large
   * enough
   */
   static synchronized void hash(byte[] dest, int offset,
      int len, byte[]... inputs) throws GeneralSecurityException {
    for (byte[] array : inputs) {
      md.update(array);
    }
    md.digest(dest, offset, len);
  }

  /**
   * Returns a byte array containing 4 big-endian ordered bytes representing 
   * the given integer.
   * 
   * @param input The integer to convert to a byte array.
   * @return A byte array representation of an integer.
   */
  static byte[] fromInt(int input) {
    byte[] output = new byte[4];
    writeInt(input, output, 0);
    return output;
  }
  
  /**
   * Writes 4 big-endian ordered bytes representing the given integer into the 
   * destination byte array starting from the given offset. 
   * 
   * This method does not check the destination array length.
   * 
   * @param input The integer to convert to bytes
   * @param dest The array in which to write the integer byte representation
   * @param offset The offset to start writing the bytes from
   */
  static void writeInt(int input, byte[] dest, int offset) {
    dest[offset++] = (byte) (input >> 24);
    dest[offset++] = (byte) (input >> 16);
    dest[offset++] = (byte) (input >> 8);
    dest[offset++] = (byte) (input);
  }

  /**
   * Converts a given byte array to an integer. Reads the bytes in big-endian
   * order.
   * 
   * This method does not check the source array length.
   * 
   * @param src A big-endian representation of an integer 
   * @return The integer value represented by the source array 
   */
  static int toInt(byte[] src) {
    return readInt(src, 0);
  }

  /**
   * Reads 4 big-endian ordered bytes from a given offset in an array and
   * returns an integer representation.
   * 
   * This method does not check the source array length.
   * 
   * @param src The source array to read bytes from
   * @param offset The offset to start reading bytes from.
   * @return The integer value represented by the source array from the offset 
   */
  static int readInt(byte[] src, int offset) {
    int output = 0;
    output |= (src[offset++] & 0xFF) << 24;
    output |= (src[offset++] & 0xFF) << 16;
    output |= (src[offset++] & 0xFF) << 8;
    output |= (src[offset++] & 0xFF);
    return output;
  }
  
  /**
   * Returns a byte array containing 8 big-endian ordered bytes representing 
   * the given long.
   * 
   * @param input The long to convert to a byte array.
   * @return A byte array representation of a long.
   */
  static byte[] fromLong(long input) {
    byte[] output = new byte[8];
    writeLong(input, output, 0);
    return output;
  }
  
  /**
   * Writes 8 big-endian ordered bytes representing the given long into the 
   * destination byte array starting from the given offset. 
   * 
   * This method does not check the destination array length.
   * 
   * @param input The long to convert to bytes
   * @param dest The array in which to write the long byte representation
   * @param offset The offset to start writing the bytes from
   */
  static void writeLong(long input, byte[] dest, int offset) {
    dest[offset++] = (byte) (input >> 56);
    dest[offset++] = (byte) (input >> 48);
    dest[offset++] = (byte) (input >> 40);
    dest[offset++] = (byte) (input >> 32);
    dest[offset++] = (byte) (input >> 24);
    dest[offset++] = (byte) (input >> 16);
    dest[offset++] = (byte) (input >> 8);
    dest[offset++] = (byte) (input);
  }

  /**
   * Converts a given byte array to a long. Reads the bytes in big-endian
   * order.
   * 
   * This method does not check the source array length.
   * 
   * @param src A big-endian representation of a long 
   * @return The long value represented by the source array 
   */
  static long toLong(byte[] src) {
    return readLong(src, 0);
  }

  /**
   * Reads 8 big-endian ordered bytes from a given offset in an array and
   * returns a long representation.
   * 
   * This method does not check the source array length.
   * 
   * @param src The source array to read bytes from
   * @param offset The offset to start reading bytes from.
   * @return The long value represented by the source array from the offset 
   */
  static long readLong(byte[] src, int offset) {
    long output = 0;
    output |= (src[offset++] & 0xFFL) << 56;
    output |= (src[offset++] & 0xFFL) << 48;
    output |= (src[offset++] & 0xFFL) << 40;
    output |= (src[offset++] & 0xFFL) << 32;
    output |= (src[offset++] & 0xFFL) << 24;
    output |= (src[offset++] & 0xFFL) << 16;
    output |= (src[offset++] & 0xFFL) << 8;
    output |= (src[offset++] & 0xFFL);
    return output;
  }
}
