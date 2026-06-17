package com.hospital.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptLineItem {

    private Integer srNo;
    private String particulars;
    private BigDecimal amount;
}

