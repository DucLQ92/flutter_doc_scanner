import 'package:flutter/foundation.dart';
import 'flutter_doc_scanner_platform_interface.dart';

class FlutterDocScanner {
  /// Get scanned document as images
  /// [page] ony Android
  /// result {imagesUri: [...], pageCount: ...}
  Future<dynamic> getScannedDocumentAsImages({int page = 4}) {
    return FlutterDocScannerPlatform.instance.getScannedDocumentAsImages(page);
  }

  /// Get scanned document as pdf
  /// [page] ony Android
  /// result {pdfUri: ..., pageCount: ...}
  Future<dynamic> getScannedDocumentAsPdf({int page = 4}) {
    return FlutterDocScannerPlatform.instance.getScannedDocumentAsPdf(page);
  }
}
