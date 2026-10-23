package com.frauddetection.controller;

import com.frauddetection.dto.request.TransactionRequest;
import com.frauddetection.dto.response.PagedResponse;
import com.frauddetection.dto.response.RiskAssessmentResponse;
import com.frauddetection.dto.response.TransactionResponse;
import com.frauddetection.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management and risk assessment execution")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Submit a new transaction",
               description = "Creates a transaction, runs all risk rules through the RiskEngine, and returns decision.")
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a transaction by ID")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all transactions for a user",
               description = "Authorized for resource owner or ADMIN / ANALYST.")
    public ResponseEntity<PagedResponse<TransactionResponse>> getUserTransactions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<TransactionResponse> response = transactionService.getTransactionsByUser(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionTimestamp")));
        return ResponseEntity.ok(response);
    }
}
