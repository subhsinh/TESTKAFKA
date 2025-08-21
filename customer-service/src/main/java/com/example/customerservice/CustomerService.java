package com.example.customerservice;

import java.util.HashMap;
import java.util.Map;

public class CustomerService {

    private final Map<String, Customer> customerMap = new HashMap<>();

    /**
     * Register a new customer.
     */
    public synchronized void registerCustomer(String id, String name, String email) {
        if (customerMap.containsKey(id))
            throw new IllegalStateException("Customer with this ID already exists");
        customerMap.put(id, new Customer(id, name, email));
    }

    /**
     * Update name/email for a given customer.
     */
    public synchronized void updateCustomer(String id, String name, String email) {
        Customer c = customerMap.get(id);
        if (c == null)
            throw new IllegalArgumentException("Customer not found");
        c.setName(name);
        c.setEmail(email);
    }

    /**
     * Return a Customer; throw if not found.
     */
    public synchronized Customer getCustomer(String id) {
        Customer c = customerMap.get(id);
        if (c == null)
            throw new IllegalArgumentException("Customer not found");
        return c;
    }

    /**
     * Add loyalty points.
     */
    public synchronized void addLoyaltyPoints(String id, int points) {
        Customer c = customerMap.get(id);
        if (c == null)
            throw new IllegalArgumentException("Customer not found");
        c.addLoyaltyPoints(points);
    }
    
    /**
     * Does customer exist.
     */
    public boolean exists(String id) {
        return customerMap.containsKey(id);
    }
    
    /**
     * For advanced tests: expose all customers.
     */
    Map<String, Customer> getAllCustomers() {
        return customerMap;
    }
}
