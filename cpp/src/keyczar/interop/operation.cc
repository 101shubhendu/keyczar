// Copyright 2013 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
#include <keyczar/interop/operation.h>

#include <keyczar/base/base64w.h>
#include <keyczar/base/file_path.h>
#include <keyczar/base/json_reader.h>
#include <keyczar/base/json_writer.h>
#include <keyczar/keyczar.h>
#include <keyczar/session.h>

namespace keyczar {
namespace interop {

// static
Operation * Operation::GetOperationByName(
      const std::string& name, const std::string& key_path,
      const std::string& test_data) {
  if (name == "unversioned") {
    return new UnversionedSignOperation(key_path, test_data);
  } else if (name == "signedSession") {
    return new SignedSessionOperation(key_path, test_data);
  } else if (name == "attached") {
    return new AttachedSignOperation(key_path, test_data);
  } else if (name == "sign") {
    return new SignOperation(key_path, test_data);
  } else if (name == "encrypt") {
    return new EncryptOperation(key_path, test_data);
  } else {
    return NULL;
  }
}

bool Operation::OutputToJson(
      const std::string& output, std::string * json_string) {
  std::string encoded_output;
  DictionaryValue dictionary_value;

  if (!base::Base64WEncode(output, &encoded_output)) {
    return false;
  }

  Value * output_value = Value::CreateStringValue(encoded_output);

  if (!dictionary_value.Set("output", output_value)) {
    return false;
  }

  base::JSONWriter::Write(&dictionary_value, false, json_string);

  return true;
}

bool Operation::InputFromJson(const std::string& json, std::string * output) {
  std::string encoded_output;
  scoped_ptr<const Value> json_value(base::JSONReader::Read(json, false));
  if (json_value.get() == NULL
      || !json_value->IsType(Value::TYPE_DICTIONARY))
    return false;

  const DictionaryValue* json_dict
      = static_cast<const DictionaryValue*>(json_value.get());

  if (!json_dict->GetString("output", &encoded_output)) {
    return false;
  }
  if (!base::Base64WDecode(encoded_output, output)) {
    return false;
  }
  return true;
}

const std::string Operation::GetKeyPath(const std::string& algorithm) {
  FilePath fp(key_path_);
  return fp.Append(algorithm).value();
}


bool EncryptOperation::Generate(
    const std::string& algorithm, const DictionaryValue * generate_params,
    std::string * output) {
  keyczar::Keyczar* crypter;
  std::string encoding, crypter_class;
  if (!generate_params->GetString("encoding", &encoding) ||
      !generate_params->GetString("class", &crypter_class)) {
    return false;
  }
  if (crypter_class == "encrypter") {
    crypter = keyczar::Encrypter::Read(GetKeyPath(algorithm));
  } else if (crypter_class == "crypter") {
    crypter = keyczar::Crypter::Read(GetKeyPath(algorithm));
  } else {
    return false;
  }
  if (!crypter) return false;
  if (encoding == "unencoded") {
    crypter->set_encoding(Keyczar::NO_ENCODING);
  } else if (encoding != "encoded") {
    return false;
  }
  if (!crypter->Encrypt(test_data_, output)) return false;
  return true;
}

bool EncryptOperation::Test(
      const std::string& output, const std::string& algorithm,
      const DictionaryValue * generate_params,
      const DictionaryValue * test_params) {
  keyczar::Keyczar* crypter;
  std::string encoding, plaintext;
  if (!generate_params->GetString("encoding", &encoding)) {
    return false;
  }
  crypter = keyczar::Crypter::Read(GetKeyPath(algorithm));

  if (!crypter) return false;
  if (encoding == "unencoded") {
    crypter->set_encoding(Keyczar::NO_ENCODING);
  } else if (encoding != "encoded") {
    return false;
  }
  if (!crypter->Decrypt(output, &plaintext) || plaintext != test_data_) {
    return false;
  }
  return true;
}

bool SignedSessionOperation::OutputToJson(
      const std::string& output, std::string * json_string) {
  // Signed sessions already are in json format
  json_string->assign(std::string(output));
  return true;
}

bool SignedSessionOperation::InputFromJson(
      const std::string& json, std::string * output) {
  // Signed sessions already are in json format
  output->assign(std::string(json));
  return true;
}

bool SignedSessionOperation::Generate(
    const std::string& algorithm, const DictionaryValue * generate_params,
    std::string * output) {
  std::string session_material, encrypted_data, signer_algorithm;

  keyczar::Encrypter* key_encrypter;
  keyczar::Signer* signer;

  if (!generate_params->GetString("signer", &signer_algorithm)) {
    return false;
  }

  key_encrypter = keyczar::Crypter::Read(GetKeyPath(algorithm));
  signer = keyczar::Signer::Read(GetKeyPath(signer_algorithm));

  if (!key_encrypter || !signer) {
    return false;
  }

  keyczar::SignedSessionEncrypter* crypter =
      SignedSessionEncrypter::NewSessionEncrypter(key_encrypter, signer);

  if (!crypter) {
    return false;
  }
  crypter->set_encoding(Keyczar::NO_ENCODING);
  if (!crypter->EncryptedSessionBlob(&session_material) ||
      !crypter->SessionEncrypt(test_data_, &encrypted_data)) {
    return false;
  }

  std::string encoded_output;
  DictionaryValue dictionary_value;

  if (!base::Base64WEncode(encrypted_data, &encoded_output)) {
    return false;
  }

  Value * output_value = Value::CreateStringValue(encoded_output);
  Value * session_material_value = Value::CreateStringValue(session_material);

  if (!dictionary_value.Set("output", output_value) ||
      !dictionary_value.Set("sessionMaterial", session_material_value)) {
    return false;
  }

  base::JSONWriter::Write(&dictionary_value, false, output);

  return true;
}

bool SignedSessionOperation::Test(
      const std::string& output, const std::string& algorithm,
      const DictionaryValue * generate_params,
      const DictionaryValue * test_params) {
  std::string encoded_output,
      signer_algorithm,
      encrypted_data,
      session_material,
      plaintext;
  scoped_ptr<const Value> json_value(base::JSONReader::Read(output, false));
  if (json_value.get() == NULL ||
      !json_value->IsType(Value::TYPE_DICTIONARY))
    return false;

  const DictionaryValue* json_dict
      = static_cast<const DictionaryValue*>(json_value.get());

  if (!json_dict->GetString("output", &encoded_output) ||
      !json_dict->GetString("sessionMaterial", &session_material)) {
    return false;
  }
  if (!base::Base64WDecode(encoded_output, &encrypted_data)) {
    return false;
  }

  keyczar::Crypter* key_decrypter;
  keyczar::Verifier* verifier;

  if (!generate_params->GetString("signer", &signer_algorithm)) {
    return false;
  }

  key_decrypter = keyczar::Crypter::Read(GetKeyPath(algorithm));
  verifier = keyczar::Signer::Read(GetKeyPath(signer_algorithm));

  if (!key_decrypter || !verifier) {
    return false;
  }

  keyczar::SignedSessionDecrypter* crypter =
      SignedSessionDecrypter::NewSessionDecrypter(
          key_decrypter, verifier, session_material);

  if (!crypter) {
    return false;
  }

  crypter->set_encoding(Keyczar::NO_ENCODING);

  return crypter->SessionDecrypt(encrypted_data, &plaintext) &&
      plaintext == test_data_;
}

bool SignOperation::Generate(
    const std::string& algorithm, const DictionaryValue * generate_params,
    std::string * output) {
  keyczar::Keyczar* signer;
  std::string encoding, crypter_class;
  if (!generate_params->GetString("encoding", &encoding)) {
    return false;
  }
  signer = keyczar::Signer::Read(GetKeyPath(algorithm));
  if (!signer) return false;
  if (encoding == "unencoded") {
    signer->set_encoding(Keyczar::NO_ENCODING);
  } else if (encoding != "encoded") {
    return false;
  }
  if (!signer->Sign(test_data_, output)) return false;
  return true;
}

bool SignOperation::Test(
      const std::string& output, const std::string& algorithm,
      const DictionaryValue * generate_params,
      const DictionaryValue * test_params) {
  keyczar::Keyczar* verifier;
  std::string encoding, verifier_class;
  if (!generate_params->GetString("encoding", &encoding) ||
      !test_params->GetString("class", &verifier_class)) {
    return false;
  }
  if (verifier_class == "signer") {
    verifier = keyczar::Signer::Read(GetKeyPath(algorithm));
  } else if (verifier_class == "verifier") {
    verifier = keyczar::Verifier::Read(GetKeyPath(algorithm));
  } else {
    return false;
  }
  if (!verifier) return false;
  if (encoding == "unencoded") {
    verifier->set_encoding(Keyczar::NO_ENCODING);
  } else if (encoding != "encoded") {
    return false;
  }
  return verifier->Verify(test_data_, output);
}

bool AttachedSignOperation::Generate(
    const std::string& algorithm, const DictionaryValue * generate_params,
    std::string * output) {
  keyczar::Keyczar* signer;
  std::string encoding, crypter_class;
  if (!generate_params->GetString("encoding", &encoding)) {
    return false;
  }
  signer = keyczar::Signer::Read(GetKeyPath(algorithm));
  if (!signer) return false;
  if (encoding == "unencoded") {
    signer->set_encoding(Keyczar::NO_ENCODING);
  } else if (encoding != "encoded") {
    return false;
  }
  if (!signer->AttachedSign(test_data_, "", output)) return false;
  return true;
}

bool AttachedSignOperation::Test(
      const std::string& output, const std::string& algorithm,
      const DictionaryValue * generate_params,
      const DictionaryValue * test_params) {
  keyczar::Keyczar* verifier;
  std::string message, encoding, verifier_class;
  if (!generate_params->GetString("encoding", &encoding) ||
      !test_params->GetString("class", &verifier_class)) {
    return false;
  }
  if (verifier_class == "signer") {
    verifier = keyczar::Signer::Read(GetKeyPath(algorithm));
  } else if (verifier_class == "verifier") {
    verifier = keyczar::Verifier::Read(GetKeyPath(algorithm));
  } else {
    return false;
  }
  if (!verifier) return false;
  if (encoding == "unencoded") {
    verifier->set_encoding(Keyczar::NO_ENCODING);
  } else if (encoding != "encoded") {
    return false;
  }

  return verifier->AttachedVerify(output, "", &message) &&
      message == test_data_;
}

bool UnversionedSignOperation::Generate(
    const std::string& algorithm, const DictionaryValue * generate_params,
    std::string * output) {
  keyczar::Keyczar* signer;
  std::string encoding, crypter_class;
  if (!generate_params->GetString("encoding", &encoding)) {
    return false;
  }
  signer = keyczar::UnversionedSigner::Read(GetKeyPath(algorithm));
  if (!signer) return false;
  if (encoding == "unencoded") {
    signer->set_encoding(Keyczar::NO_ENCODING);
  } else if (encoding != "encoded") {
    return false;
  }
  if (!signer->Sign(test_data_, output)) return false;
  return true;
}

bool UnversionedSignOperation::Test(
      const std::string& output, const std::string& algorithm,
      const DictionaryValue * generate_params,
      const DictionaryValue * test_params) {
  keyczar::Keyczar* verifier;
  std::string encoding, verifier_class;
  if (!generate_params->GetString("encoding", &encoding) ||
      !test_params->GetString("class", &verifier_class)) {
    return false;
  }
  if (verifier_class == "signer") {
    verifier = keyczar::UnversionedSigner::Read(GetKeyPath(algorithm));
  } else if (verifier_class == "verifier") {
    verifier = keyczar::UnversionedVerifier::Read(GetKeyPath(algorithm));
  } else {
    return false;
  }
  if (!verifier) return false;
  if (encoding == "unencoded") {
    verifier->set_encoding(Keyczar::NO_ENCODING);
  } else if (encoding != "encoded") {
    return false;
  }
  return verifier->Verify(test_data_, output);
}

}  // namespace interop
}  // namespace keyczar
