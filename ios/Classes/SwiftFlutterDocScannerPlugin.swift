import Flutter
import UIKit
import Vision
import VisionKit
import PDFKit

@available(iOS 13.0, *)
public class SwiftFlutterDocScannerPlugin: NSObject, FlutterPlugin, VNDocumentCameraViewControllerDelegate {
    var resultChannel: FlutterResult?
    var presentingController: VNDocumentCameraViewController?
    var currentMethod: String?
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_doc_scanner", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterDocScannerPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "getScannedDocumentAsImages" {
            let presentedVC: UIViewController? = UIApplication.shared.keyWindow?.rootViewController
            self.resultChannel = result
            self.currentMethod = call.method
            self.presentingController = VNDocumentCameraViewController()
            self.presentingController!.delegate = self
            presentedVC?.present(self.presentingController!, animated: true)
        } else if call.method == "getScannedDocumentAsPdf" {
            let presentedVC: UIViewController? = UIApplication.shared.keyWindow?.rootViewController
            self.resultChannel = result
            self.currentMethod = call.method
            self.presentingController = VNDocumentCameraViewController()
            self.presentingController!.delegate = self
            presentedVC?.present(self.presentingController!, animated: true)
        } else {
            result(FlutterMethodNotImplemented)
            return
        }
    }
    
    func getDocumentsDirectory() -> URL {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        let documentsDirectory = paths[0]
        return documentsDirectory
    }
    
    private func getCacheDirectory() -> URL {
        return FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
    }
    
    public func documentCameraViewController(_ controller: VNDocumentCameraViewController, didFinishWith scan: VNDocumentCameraScan) {
        showLoading()
        
        DispatchQueue.global(qos: .userInitiated).async {
            if self.currentMethod == "getScannedDocumentAsImages" {
                self.saveScannedImages(scan: scan)
            } else if self.currentMethod == "getScannedDocumentAsPdf" {
                self.saveScannedPdf(scan: scan)
            }
            DispatchQueue.main.async {
                self.hideLoading()
                self.presentingController?.dismiss(animated: true)
            }
        }
    }
    
    private func saveScannedImages(scan: VNDocumentCameraScan) {
        let tempDirPath = getCacheDirectory()
        let currentDateTime = Date()
        let df = DateFormatter()
        df.dateFormat = "yyyyMMdd-HHmmss"
        let formattedDate = df.string(from: currentDateTime)
        var filenames: [String] = []
        for i in 0 ..< scan.pageCount {
            let page = scan.imageOfPage(at: i)
            let url = tempDirPath.appendingPathComponent(formattedDate + "-\(i).png")
            try? page.pngData()?.write(to: url)
            filenames.append(url.path)
        }
        let resultMap: [String: Any] = [
            "imagesUri": filenames,
            "pageCount": scan.pageCount
        ]
        resultChannel?(resultMap)
    }
    
    private func saveScannedPdf(scan: VNDocumentCameraScan) {
        let tempDirPath = getCacheDirectory()
        let currentDateTime = Date()
        let df = DateFormatter()
        df.dateFormat = "yyyyMMdd-HHmmss"
        let formattedDate = df.string(from: currentDateTime)
        let pdfFilePath = tempDirPath.appendingPathComponent("\(formattedDate).pdf")
        
        let pdfDocument = PDFDocument()
        for i in 0 ..< scan.pageCount {
            let pageImage = scan.imageOfPage(at: i)
            if let pdfPage = PDFPage(image: pageImage) {
                pdfDocument.insert(pdfPage, at: pdfDocument.pageCount)
            }
        }
        
        do {
            try pdfDocument.write(to: pdfFilePath)
            let resultMap: [String: Any] = [
                "pdfUri": pdfFilePath.path,
                "pageCount": scan.pageCount
            ]
            resultChannel?(resultMap)
        } catch {
            resultChannel?(FlutterError(code: "PDF_CREATION_ERROR", message: "Failed to create PDF", details: error.localizedDescription))
        }
    }
    
    public func documentCameraViewControllerDidCancel(_ controller: VNDocumentCameraViewController) {
        resultChannel?(nil)
        presentingController?.dismiss(animated: true)
    }
    
    public func documentCameraViewController(_ controller: VNDocumentCameraViewController, didFailWithError error: Error) {
        resultChannel?(FlutterError(code: "SCAN_ERROR", message: "Failed to scan documents", details: error.localizedDescription))
        presentingController?.dismiss(animated: true)
    }
    
    var loadingOverlay: UIView?
    func showLoading() {
        guard let presentingVC = presentingController else { return }
        
        let overlay = UIView(frame: presentingVC.view.bounds)
        overlay.backgroundColor = UIColor.black.withAlphaComponent(0.4)
        overlay.isUserInteractionEnabled = true  // block tap
        
        let indicator = UIActivityIndicatorView(style: .large)
        indicator.color = .white  // set màu trắng tuyệt đối
        indicator.center = overlay.center
        indicator.startAnimating()
        
        overlay.addSubview(indicator)
        presentingVC.view.addSubview(overlay)
        
        loadingOverlay = overlay
    }
    
    func hideLoading() {
        loadingOverlay?.removeFromSuperview()
        loadingOverlay = nil
    }
}
