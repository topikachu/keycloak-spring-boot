package com.github.topikachu.keycloak.spring.support.filter;

import org.keycloak.common.ClientConnection;
import org.keycloak.services.filters.AbstractRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

public class KeycloakRequestFilter extends AbstractRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws UnsupportedEncodingException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        request.setCharacterEncoding("UTF-8");

        org.keycloak.common.ClientConnection clientConnection = createClientConnection(request);
        filter(clientConnection, (session) -> {
            try {
                servletRequest.setAttribute("KEYCLOAK_CLIENT_CONNECTION", clientConnection);
                servletRequest.setAttribute("KEYCLOAK_SESSION",session);
                filterChain.doFilter(servletRequest, servletResponse);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected org.keycloak.common.ClientConnection createClientConnection(HttpServletRequest request) {
        return new ClientConnection(request);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // NOOP
    }

    @Override
    public void destroy() {
        // NOOP
    }

    public static class ClientConnection implements org.keycloak.common.ClientConnection {

        private final HttpServletRequest request;

        public ClientConnection(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public String getRemoteAddr() {
            String forwardedFor = request.getHeader("X-Forwarded-For");

            if (forwardedFor != null) {
                return forwardedFor;
            }

            return request.getRemoteAddr();
        }

        @Override
        public String getRemoteHost() {
            return request.getRemoteHost();
        }

        @Override
        public int getRemotePort() {
            return request.getRemotePort();
        }

        @Override
        public String getLocalAddr() {
            return request.getLocalAddr();
        }

        @Override
        public int getLocalPort() {
            return request.getLocalPort();
        }
    }
}
