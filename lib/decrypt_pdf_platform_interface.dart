// lib/decrypt_pdf_platform_interface.dart
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'decrypt_pdf_method_channel.dart';

abstract class DecryptPdfPlatform extends PlatformInterface {
  DecryptPdfPlatform() : super(token: _token);

  static final Object _token = Object();

  static DecryptPdfPlatform _instance = MethodChannelDecryptPdf();

  static DecryptPdfPlatform get instance => _instance;

  static set instance(DecryptPdfPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('getPlatformVersion() has not been implemented.');
  }

  Future<bool> isPdfProtected({required String filePath}) {
    throw UnimplementedError('isPdfProtected() has not been implemented.');
  }

  Future<String?> openPdf({
    required String filePath,
    required String password,
  }) {
    throw UnimplementedError('openPdf() has not been implemented.');
  }

  Future<String?> getPdfAsBase64({
    required String filePath,
    required String password,
  }) {
    throw UnimplementedError('getPdfAsBase64() has not been implemented.');
  }

   Future<bool> encryptPdf({
    required String filePath,
    required String ownerPassword,
    required String userPassword,
  }) {
    throw UnimplementedError('encryptPdf() has not been implemented.');
  }

  Future<bool> decryptPdf({
    required String filePath,
    required String password,
  }) {
    throw UnimplementedError('decryptPdf() has not been implemented.');
  }

  Future<bool> isEncrypted({required String filePath}) {
    throw UnimplementedError('isEncrypted() has not been implemented.');
  }
}
