package br.pucpr.authserver.products

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class InsufficientStockException(productName: String, available: Int) : RuntimeException(
    "Not enough stock for '$productName'. Available: $available"
)
