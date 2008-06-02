// Keyczar (http://code.google.com/p/keyczar/) 2008

package com.google.keyczar.interfaces;

import com.google.keyczar.exceptions.KeyczarException;

/**
 * Abstract class for KeyczarReaders. Typically, these will read key files from
 * disk, but may be implemented to read from arbitrary sources.
 * 
 * @author steveweis@gmail.com (Steve Weis)
 */
public interface KeyczarReader {
  /**
   * Returns an input stream of a particular version of a packed key
   * 
   * @param version The Version number of the key to read
   * @return A JSON string data representation of a Key
   * @throws KeyczarException If an error occurs while attempting to read data,
   *         e.g. an IOException
   */
  String getKey(int version) throws KeyczarException;

  /**
   * @return A JSON string representation of KeyMetadata
   * @throws KeyczarException If an error occurs while attempting to read data,
   *         e.g. an IOException
   */
  String getMetadata() throws KeyczarException;
}
