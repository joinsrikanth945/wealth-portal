package com.yourbank.wealth.client;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByRmUsernameOrderByLastNameAsc(String rmUsername);

    List<Client> findAllByOrderByLastNameAsc();

    Optional<Client> findByPortalUsername(String portalUsername);
}
