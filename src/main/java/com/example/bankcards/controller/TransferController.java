package com.example.bankcards.controller;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/api/v1/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer money between current user's cards")
    @PreAuthorize("hasPermission(null, @permission.TRANSFER_CREATE)")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transferService.transfer(request);
    }

    @GetMapping("/api/v1/transfers/my")
    @Operation(summary = "List current user's transfers")
    @PreAuthorize("hasPermission(null, @permission.TRANSFER_VIEW_OWN)")
    public Page<TransferResponse> listMy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return transferService.listMy(from, to, pageable);
    }

    @GetMapping("/api/v1/transfers/{id}")
    @Operation(summary = "Get current user's transfer")
    @PreAuthorize("hasPermission(null, @permission.TRANSFER_VIEW_OWN)")
    public TransferResponse getMy(@PathVariable UUID id) {
        return transferService.getMy(id);
    }

    @GetMapping("/api/v1/admin/transfers")
    @Operation(summary = "List all transfers as admin")
    @PreAuthorize("hasPermission(null, @permission.TRANSFER_VIEW_ALL)")
    public Page<TransferResponse> listAll(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return transferService.listAll(userId, from, to, pageable);
    }
}
