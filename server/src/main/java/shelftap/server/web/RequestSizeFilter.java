package shelftap.server.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/** Caps request bodies: a declared Content-Length over the limit is refused, and a chunked body is cut off while it is read. */
public class RequestSizeFilter extends OncePerRequestFilter {
    private final long maxBytes;
    private final JsonMapper mapper;

    public RequestSizeFilter(long maxBytes, JsonMapper mapper) {
        this.maxBytes = maxBytes;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maxBytes) {
            Problems.write(response, mapper, ErrorCode.VALIDATION_FAILED, 413, "request body too large", null);
            return;
        }
        chain.doFilter(new Limited(request, maxBytes), response);
    }

    private static final class Limited extends HttpServletRequestWrapper {
        private final long max;

        Limited(HttpServletRequest request, long max) {
            super(request);
            this.max = max;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream in = super.getInputStream();
            return new ServletInputStream() {
                private long read;

                @Override
                public int read() throws IOException {
                    int b = in.read();
                    if (b >= 0 && ++read > max) {
                        throw new IOException("request body too large");
                    }
                    return b;
                }

                @Override
                public boolean isFinished() {
                    return in.isFinished();
                }

                @Override
                public boolean isReady() {
                    return in.isReady();
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    in.setReadListener(listener);
                }
            };
        }
    }
}
