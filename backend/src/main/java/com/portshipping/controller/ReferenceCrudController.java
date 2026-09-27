package com.portshipping.controller;

import com.portshipping.entity.*;
import com.portshipping.repository.InvoiceItemRepository;
import com.portshipping.service.CrudService;
import jakarta.validation.Valid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * CRUD endpoints for remaining auxiliary reference entities.
 */
@RestController
@RequestMapping("/api")
public class ReferenceCrudController {
    private final CrudService crud;
    private final Map<String, JpaRepository<?, Long>> repos = new HashMap<>();
    private final Map<String, Class<?>> types = new HashMap<>();

    public ReferenceCrudController(CrudService c, InvoiceItemRepository ii) {
        crud = c;
        put("invoice-items", ii, InvoiceItem.class);
    }

    private void put(String n, JpaRepository<?, Long> r, Class<?> t) { repos.put(n, r); types.put(n, t); }
    private JpaRepository repo(String n) { JpaRepository r = repos.get(n); if (r == null) throw new com.portshipping.exception.NotFoundException("Unknown resource: " + n); return r; }
    private Class type(String n) { Class t = types.get(n); if (t == null) throw new com.portshipping.exception.NotFoundException("Unknown resource: " + n); return t; }

    @GetMapping("/ref/{resource}")
    public Object all(@PathVariable String resource, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return crud.page(repo(resource), page, size);
    }

    @GetMapping("/ref/{resource}/{id}")
    public Object one(@PathVariable String resource, @PathVariable Long id) {
        return crud.get(repo(resource), id, type(resource).getSimpleName());
    }

    @PostMapping("/ref/{resource}")
    public ResponseEntity<?> create(@PathVariable String resource, @Valid @RequestBody Map<String, Object> body) {
        if (body == null || body.isEmpty()) throw new IllegalArgumentException("Request body must not be empty");
        return ResponseEntity.status(HttpStatus.CREATED).body(crud.create(repo(resource), type(resource), body));
    }
}
