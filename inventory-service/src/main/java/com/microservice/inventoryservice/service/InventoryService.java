package com.microservice.inventoryservice.service;


import com.microservice.inventoryservice.dto.InventoryResponse;
import com.microservice.inventoryservice.model.Inventory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.microservice.inventoryservice.repository.InventoryRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

  private final InventoryRepository inventoryRepository;

  @SneakyThrows
  @Transactional(readOnly = true)
  public List<InventoryResponse> isInStock(List<String> skuCode) {
    return inventoryRepository.findBySkuCodeIn(skuCode).stream()
        .map(inventory ->
          InventoryResponse.builder()
              .skuCode(inventory.getSkuCode())
              .isInStock(inventory.getQuantity() > 0)
              .build()
        ).toList();
  }
}
