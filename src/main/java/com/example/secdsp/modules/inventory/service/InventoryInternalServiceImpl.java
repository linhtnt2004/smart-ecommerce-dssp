package com.example.secdsp.modules.inventory.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ErrorCode;
import com.example.secdsp.modules.inventory.entity.Inventory;
import com.example.secdsp.modules.inventory.entity.InventoryLog;
import com.example.secdsp.modules.inventory.entity.InventoryLogReason;
import com.example.secdsp.modules.inventory.repository.InventoryLogRepository;
import com.example.secdsp.modules.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryInternalServiceImpl implements InventoryInternalService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Override
    @Transactional
    public void reserveForOrder(Long productId, int quantity) {

        if (quantity <= 0) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Quantity must be greater than 0."
            );
        }

        Inventory inventory = inventoryRepository
            .findByProduct_IdForUpdate(productId)
            .orElseThrow(() ->
                             new BusinessException(
                                 ErrorCode.RESOURCE_NOT_FOUND,
                                 "Inventory not found for product " + productId
                             )
            );

        if (inventory.getAvailableQuantity() < quantity) {
            log.warn(
                "Insufficient stock for product {}. Available: {}, Requested: {}",
                productId,
                inventory.getAvailableQuantity(),
                quantity
            );

            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Insufficient stock for product ID: " + productId
            );
        }

        int previousAvailable = inventory.getAvailableQuantity();
        int previousReserved = inventory.getReservedQuantity();

        inventory.setAvailableQuantity(previousAvailable - quantity);
        inventory.setReservedQuantity(previousReserved + quantity);

        insertLog(
            inventory,
            -quantity,
            previousAvailable,
            inventory.getAvailableQuantity(),
            InventoryLogReason.ORDER
        );

        log.info(
            "Reserved {} units for product {}",
            quantity,
            productId
        );
    }

    @Override
    @Transactional
    public void releaseForCancel(Long productId, int quantity) {

        if (quantity <= 0) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Quantity must be greater than 0."
            );
        }

        Inventory inventory = inventoryRepository
            .findByProduct_IdForUpdate(productId)
            .orElseThrow(() ->
                             new BusinessException(
                                 ErrorCode.RESOURCE_NOT_FOUND,
                                 "Inventory not found for product " + productId
                             )
            );

        int previousAvailable = inventory.getAvailableQuantity();
        int previousReserved = inventory.getReservedQuantity();

        if (previousReserved < quantity) {
            log.error(
                "Reserved quantity inconsistent for product {}. Reserved: {}, Requested release: {}",
                productId,
                previousReserved,
                quantity
            );

            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Reserved quantity is inconsistent for product ID: " + productId
            );
        }

        inventory.setAvailableQuantity(previousAvailable + quantity);
        inventory.setReservedQuantity(previousReserved - quantity);

        insertLog(
            inventory,
            quantity,
            previousAvailable,
            inventory.getAvailableQuantity(),
            InventoryLogReason.ORDER_CANCEL
        );

        log.info(
            "Released {} units for product {}",
            quantity,
            productId
        );
    }

    private void insertLog(
        Inventory inventory,
        int changeAmount,
        int previousAvailable,
        int currentAvailable,
        InventoryLogReason reason
    ) {

        InventoryLog logEntry = InventoryLog.builder()
            .product(inventory.getProduct())
            .changeAmount(changeAmount)
            .previousQuantity(previousAvailable)
            .currentQuantity(currentAvailable)
            .reason(reason)
            .build();

        inventoryLogRepository.save(logEntry);
    }
}