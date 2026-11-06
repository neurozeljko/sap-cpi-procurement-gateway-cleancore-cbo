import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.util.UUID

def Message processData(Message message) {
    String body = message.getBody(String.class)
    def parser = new JsonSlurper()
    def data = parser.parseText(body)

    def aribaGov = data.AribaGovernance ?: [:]

    def cboPayload = [
        "CertificateUUID"        : UUID.randomUUID().toString(),
        "CertificateNumber"      : data.CertificateID ? data.CertificateID.toString() : "CERT-UNDEFINED",
        "PurchaseOrderNumber"    : data.PurchaseOrder ? data.PurchaseOrder.toString() : "PO-4500008812",
        "MaterialNumber"         : data.MaterialNumber ? data.MaterialNumber.toString() : "MAT-QC-9901",
        "BatchNumber"            : data.BatchNumber ? data.BatchNumber.toString() : "BATCH-2026-01",
        "SupplierID"             : data.SupplierID ? data.SupplierID.toString() : "VEND-100234",
        "AribaNetworkID"         : aribaGov.AribaNetworkID ? aribaGov.AribaNetworkID.toString() : "AN-UNREGISTERED",
        "ComplianceStatus"       : aribaGov.QualificationStatus ? aribaGov.QualificationStatus.toString() : "PENDING_REVIEW",
        "InspectionStandard"     : data.StandardApplied ? data.StandardApplied.toString() : "ISO 9001:2015",
        "CertificateValidityDate": data.ValidUntil ? data.ValidUntil.toString() : "2027-12-31",
        "IsCleanCoreCompliant"   : true
    ]

    message.setBody(JsonOutput.prettyPrint(JsonOutput.toJson(cboPayload)))
    message.setHeader("Content-Type", "application/json")
    message.setHeader("Accept", "application/json")
    
    return message
}
