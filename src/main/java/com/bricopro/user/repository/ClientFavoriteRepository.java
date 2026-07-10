package com.bricopro.user.repository;

import com.bricopro.user.entity.ClientFavorite;
import com.bricopro.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ClientFavoriteRepository extends JpaRepository<ClientFavorite, Long> {
    List<ClientFavorite> findByClient(User client);
    boolean existsByClientAndWorker(User client, User worker);
    void deleteByClientAndWorker(User client, User worker);
}