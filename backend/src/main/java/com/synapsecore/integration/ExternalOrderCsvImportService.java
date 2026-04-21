package com.synapsecore.integration;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.audit.AuditLogService;
import com.synapsecore.domain.dto.OrderCreateRequest;
import com.synapsecore.domain.dto.OrderItemRequest;
import com.synapsecore.domain.entity.IntegrationConnectorType;
import com.synapsecore.domain.dto.OrderResponse;
import com.synapsecore.domain.entity.BusinessEventType;
import com.synapsecore.domain.service.OrderService;
import com.synapsecore.event.BusinessEventService;
import com.synapsecore.integration.dto.ExternalOrderCsvImportFailure;
import com.synapsecore.integration.dto.ExternalOrderCsvImportOrderResult;
import com.synapsecore.integration.dto.ExternalOrderCsvImportResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ExternalOrderCsvImportService {

    private static final Pattern SOURCE_SYSTEM_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final OrderService orderService;
    private final BusinessEventService businessEventService;
    private final AuditLogService auditLogService;
    private final IntegrationConnectorService integrationConnectorService;
    private final IntegrationConnectorPolicyService integrationConnectorPolicyService;
    private final IntegrationImportRunService integrationImportRunService;
    private final IntegrationReplayService integrationReplayService;
    private final IntegrationInboundRecordService integrationInboundRecordService;
    private final RequestTraceContext requestTraceContext;

    public ExternalOrderCsvImportResponse ingest(MultipartFile file, String sourceSystemDefault) {
        return ingest(file, sourceSystemDefault, null);
    }

    public ExternalOrderCsvImportResponse ingest(MultipartFile file,
                                                 String sourceSystemDefault,
                                                 com.synapsecore.domain.entity.IntegrationConnector authenticatedConnector) {
        if (file == null || file.isEmpty()) {
            throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.MISSING_FILE, "CSV file is required.");
        }

        String normalizedDefaultSource = normalizeOptionalSourceSystem(sourceSystemDefault);
        CsvFileContent csvContent = readCsv(file, normalizedDefaultSource);
        if (csvContent.rows().isEmpty() && csvContent.failures().isEmpty()) {
            throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.EMPTY_CSV, "CSV import requires at least one data row.");
        }

        Map<ImportOrderKey, List<ParsedCsvRow>> groupedRows = groupRows(csvContent.rows(), csvContent.failures());
        List<ExternalOrderCsvImportOrderResult> importedOrders = new ArrayList<>();
        List<ExternalOrderCsvImportFailure> failedOrders = new ArrayList<>(csvContent.failures());

        groupedRows.forEach((key, rows) -> {
            Long inboundRecordId = null;
            String tenantCode = requestTraceContext.getCurrentTenant()
                .filter(currentTenant -> !RequestTraceContext.MISSING_TENANT_CONTEXT.equalsIgnoreCase(currentTenant))
                .orElse(authenticatedConnector != null ? authenticatedConnector.getTenant().getCode() : null);
            try {
                if (authenticatedConnector != null
                    && !authenticatedConnector.getSourceSystem().equalsIgnoreCase(key.sourceSystem())) {
                    throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.CONNECTOR_SOURCE_MISMATCH,
                        "Connector-authenticated CSV imports may only submit orders for sourceSystem "
                            + authenticatedConnector.getSourceSystem() + ".");
                }
                var connector = authenticatedConnector != null
                    ? authenticatedConnector
                    : integrationConnectorService.requireEnabledConnector(
                        key.sourceSystem(),
                        IntegrationConnectorType.CSV_ORDER_IMPORT,
                        "accept CSV imports");
                tenantCode = connector.getTenant().getCode();
                inboundRecordId = integrationInboundRecordService.recordReceived(
                    tenantCode,
                    connector.getSourceSystem(),
                    IntegrationConnectorType.CSV_ORDER_IMPORT,
                    Objects.requireNonNullElse(file.getOriginalFilename(), "orders.csv"),
                    key.externalOrderId(),
                    key.warehouseCode(),
                    new OrderCreateRequest(
                        key.externalOrderId(),
                        key.warehouseCode(),
                        rows.stream()
                            .map(row -> new OrderItemRequest(row.productSku(), row.quantity(), row.unitPrice()))
                            .toList()
                    )
                ).getId();

                var preparedOrder = integrationConnectorPolicyService.prepareOrder(
                    connector,
                    key.externalOrderId(),
                    key.warehouseCode(),
                    null,
                    null,
                    rows.stream()
                        .map(row -> new OrderItemRequest(row.productSku(), row.quantity(), row.unitPrice()))
                        .toList()
                );

                OrderResponse order = orderService.createOrder(
                    preparedOrder.orderRequest(),
                    buildIngestionSource(key.sourceSystem())
                );
                integrationInboundRecordService.markAccepted(inboundRecordId, buildIngestionSource(key.sourceSystem()));

                importedOrders.add(new ExternalOrderCsvImportOrderResult(
                    key.sourceSystem(),
                    buildIngestionSource(key.sourceSystem()),
                    key.externalOrderId(),
                    preparedOrder.orderRequest().warehouseCode(),
                    preparedOrder.lineItemCount(),
                    Instant.now(),
                    order
                ));
            } catch (ResponseStatusException exception) {
                var failure = IntegrationFailureCodes.extract(exception);
                integrationInboundRecordService.markRejected(inboundRecordId, failure.failureCode(), failure.failureMessage());
                failedOrders.add(new ExternalOrderCsvImportFailure(
                    key.sourceSystem(),
                    key.externalOrderId(),
                    key.warehouseCode(),
                    rows.stream().map(ParsedCsvRow::rowNumber).toList(),
                    failure.failureCode(),
                    failure.failureMessage()
                ));
                integrationReplayService.recordFailure(
                    tenantCode,
                    key.sourceSystem(),
                    IntegrationConnectorType.CSV_ORDER_IMPORT,
                    new OrderCreateRequest(
                        key.externalOrderId(),
                        key.warehouseCode(),
                        rows.stream()
                            .map(row -> new OrderItemRequest(row.productSku(), row.quantity(), row.unitPrice()))
                            .toList()
                    ),
                    failure.failureCode(),
                    failure.failureMessage(),
                    inboundRecordId
                );
            }
        });

        ExternalOrderCsvImportResponse response = new ExternalOrderCsvImportResponse(
            normalizedDefaultSource,
            csvContent.rows().size(),
            importedOrders.size(),
            failedOrders.size(),
            Instant.now(),
            importedOrders,
            failedOrders
        );

        businessEventService.record(
            BusinessEventType.INTEGRATION_IMPORT_PROCESSED,
            "integration-csv",
            "CSV import processed " + importedOrders.size() + " orders with " + failedOrders.size()
                + " failures from file " + Objects.requireNonNullElse(file.getOriginalFilename(), "orders.csv") + "."
        );

        integrationImportRunService.recordRun(
            resolveBatchSourceSystem(normalizedDefaultSource, importedOrders, failedOrders),
            IntegrationConnectorType.CSV_ORDER_IMPORT,
            Objects.requireNonNullElse(file.getOriginalFilename(), "orders.csv"),
            csvContent.rows().size(),
            importedOrders.size(),
            failedOrders.size(),
            "Processed CSV import with " + importedOrders.size() + " imported orders and "
                + failedOrders.size() + " failures."
        );

        if (importedOrders.isEmpty()) {
            auditLogService.recordFailure(
                "CSV_ORDER_IMPORT",
                "integration-csv",
                "integration-csv",
                "IntegrationImport",
                Objects.requireNonNullElse(file.getOriginalFilename(), "orders.csv"),
                "Imported 0 orders and encountered " + failedOrders.size() + " failures."
            );
        } else {
            auditLogService.recordSuccess(
                "CSV_ORDER_IMPORT",
                "integration-csv",
                "integration-csv",
                "IntegrationImport",
                Objects.requireNonNullElse(file.getOriginalFilename(), "orders.csv"),
                "Imported " + importedOrders.size() + " orders and encountered " + failedOrders.size() + " failures."
            );
        }

        return response;
    }

    private CsvFileContent readCsv(MultipartFile file, String defaultSourceSystem) {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.MISSING_CSV_HEADER, "CSV file must include a header row.");
            }

            CsvHeader header = CsvHeader.parse(parseCsvLine(headerLine));
            List<ParsedCsvRow> rows = new ArrayList<>();
            List<ExternalOrderCsvImportFailure> failures = new ArrayList<>();
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                try {
                    rows.add(toParsedRow(header, values, rowNumber, defaultSourceSystem));
                } catch (ResponseStatusException exception) {
                    var failure = IntegrationFailureCodes.extract(exception);
                    failures.add(new ExternalOrderCsvImportFailure(
                        extractValue(header, values, header.sourceSystemIndex(), defaultSourceSystem),
                        extractValue(header, values, header.externalOrderIdIndex(), null),
                        extractValue(header, values, header.warehouseCodeIndex(), null),
                        List.of(rowNumber),
                        failure.failureCode(),
                        failure.failureMessage()
                    ));
                }
            }
            return new CsvFileContent(rows, failures);
        } catch (IOException exception) {
            throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.MISSING_FILE, "CSV file could not be read.");
        }
    }

    private Map<ImportOrderKey, List<ParsedCsvRow>> groupRows(List<ParsedCsvRow> rows,
                                                              List<ExternalOrderCsvImportFailure> failures) {
        Map<ImportOrderKey, List<ParsedCsvRow>> groupedRows = new LinkedHashMap<>();
        rows.forEach(row -> groupedRows.computeIfAbsent(
            new ImportOrderKey(row.sourceSystem(), row.externalOrderId(), row.warehouseCode()),
            ignored -> new ArrayList<>()).add(row));
        return groupedRows;
    }

    private ParsedCsvRow toParsedRow(CsvHeader header,
                                     List<String> values,
                                     int rowNumber,
                                     String defaultSourceSystem) {
        String sourceSystem = normalizeSourceSystem(requiredValue(header, values, header.sourceSystemIndex(),
            null, null), defaultSourceSystem);
        String externalOrderId = requiredValue(header, values, header.externalOrderIdIndex(),
            IntegrationFailureCode.MISSING_EXTERNAL_ORDER_ID, "externalOrderId is required");
        String warehouseCode = extractValue(header, values, header.warehouseCodeIndex(), null);
        String productSku = requiredValue(header, values, header.productSkuIndex(),
            IntegrationFailureCode.MISSING_PRODUCT_SKU, "productSku is required");
        int quantity = parseQuantity(requiredValue(header, values, header.quantityIndex(),
            IntegrationFailureCode.INVALID_QUANTITY, "quantity is required"), rowNumber);
        BigDecimal unitPrice = parseUnitPrice(requiredValue(header, values, header.unitPriceIndex(),
            IntegrationFailureCode.INVALID_UNIT_PRICE, "unitPrice is required"), rowNumber);
        return new ParsedCsvRow(
            rowNumber,
            sourceSystem,
            externalOrderId.trim(),
            warehouseCode == null ? null : warehouseCode.trim(),
            productSku.trim(),
            quantity,
            unitPrice
        );
    }

    private String requiredValue(CsvHeader header,
                                 List<String> values,
                                 int index,
                                 IntegrationFailureCode failureCode,
                                 String message) {
        String value = extractValue(header, values, index, null);
        if (message == null) {
            return value;
        }
        if (value == null || value.isBlank()) {
            throw IntegrationFailureCodes.badRequest(failureCode == null ? IntegrationFailureCode.UNKNOWN : failureCode, message);
        }
        return value;
    }

    private String extractValue(CsvHeader header, List<String> values, int index, String fallback) {
        if (index < 0 || index >= values.size()) {
            return fallback;
        }
        String value = values.get(index);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeSourceSystem(String sourceSystem, String defaultSourceSystem) {
        String resolvedSourceSystem = (sourceSystem == null || sourceSystem.isBlank()) ? defaultSourceSystem : sourceSystem.trim();
        if (resolvedSourceSystem == null || resolvedSourceSystem.isBlank()) {
            throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.INVALID_SOURCE_SYSTEM,
                "sourceSystem is required in the CSV row or as a request parameter");
        }
        if (!SOURCE_SYSTEM_PATTERN.matcher(resolvedSourceSystem).matches()) {
            throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.INVALID_SOURCE_SYSTEM,
                "sourceSystem may only contain letters, numbers, hyphens, and underscores");
        }
        return resolvedSourceSystem;
    }

    private String normalizeOptionalSourceSystem(String sourceSystem) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            return null;
        }
        return normalizeSourceSystem(sourceSystem, null);
    }

    private int parseQuantity(String rawQuantity, int rowNumber) {
        try {
            int quantity = Integer.parseInt(rawQuantity.trim());
            if (quantity < 1) {
                throw new NumberFormatException("quantity must be positive");
            }
            return quantity;
        } catch (NumberFormatException exception) {
            throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.INVALID_QUANTITY,
                "Row " + rowNumber + " has invalid quantity: " + rawQuantity);
        }
    }

    private BigDecimal parseUnitPrice(String rawUnitPrice, int rowNumber) {
        try {
            BigDecimal unitPrice = new BigDecimal(rawUnitPrice.trim());
            if (unitPrice.signum() <= 0) {
                throw new NumberFormatException("unitPrice must be positive");
            }
            return unitPrice;
        } catch (NumberFormatException exception) {
            throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.INVALID_UNIT_PRICE,
                "Row " + rowNumber + " has invalid unitPrice: " + rawUnitPrice);
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char currentCharacter = line.charAt(index);
            if (currentCharacter == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    currentValue.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (currentCharacter == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue.setLength(0);
            } else {
                currentValue.append(currentCharacter);
            }
        }
        values.add(currentValue.toString().trim());
        return values;
    }

    private String buildIngestionSource(String sourceSystem) {
        return "integration-csv:" + sourceSystem.toLowerCase(Locale.ROOT);
    }

    private String resolveBatchSourceSystem(String normalizedDefaultSource,
                                            List<ExternalOrderCsvImportOrderResult> importedOrders,
                                            List<ExternalOrderCsvImportFailure> failedOrders) {
        if (normalizedDefaultSource != null) {
            return normalizedDefaultSource;
        }

        List<String> sources = new ArrayList<>();
        importedOrders.stream().map(ExternalOrderCsvImportOrderResult::sourceSystem).filter(Objects::nonNull).forEach(sources::add);
        failedOrders.stream().map(ExternalOrderCsvImportFailure::sourceSystem).filter(Objects::nonNull).forEach(sources::add);
        return sources.stream().distinct().limit(2).count() <= 1 && !sources.isEmpty() ? sources.get(0) : "mixed";
    }

    private record CsvFileContent(List<ParsedCsvRow> rows, List<ExternalOrderCsvImportFailure> failures) {
    }

    private record ParsedCsvRow(
        int rowNumber,
        String sourceSystem,
        String externalOrderId,
        String warehouseCode,
        String productSku,
        int quantity,
        BigDecimal unitPrice
    ) {
    }

    private record ImportOrderKey(String sourceSystem, String externalOrderId, String warehouseCode) {
    }

    private record CsvHeader(
        int sourceSystemIndex,
        int externalOrderIdIndex,
        int warehouseCodeIndex,
        int productSkuIndex,
        int quantityIndex,
        int unitPriceIndex
    ) {

        private static CsvHeader parse(List<String> rawHeaderColumns) {
            Map<String, Integer> indexByName = new LinkedHashMap<>();
            for (int index = 0; index < rawHeaderColumns.size(); index++) {
                indexByName.put(rawHeaderColumns.get(index).trim().toLowerCase(Locale.ROOT), index);
            }
            validateRequiredColumn(indexByName, "externalorderid");
            validateRequiredColumn(indexByName, "warehousecode");
            validateRequiredColumn(indexByName, "productsku");
            validateRequiredColumn(indexByName, "quantity");
            validateRequiredColumn(indexByName, "unitprice");
            return new CsvHeader(
                indexByName.getOrDefault("sourcesystem", -1),
                indexByName.get("externalorderid"),
                indexByName.get("warehousecode"),
                indexByName.get("productsku"),
                indexByName.get("quantity"),
                indexByName.get("unitprice")
            );
        }

        private static void validateRequiredColumn(Map<String, Integer> indexByName, String columnName) {
            if (!indexByName.containsKey(columnName)) {
                throw IntegrationFailureCodes.badRequest(IntegrationFailureCode.MISSING_HEADER_COLUMN,
                    "CSV header must include column " + columnName);
            }
        }
    }
}
