package com.github.topikachu.keycloak.spring.support.config;

import com.github.topikachu.keycloak.spring.support.filter.KeycloakRequestFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import javax.servlet.Filter;
import java.util.Arrays;


@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
@Slf4j
@EnableConfigurationProperties({KeycloakProperties.class, KeycloakCustomProperties.class})
public class KeycloakConfig {
    final static String classes[] = {
            "org.keycloak.models.jpa.entities.ClientEntity",
            "org.keycloak.models.jpa.entities.ClientAttributeEntity",
            "org.keycloak.models.jpa.entities.CredentialEntity",
            "org.keycloak.models.jpa.entities.RealmEntity",
            "org.keycloak.models.jpa.entities.RealmAttributeEntity",
            "org.keycloak.models.jpa.entities.RequiredCredentialEntity",
            "org.keycloak.models.jpa.entities.ComponentConfigEntity",
            "org.keycloak.models.jpa.entities.ComponentEntity",
            "org.keycloak.models.jpa.entities.UserFederationProviderEntity",
            "org.keycloak.models.jpa.entities.UserFederationMapperEntity",
            "org.keycloak.models.jpa.entities.RoleEntity",
            "org.keycloak.models.jpa.entities.RoleAttributeEntity",
            "org.keycloak.models.jpa.entities.FederatedIdentityEntity",
            "org.keycloak.models.jpa.entities.MigrationModelEntity",
            "org.keycloak.models.jpa.entities.UserEntity",
            "org.keycloak.models.jpa.entities.RealmLocalizationTextsEntity",
            "org.keycloak.models.jpa.entities.UserRequiredActionEntity",
            "org.keycloak.models.jpa.entities.UserAttributeEntity",
            "org.keycloak.models.jpa.entities.UserRoleMappingEntity",
            "org.keycloak.models.jpa.entities.IdentityProviderEntity",
            "org.keycloak.models.jpa.entities.IdentityProviderMapperEntity",
            "org.keycloak.models.jpa.entities.ProtocolMapperEntity",
            "org.keycloak.models.jpa.entities.UserConsentEntity",
            "org.keycloak.models.jpa.entities.UserConsentClientScopeEntity",
            "org.keycloak.models.jpa.entities.AuthenticationFlowEntity",
            "org.keycloak.models.jpa.entities.AuthenticationExecutionEntity",
            "org.keycloak.models.jpa.entities.AuthenticatorConfigEntity",
            "org.keycloak.models.jpa.entities.RequiredActionProviderEntity",
            "org.keycloak.models.jpa.session.PersistentUserSessionEntity",
            "org.keycloak.models.jpa.session.PersistentClientSessionEntity",
            "org.keycloak.models.jpa.entities.GroupEntity",
            "org.keycloak.models.jpa.entities.GroupAttributeEntity",
            "org.keycloak.models.jpa.entities.GroupRoleMappingEntity",
            "org.keycloak.models.jpa.entities.UserGroupMembershipEntity",
            "org.keycloak.models.jpa.entities.ClientScopeEntity",
            "org.keycloak.models.jpa.entities.ClientScopeAttributeEntity",
            "org.keycloak.models.jpa.entities.ClientScopeRoleMappingEntity",
            "org.keycloak.models.jpa.entities.ClientScopeClientMappingEntity",
            "org.keycloak.models.jpa.entities.DefaultClientScopeRealmMappingEntity",
            "org.keycloak.models.jpa.entities.ClientInitialAccessEntity",
//            < !--JpaAuditProviders-- >
            "org.keycloak.events.jpa.EventEntity",
            "org.keycloak.events.jpa.AdminEventEntity",
//        <!--Authorization -->
            "org.keycloak.authorization.jpa.entities.ResourceServerEntity",
            "org.keycloak.authorization.jpa.entities.ResourceEntity",
            "org.keycloak.authorization.jpa.entities.ScopeEntity",
            "org.keycloak.authorization.jpa.entities.PolicyEntity",
            "org.keycloak.authorization.jpa.entities.PermissionTicketEntity",
            "org.keycloak.authorization.jpa.entities.ResourceAttributeEntity",
//        <!--
//    User Federation
//    Storage -->
            "org.keycloak.storage.jpa.entity.BrokerLinkEntity",
            "org.keycloak.storage.jpa.entity.FederatedUser",
            "org.keycloak.storage.jpa.entity.FederatedUserAttributeEntity",
            "org.keycloak.storage.jpa.entity.FederatedUserConsentEntity",
            "org.keycloak.storage.jpa.entity.FederatedUserConsentClientScopeEntity",
            "org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity",
            "org.keycloak.storage.jpa.entity.FederatedUserGroupMembershipEntity",
            "org.keycloak.storage.jpa.entity.FederatedUserRequiredActionEntity",
            "org.keycloak.storage.jpa.entity.FederatedUserRoleMappingEntity"
    };


    @Bean
    @ConditionalOnMissingBean(name = "keycloakEntitiesCustomizer")
    protected EntityManagerFactoryBuilderCustomizer keycloakEntitiesCustomizer() {
        return builder -> {
            builder.setPersistenceUnitPostProcessors(pui -> {
                if (!pui.getPersistenceUnitName().equals("keycloak-default")) {
                    Arrays.asList(classes)
                            .forEach(pui::addManagedClassName);
                }
            });
        };
    }

    @Bean
    @ConditionalOnMissingBean(name = "keycloakSessionManagement")
    protected FilterRegistrationBean<Filter> keycloakSessionManagement(KeycloakCustomProperties customProperties) {
        FilterRegistrationBean<Filter> filter = new FilterRegistrationBean<>();
        filter.setName("Keycloak Session Management");
        filter.setFilter(new KeycloakRequestFilter());
        filter.addUrlPatterns(customProperties.getServer().getKeycloakPath() + "/*");
        return filter;
    }


}
