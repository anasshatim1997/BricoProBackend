package com.bricopro.auth.repository;

import com.bricopro.auth.entity.OAuth2Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuth2AccountRepository extends JpaRepository<OAuth2Account, Long> {
    Optional<OAuth2Account> findByProviderAndProviderUserId(OAuth2Account.Provider provider, String providerUserId);
}
