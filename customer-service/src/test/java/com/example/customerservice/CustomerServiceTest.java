package com.example.customerservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceTest {

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService();
    }

    @Test
    void registerAndLookupCustomer() {
        customerService.registerCustomer("c1", "Alice", "alice@example.com");
        Customer c = customerService.getCustomer("c1");
        assertEquals("Alice", c.getName());
        assertEquals("alice@example.com", c.getEmail());
        assertEquals(0, c.getLoyaltyPoints());
    }

    @Test
    void duplicateRegisterThrows() {
        customerService.registerCustomer("c2", "Bob", "bob@example.com");
        assertThrows(IllegalStateException.class, () -> customerService.registerCustomer("c2", "Bobby", "bobby@example.com"));
    }

    @Test
    void updateCustomerInfo() {
        customerService.registerCustomer("c3", "Charlie", "c@example.com");
        customerService.updateCustomer("c3", "Charles", "charles@example.com");
        Customer c = customerService.getCustomer("c3");
        assertEquals("Charles", c.getName());
        assertEquals("charles@example.com", c.getEmail());
    }

    @Test
    void getCustomerThrowsOnMissing() {
        assertThrows(IllegalArgumentException.class, () -> customerService.getCustomer("none"));
    }

    @Test
    void existsReturnsTrueForPresentFalseForAbsent() {
        customerService.registerCustomer("c4", "Deb", "d@ex.com");
        assertTrue(customerService.exists("c4"));
        assertFalse(customerService.exists("cNotHere"));
    }

    @Test
    void addLoyaltyPointsHappyCaseAndNegative() {
        customerService.registerCustomer("c5", "Eve", "eve@ex.com");
        customerService.addLoyaltyPoints("c5", 20);
        assertEquals(20, customerService.getCustomer("c5").getLoyaltyPoints());
        assertThrows(IllegalArgumentException.class, () -> customerService.addLoyaltyPoints("c5", -1));
    }

    @Test
    void addLoyaltyPointsMissingCustomer() {
        assertThrows(IllegalArgumentException.class, () -> customerService.addLoyaltyPoints("badUser", 5));
    }

    @Test
    void updateCustomerThrowsOnMissing() {
        assertThrows(IllegalArgumentException.class, () -> customerService.updateCustomer("ghost", "Name", "email@x.com"));
    }

    @Test
    void registerManyAndList() {
        for (int i = 0; i < 100; ++i) {
            customerService.registerCustomer(Integer.toString(i), "Name"+i, "e"+i+"@x.com");
        }
        assertEquals(100, customerService.getAllCustomers().size());
    }

    // Edge & stress: rapid update, loyalty, and existence checks
    @Test
    void fuzzCustomerStress() {
        for (int i = 0; i < 50; ++i) {
            customerService.registerCustomer("f"+i, "Fuzz"+i, "email"+i+"@x.com");
            customerService.addLoyaltyPoints("f"+i, i+1);
            customerService.updateCustomer("f"+i, "Fuzzed"+i, "good"+i+"@x.com");
            assertEquals("good"+i+"@x.com", customerService.getCustomer("f"+i).getEmail());
        }
    }
}
