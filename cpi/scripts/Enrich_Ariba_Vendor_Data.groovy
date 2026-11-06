import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.time.Instant

def Message processData(Message message) {
    String rawBody = message.getBody(String.class)
    def jsonSlurper = new JsonSlurper()
    def certData = jsonSlurper.parseText(rawBody)

    String supplierId = certData.SupplierID ? certData.SupplierID.toString() : "VEND-100234"
    String certId = certData.CertificateID ? certData.CertificateID.toString() : "CERT-2026-X1"

    def aribaProfile = [
        "AribaNetworkID"     : "AN0158932014-T",
        "QualificationStatus": "QUALIFIED_PREFERRED",
        "ESGComplianceScore" : 94.5,
        "LastAuditDate"      : "2026-09-15",
        "RiskRating"         : "LOW"
    ]

    certData.AribaGovernance = aribaProfile
    certData.IngestionTimestamp = Instant.now().toString()

    message.setProperty("SupplierID", supplierId)
    message.setProperty("CertificateID", certId)
    message.setBody(JsonOutput.toJson(certData))
    message.setHeader("Content-Type", "application/json")
    
    return message
}
