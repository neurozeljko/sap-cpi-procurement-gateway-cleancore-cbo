import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import java.time.Instant
import java.util.UUID

def Message processData(Message message) {
    def properties = message.getProperties()
    def ex = properties.get("CamelExceptionCaught")
    
    String rawError = ex ? ex.getMessage() : "Procurement Gateway Validation or CBO Ingestion Failure"
    String rawPayload = properties.get("Original_Payload") ? properties.get("Original_Payload").toString() : "{}"
    String supplierId = properties.get("SupplierID") ? properties.get("SupplierID").toString() : "UNKNOWN_SUPPLIER"
    String certId = properties.get("CertificateID") ? properties.get("CertificateID").toString() : "UNKNOWN_CERT"
    String corrId = properties.get("CorrelationID") ? properties.get("CorrelationID").toString() : UUID.randomUUID().toString()

    def dlqEnvelope = [
        "DLQ_Header": [
            "Category"      : "PROCUREMENT_GATEWAY_EXCEPTION",
            "Timestamp"     : Instant.now().toString(),
            "CorrelationId" : corrId,
            "SupplierID"    : supplierId,
            "CertificateID" : certId,
            "TargetEntity"  : "YY1_MATERIAL_CERTIFICATE_CDS"
        ],
        "ErrorDetails": [
            "FaultReason"   : rawError,
            "ActionRequired": "Review supplier accreditation in Ariba Network or verify CBO CDS view mapping"
        ],
        "ArchivedRawPayload": rawPayload
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(dlqEnvelope)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("DLQ_Status", "FAILED_ROUTED_TO_SFTP")
    return message
}
