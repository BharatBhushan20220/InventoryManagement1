package com.example.InventoryManagement;

import com.example.InventoryManagement.controller.Controller;
import com.example.InventoryManagement.entity.Items;
import com.example.InventoryManagement.service.ServiceImplementation;
import com.example.InventoryManagement.service.ServiceInterface;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@WebMvcTest(Controller.class)
@AutoConfigureMockMvc
public class ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ServiceInterface serviceInterface;

    @Test
    void testCreateItem_returnsCreatedItem() throws Exception {
        Items item = Items.builder()
                .id(1L)
                .name("Mouse")
                .sku("SKU002")
                .quantity(100)
                .reservedQuantity(0)
                .price(499.99)
                .build();

        when(serviceInterface.createItem(any(Items.class))).thenReturn(item);

        String requestJson = """
                {
                  "name": "Mouse",
                  "sku": "SKU002",
                  "quantity": 100,
                  "price": 499.99
                }
                """;

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mouse"));
    }
}
