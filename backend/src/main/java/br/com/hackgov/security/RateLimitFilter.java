package br.com.hackgov.security;

import br.com.hackgov.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limiting em memória para os endpoints de autenticação (/api/auth/login e
 * /api/auth/register) — únicos pontos de entrada não autenticados por onde um
 * atacante pode tentar força bruta de credenciais ou automatizar cadastros em massa.
 *
 * Estrutura de dados: uma fila (Deque usado como Queue, FIFO) de timestamps por IP —
 * "sliding window". A cada requisição, descarta do início da fila os timestamps mais
 * antigos que a janela e só então conta quantos restaram; se o limite já foi atingido,
 * a requisição é recusada com 429 antes de chegar ao controller.
 *
 * Roda antes do Spring Security decidir autenticação/autorização (registrado no
 * SecurityFilterChain), por isso escreve a resposta de erro diretamente — o mesmo
 * padrão já usado pelo authenticationEntryPoint/accessDeniedHandler em SecurityConfig,
 * já que uma exceção lançada aqui não chegaria ao GlobalExceptionHandler (que só trata
 * exceções de dentro do DispatcherServlet, depois deste filtro).
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> ROTAS_LIMITADAS = Set.of("/api/auth/login", "/api/auth/register");
    private static final int MAX_TENTATIVAS = 5;
    private static final long JANELA_MS = 60_000; // 60 segundos

    private final ObjectMapper mapper;
    private final ConcurrentHashMap<String, Deque<Long>> tentativasPorIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        boolean rotaLimitada = HttpMethod.POST.matches(req.getMethod())
                && ROTAS_LIMITADAS.contains(req.getRequestURI());

        if (!rotaLimitada) {
            chain.doFilter(req, res);
            return;
        }

        String ip = req.getRemoteAddr();
        Deque<Long> tentativas = tentativasPorIp.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        long agora = System.currentTimeMillis();

        // Descarta do início da fila (mais antigas primeiro) tudo fora da janela.
        Long maisAntiga;
        while ((maisAntiga = tentativas.peekFirst()) != null && agora - maisAntiga > JANELA_MS) {
            tentativas.pollFirst();
        }

        if (tentativas.size() >= MAX_TENTATIVAS) {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write(mapper.writeValueAsString(
                    ApiResponse.error("Muitas tentativas. Aguarde um minuto antes de tentar novamente.")));
            return;
        }

        tentativas.addLast(agora);
        chain.doFilter(req, res);
    }
}
