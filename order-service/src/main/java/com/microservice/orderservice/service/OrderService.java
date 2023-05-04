package com.microservice.orderservice.service;

import com.microservice.orderservice.dto.InventoryResponse;
import com.microservice.orderservice.dto.OrderLineItemsDto;
import com.microservice.orderservice.dto.OrderRequest;
import com.microservice.orderservice.event.OrderPlacedEvent;
import com.microservice.orderservice.model.Order;
import com.microservice.orderservice.model.OrderLineItems;
import com.microservice.orderservice.repository.OrderRepository;
import java.util.Arrays;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

  private final OrderRepository orderRepository;

  private final WebClient.Builder webClientBuilder;

  private KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;

  public String placeOrder(OrderRequest orderRequest){
    Order order = new Order();
    order.setOrderNumber(UUID.randomUUID().toString());

    List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
        .stream()
        .map(orderLineItemsDto -> mapToDto(orderLineItemsDto))
        .toList();

    order.setOrderLineItemsList(orderLineItems);

    List<String> skuCodeList = order.getOrderLineItemsList().stream()
        .map(OrderLineItems::getSkuCode)
        .toList();
    // Check inventory service and place an order if product is in stock.
    InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
        .uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("skuCode", skuCodeList).build())
        .retrieve()
        .bodyToMono(InventoryResponse[].class)
        .block();//block is used to make Synchronous call, webclient call is Asynchronous by default.

    boolean allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);
    if (allProductsInStock){
      orderRepository.save(order);
      kafkaTemplate.send("notificationTopic",new OrderPlacedEvent(order.getOrderNumber()));
      return "Order Placed";
    }else{
      throw new IllegalArgumentException("Product is not in stock. Please try again later.");
    }
  }

  private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
    OrderLineItems orderLineItems = new OrderLineItems();
    orderLineItems.setPrice(orderLineItemsDto.getPrice());
    orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
    orderLineItems.setQuantity(orderLineItemsDto.getQuantity());

    return orderLineItems;
  }


}
