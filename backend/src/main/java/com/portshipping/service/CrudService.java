package com.portshipping.service;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;

/** Small common CRUD implementation used by the resource controllers. */
@Service
public class CrudService {
    private final EntityManager em;
    public CrudService(EntityManager em) { this.em = em; }

    public <T> Page<T> page(JpaRepository<T,Long> repo, int page, int size) {
        return repo.findAll(PageRequest.of(Math.max(0,page), Math.min(100,Math.max(1,size)),
                Sort.by("id").ascending()));
    }
    public <T> T get(JpaRepository<T,Long> repo, Long id, String name) {
        return repo.findById(id).orElseThrow(() -> new com.portshipping.exception.NotFoundException(name+" "+id+" not found"));
    }
    public <T> T create(JpaRepository<T,Long> repo, Class<T> type, Map<String,Object> values) {
        try {
            Constructor<T> c=type.getDeclaredConstructor(); c.setAccessible(true);
            T entity=c.newInstance(); apply(entity, values); return repo.save(entity);
        } catch (ReflectiveOperationException e) { throw new IllegalArgumentException("Invalid "+type.getSimpleName()+" payload",e); }
    }
    public <T> T update(JpaRepository<T,Long> repo, T entity, Map<String,Object> values) { apply(entity,values); return repo.save(entity); }
    private void apply(Object entity, Map<String,Object> values) {
        if (values==null) return;
        values.forEach((key,value)-> {
            if ("id".equalsIgnoreCase(key) || value==null) return;
            String field=key;
            while(field.contains("_")||field.contains("-")) {
                int i=field.indexOf("_"); int j=field.indexOf("-");
                if(i<0 || (j>=0 && j<i)) i=j;
                if(i+1<field.length()) field=field.substring(0,i)+
                        Character.toUpperCase(field.charAt(i+1))+field.substring(i+2);
                else field=field.substring(0,i);
            }
            try {
                Field f=null; Class<?> c=entity.getClass();
                while(c!=null) { try { f=c.getDeclaredField(field); break; } catch(NoSuchFieldException e){c=c.getSuperclass();} }
                if(f==null || Modifier.isStatic(f.getModifiers())) {
                    throw new IllegalArgumentException("Unknown field '" + key + "'");
                }
                f.setAccessible(true); Class<?> target=f.getType();
                Object converted=value;
                if (target.isEnum()) converted=Enum.valueOf((Class)target,value.toString().toUpperCase());
                else if(target==Long.class||target==long.class) converted=Long.valueOf(value.toString());
                else if(target==Integer.class||target==int.class) converted=Integer.valueOf(value.toString());
                else if(target==Double.class||target==double.class) converted=Double.valueOf(value.toString());
                else if(target==java.math.BigDecimal.class) converted=new java.math.BigDecimal(value.toString());
                else if(target==LocalDate.class) converted=LocalDate.parse(value.toString());
                else if(target==LocalDateTime.class) converted=LocalDateTime.parse(value.toString());
                else if(target==Instant.class) converted=Instant.parse(value.toString());
                else if(target!=String.class && value instanceof Map m && m.get("id")!=null) converted=em.getReference(target,Long.valueOf(m.get("id").toString()));
                f.set(entity,converted);
            } catch (Exception e) { throw new IllegalArgumentException("Invalid field '"+key+"'",e); }
        });
    }
}
