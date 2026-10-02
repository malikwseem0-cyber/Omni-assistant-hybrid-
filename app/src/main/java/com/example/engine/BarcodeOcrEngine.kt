package com.example.engine

data class ScanResult(
    val type: ScanType,
    val rawValue: String,
    val title: String,
    val suggestedAction: String
)

enum class ScanType {
    QR_CODE,
    BARCODE_EAN,
    OCR_TEXT,
    WIFI_CONFIG,
    URL_LINK
}

object BarcodeOcrEngine {

    fun processSimulatedScan(sampleType: Int): ScanResult {
        return when (sampleType % 4) {
            0 -> ScanResult(
                type = ScanType.URL_LINK,
                rawValue = "https://github.com/android/architecture-samples",
                title = "Web Link Detected",
                suggestedAction = "Open in Browser"
            )
            1 -> ScanResult(
                type = ScanType.WIFI_CONFIG,
                rawValue = "WIFI:S:OmniOffice_5G;T:WPA;P:cybersecure99;;",
                title = "Wi-Fi Network QR",
                suggestedAction = "Connect to OmniOffice_5G"
            )
            2 -> ScanResult(
                type = ScanType.OCR_TEXT,
                rawValue = "INVOICE #94820 - Total Due: $149.50 - Due Date: Oct 15",
                title = "Document OCR Text",
                suggestedAction = "Add Payment Reminder"
            )
            else -> ScanResult(
                type = ScanType.BARCODE_EAN,
                rawValue = "8901030876543",
                title = "Product Barcode (EAN-13)",
                suggestedAction = "Search Product Details"
            )
        }
    }
}
