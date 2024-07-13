package user_and_auth_microservice.config;

// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.Cookie;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.HttpHeaders;
// import org.springframework.web.filter.OncePerRequestFilter;

// import java.io.IOException;
// import java.util.Arrays;

// @RequiredArgsConstructor
// public class JwtAuthFilter extends OncePerRequestFilter {

//     private final UserAuthProvider userAuthProvider;

//     @Override
//     protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//             throws ServletException, IOException {
//         String token = null;

//         // Check for token in cookies first
//         Cookie[] cookies = request.getCookies();
//         if (cookies != null) {
//             token = Arrays.stream(cookies)
//                     .filter(cookie -> "JWT".equals(cookie.getName()))
//                     .map(Cookie::getValue)
//                     .findFirst()
//                     .orElse(null);
//         }

//         // If no token found in cookies, check the Authorization header
//         if (token == null) {
//             String header = request.getHeader(HttpHeaders.AUTHORIZATION);
//             if (header != null) {
//                 String[] authElements = header.split(" ");
//                 if (authElements.length == 2 && "Bearer".equals(authElements[0])) {
//                     token = authElements[1];
//                 }
//             }
//         }

//         // Validate the token if present
//         if (token != null) {
//             try {
//                 var authentication = userAuthProvider.validateToken(token);
//                 SecurityContextHolder.getContext().setAuthentication(authentication);

//                 // Refresh the token if it is about to expire
//                 String refreshedToken = userAuthProvider.refreshToken(token);
//                 if (!refreshedToken.equals(token)) {
//                     Cookie jwtCookie = new Cookie("JWT", refreshedToken);
//                     jwtCookie.setHttpOnly(true);
//                     jwtCookie.setSecure(true);
//                     jwtCookie.setPath("/");
//                     jwtCookie.setMaxAge(10803);
//                     response.setHeader("newToken", refreshedToken);
//                     response.addCookie(jwtCookie);
//                 }

//             } catch (RuntimeException e) {
//                 SecurityContextHolder.clearContext();
//             }
//         }

//         filterChain.doFilter(request, response);
//     }
// }
