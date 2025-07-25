import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_doc_scanner_method_channel.dart';

abstract class FlutterDocScannerPlatform extends PlatformInterface {
  /// Constructs a FlutterDocScannerPlatform.
  FlutterDocScannerPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterDocScannerPlatform _instance = MethodChannelFlutterDocScanner();

  /// The default instance of [FlutterDocScannerPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterDocScanner].
  static FlutterDocScannerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterDocScannerPlatform] when
  /// they register themselves.
  static set instance(FlutterDocScannerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<dynamic> getScannedDocumentAsImages([int page = 4]) {
    throw UnimplementedError('getScannedDocumentAsImages() has not been implemented.');
  }

  Future<dynamic> getScannedDocumentAsPdf([int page = 4]) {
    throw UnimplementedError('getScannedDocumentAsPdf() has not been implemented.');
  }
}
