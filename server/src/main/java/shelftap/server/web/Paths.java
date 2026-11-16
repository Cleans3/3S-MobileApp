package shelftap.server.web;

import jakarta.servlet.http.HttpServletRequest;

public final class Paths {
    private Paths() {
    }

    /**
     * The decoded request path, so /api/auth/lo%67in matches like /api/auth/login. Servlet path plus path info works
     * under Tomcat (everything in servletPath) and under MockMvc (everything in pathInfo).
     */
    public static String of(HttpServletRequest request) {
        String info = request.getPathInfo();
        return request.getServletPath() + (info == null ? "" : info);
    }
}
