# ADR-001: Modernizing Supplier Quality Ingestion via S/4HANA CBO and APIM Gateway

## Status
Accepted

## Context
Non-Ariba suppliers must submit material quality inspection certificates (CoA) directly into S/4HANA. Under SAP Clean Core principles, classical ABAP Z-tables and modifications to the ERP core are strictly forbidden.

## Decision
We implement a decoupled cloud gateway architecture on SAP Integration Suite:
1. **S/4HANA In-App Extensibility:** Custom Business Objects (CBO) generate native CDS views (`YY1_MATERIAL_CERTIFICATE_CDS`) and OData V4 services.
2. **SAP API Management:** Acts as the DMZ edge gateway applying API Key verification, Spike Arrest, and JSON Threat Protection.
3. **Open Connectors:** Connects to SAP Ariba Network REST endpoints to enrich supplier qualification ratings.
4. **Dead-Letter Queue:** Routes unverified or malformed supplier payloads into an SFTP quarantine DLQ.
