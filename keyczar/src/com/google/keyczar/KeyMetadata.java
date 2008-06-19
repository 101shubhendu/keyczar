/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.keyczar;

import com.google.gson.annotations.Expose;
import com.google.keyczar.enums.KeyPurpose;
import com.google.keyczar.enums.KeyType;
import com.google.keyczar.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Encodes metadata for a set of keys which consists of the following:
 * <ul>  
 *   <li>a string-valued name,
 *   <li>a KeyPurpose,
 *   <li>a KeyType, and
 *   <li>a set of KeyVersion values.
 * </ul>
 * 
 * <p>JSON Representation consists of the following fields:
 * <ul>  
 *   <li>"name": a String name, 
 *   <li>"purpose": JSON representation of KeyPurpose value, 
 *   <li>"type": JSON representation of KeyType value,
 *   <li>"versions": JSON representation of an array of KeyVersion values.
 * </ul>
 *            
 * @author steveweis@gmail.com (Steve Weis)
 *
 */
class KeyMetadata {
  @Expose private String name = "";
  @Expose private KeyPurpose purpose = KeyPurpose.TEST;
  @Expose private KeyType type = KeyType.TEST;
  @Expose private ArrayList<KeyVersion> versions = new ArrayList<KeyVersion>();

  private KeyMetadata() {
    // For GSON
  }
  
  KeyMetadata(String n, KeyPurpose p, KeyType t) {
    name = n;
    purpose = p;
    type = t;
  }

  @Override
  public String toString() {
    return Util.gson().toJson(this);
  }

  boolean addVersion(KeyVersion version) {
    return versions.add(version);
  }
  
  // FIXME: need to change version number scheme, otherwise removing version
  // messes up sequential numbering
  boolean removeVersion(KeyVersion version) {
    return versions.remove(version);
  }

  String getName() {
    return name;
  }

  KeyPurpose getPurpose() {
    return purpose;
  }

  KeyType getType() {
    return type;
  }
  
  /**
   * Returns the version corresponding to the version number.
   * @param versionNumber should be in range [1,N] for N versions.
   * @return KeyVersion corresponding to given number, or null if illegal
   */
  KeyVersion getVersion(int versionNumber) {
    return (0 < versionNumber && versionNumber <= versions.size()) ? 
        versions.get(versionNumber-1) : null;
  }

  List<KeyVersion> getVersions() {
    return versions;
  }

  static KeyMetadata read(String jsonString) {
    return Util.gson().fromJson(jsonString, KeyMetadata.class);
  }
}
