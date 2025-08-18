import 'dart:async';

import 'decrypt_pdf_platform_interface.dart';

class DecryptPdf {
  static Future<String?> getPlatformVersion() {
    return DecryptPdfPlatform.instance.getPlatformVersion();
  }

  static Future<bool> isPdfProtected({required String filePath}) {
    return DecryptPdfPlatform.instance.isPdfProtected(filePath: filePath);
  }

  static Future<String?> openPdf({
    required String filePath,
    required String password,
  }) {
    return DecryptPdfPlatform.instance.openPdf(
      filePath: filePath,
      password: password,
    );
  }

  //get getPdfAsBase64
  static Future<String?> getPdfAsBase64({
    required String filePath,
    required String password,
  }) {
    return DecryptPdfPlatform.instance.getPdfAsBase64(
      filePath: filePath,
      password: password,
    );
  }

  static Future<bool> decryptPdf({
    required String filePath,
    required String password,
  }) {
    return DecryptPdfPlatform.instance.decryptPdf(
      filePath: filePath,
      password: password,
    );
  }

  static Future<bool> encryptPdf({
    required String filePath,
    required String ownerPassword,
    required String userPassword,
  }) {
    return DecryptPdfPlatform.instance.encryptPdf(
      filePath: filePath,
      ownerPassword: ownerPassword,
      userPassword: userPassword,
    );
  }

  static Future<bool> isEncrypted({required String filePath}) {
    return DecryptPdfPlatform.instance.isEncrypted(filePath: filePath);
  }
}
