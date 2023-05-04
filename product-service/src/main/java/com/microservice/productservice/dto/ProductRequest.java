package com.microservice.productservice.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ProductRequest {

  private String name;
  private String description;
  private BigDecimal price;

}
