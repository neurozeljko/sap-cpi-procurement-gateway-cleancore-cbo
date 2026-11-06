# Enterprise Integration Architecture: Procurement Gateway & Clean Core Extensibility (Ariba & S/4HANA)

[![SAP Integration Suite](https://img.shields.io/badge/SAP%20Integration%20Suite-Cloud%20Integration-0A85EA?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![SAP API Management](https://img.shields.io/badge/SAP%20APIM-Edge%20Gateway%20Security-red?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![S/4HANA Clean Core](https://img.shields.io/badge/S%2F4HANA-Custom%20Business%20Objects%20(CBO)-8A2BE2?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![OData V4](https://img.shields.io/badge/Protocol-OData%20V4%20%7C%20REST-orange?style=for-the-badge)](https://www.odata.org)
[![SAP Ariba](https://img.shields.io/badge/Network-SAP%20Ariba%20Open%20Connectors-green?style=for-the-badge&logo=sap&logoColor=white)](https://www.ariba.com)
[![Clean Core](https://img.shields.io/badge/Clean%20Core-In--App%20Extensibility-success?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)

---

## Executive Summary

This enterprise integration blueprint implements an automated, secure **Procurement Gateway** on **SAP Integration Suite**. The architecture enables tier-1 and tier-2 suppliers operating outside the SAP Ariba Network to submit official **Certificates of Analysis (CoA)** and quality accreditation records directly into **SAP S/4HANA Cloud**.

By adhering to strict **SAP Clean Core** governance, the design completely avoids classical ABAP Z-table modifications. Instead, data persistence is achieved through **S/4HANA Custom Business Objects (CBO)** exposed via auto-generated **OData V4** and **Core Data Services (CDS)** views. The perimeter is protected by **SAP API Management**, while transactional vendor data is cross-referenced in-flight against the **SAP Ariba Network** via **Open Connectors**.

---

## Architecture Overview

### High-Level Topology

```
[External Suppliers / Quality Testing Labs]
       │
       │ HTTPS POST (Quality Inspection Certificate JSON)
       ▼
[SAP API Management (APIM Edge Gateway)]
 │
 ├── Perimeter Security:
 │    ├─ VerifyAPIKey Policy: Validates partner API Key
 │    ├─ JSON Threat Protection: Blocks malformed/malicious payloads
 │    └─ Spike Arrest: Rate limiting (30 requests/min per vendor)
 │
 └─► Mutual TLS / Cloud Integration Pipeline
       │
[SAP Integration Suite (Cloud Integration Pipeline)]
 │
 ├── 1. Inbound Ingestion Layer (/procurement/certificates/ingest):
 │    └─ Content Modifier: CM_Init_Audit_Context
 │         ├─ Preserves original payload in exchange property
 │         └─ Injects correlation headers (SAP_MessageProcessingLogID)
 │
 ├── 2. Ariba Network Enrichment:
 │    └─ Groovy Script: Enrich_Ariba_Vendor_Data.groovy
 │         ├─ Queries SAP Ariba Open Connectors REST API
 │         └─ Merges vendor qualification status, risk rating, and audit date
 │
 ├── 3. Clean Core OData V4 Normalization:
 │    └─ Groovy Script: Prepare_S4HANA_CBO_Payload.groovy
 │         ├─ Maps payload to CBO CDS View: YY1_MATERIAL_CERTIFICATE_CDS
 │         └─ Generates unique CertificateUUID and assigns validation flags
 │
 ├── 4. ERP Outbound Dispatch:
 │    └─ HTTP / OData V4 POST -> S/4HANA CBO In-App Extensibility Entity
 │
 └── 5. Exception Subprocess (SFTP Dead-Letter Governance):
      ├─ Intercepts schema failures, unaccredited vendors, or ERP downtime
      ├─ Groovy Formatter: Format_Procurement_DLQ_Error.groovy
      └─ Outbound SFTP Adapter -> /dlq/procurement/failed/
```

---

## Technical Specifications

| Component | Technical Implementation | Architectural Justification |
| :--- | :--- | :--- |
| **API Gateway** | SAP API Management (APIM) | Edge perimeter defense, rate limiting, and secure external exposure without opening core ERP ports. |
| **Security Policies** | `VerifyAPIKey`, `JSONThreatProtection`, `SpikeArrest` | Hardens inbound endpoints against DoS, payload injection, and unauthorized data ingestion. |
| **Vendor Enrichment** | SAP Ariba Open Connectors (REST API) | Cross-checks supplier compliance standing and accreditation status in the global network. |
| **In-App Extensibility** | Custom Business Objects (CBO) | Clean Core standard for extending S/4HANA persistence without touching underlying ABAP tables. |
| **Service Generation** | Core Data Services (CDS) Views & OData V4 | Optimized read/write access via native SAP HANA database views with atomic CRUD capabilities. |
| **DLQ Governance** | Exception Subprocess (`Format_Procurement_DLQ_Error.groovy`) | Routes corrupted certificate payloads to an SFTP quarantine repository for operational triage. |

---

## Clean Core CBO Entity Definition (`YY1_MATERIAL_CERTIFICATE_CDS`)

| Field Name | Type | Key | Business Semantic & Validation Rule |
| :--- | :--- | :---: | :--- |
| `CertificateUUID` | `Edm.Guid` | **Yes** | Universally Unique Identifier generated on payload ingestion. |
| `CertificateNumber` | `Edm.String(30)` | No | Supplier Certificate of Analysis (CoA) reference code. |
| `PurchaseOrderNumber` | `Edm.String(10)` | No | S/4HANA Purchasing Document (EBELN) link. |
| `MaterialNumber` | `Edm.String(40)` | No | S/4HANA Material Master SKU identifier (MATNR). |
| `BatchNumber` | `Edm.String(10)` | No | S/4HANA Inventory Batch identifier (CHARG). |
| `SupplierID` | `Edm.String(10)` | No | S/4HANA Business Partner / Vendor Number (LIFNR). |
| `AribaNetworkID` | `Edm.String(30)` | No | Enriched SAP Ariba Network organizational ID. |
| `ComplianceStatus` | `Edm.String(20)` | No | Ariba qualification score (`QUALIFIED_PREFERRED`). |
| `InspectionStandard` | `Edm.String(50)` | No | Regulatory framework applied (e.g. `ISO 9001:2015`). |
| `CertificateValidityDate` | `Edm.Date` | No | Expiration timestamp for incoming material batch approval. |
| `IsCleanCoreCompliant` | `Edm.Boolean` | No | Flag certifying adherence to zero-modification ERP extensions. |

---

## Repository Structure

```
├── README.md                                           # Comprehensive architectural documentation
├── ADR-001-Procurement-Gateway-CleanCore-CBO.md        # Architecture Decision Record
├── IF_Procurement_Gateway_Ariba_Enrichment_CBO.zip     # Production iFlow runtime bundle export
├── cpi/
│   └── scripts/
│       ├── Enrich_Ariba_Vendor_Data.groovy             # Ariba qualification enrichment pipeline
│       ├── Prepare_S4HANA_CBO_Payload.groovy           # Clean Core CBO OData V4 transformer
│       └── Format_Procurement_DLQ_Error.groovy         # Dead-letter queue incident formatter
├── apim/
│   └── policies/
│       └── APIM_Security_Policies.xml                  # APIM edge policy definitions
├── tests/
│   └── sample_quality_certificate.json                 # Valid supplier certificate payload
└── .gitignore                                          # Standard Git exclusions
```

---

## Verification & Deployment Runbook

### 1. Inbound Endpoint Verification
Verify runtime status in **Monitor** ──► **Manage Integration Content**:
```text
https://<tenant-runtime>.cfapps.<region>[.hana.ondemand.com/http/procurement/certificates/ingest](https://.hana.ondemand.com/http/procurement/certificates/ingest)
```

### 2. Dispatched Test
Execute an authorized POST request using the sample certificate payload:

```bash
curl -X POST \
  'https://<tenant-runtime>.cfapps.<region>[.hana.ondemand.com/http/procurement/certificates/ingest](https://.hana.ondemand.com/http/procurement/certificates/ingest)' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <BTP_OAUTH_TOKEN>' \
  --data-binary '@tests/sample_quality_certificate.json'
```

### 3. Monitoring Trace
* Inspect message lifecycle in **Monitor Message Processing**.
* Verify Ariba enrichment fields appended in the trace.
* Confirm mock S/4HANA CBO response returns HTTP 200 with status `POSTED`.

---

## Author

**Željko Popov**  
*Integration Architect | SAP Integration Suite & Clean Core Specialist*  
[GitHub Repositories](https://github.com/neurozeljko)
