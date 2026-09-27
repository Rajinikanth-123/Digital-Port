package com.portshipping.repository; import com.portshipping.entity.Role; import org.springframework.data.jpa.repository.JpaRepository;  import java.util.*; public interface RoleRepository extends JpaRepository<Role,Long>{Optional<Role> findByNameIgnoreCase(String name); boolean existsByNameIgnoreCase(String name);}

