package com.wms.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CharsetFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (!httpResponse.containsHeader("Content-Type")) {
            httpResponse.setHeader("Content-Type", "application/json; charset=UTF-8");
        }
        httpResponse.setCharacterEncoding("UTF-8");
        chain.doFilter(request, response);
    }
}
