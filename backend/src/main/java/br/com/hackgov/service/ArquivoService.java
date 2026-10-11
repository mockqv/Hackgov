package br.com.hackgov.service;

import br.com.hackgov.exception.BusinessException;
import br.com.hackgov.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Upload real das fotos "antes" e "depois" (Fase 6).
 *
 * Antes, o front-end convertia a imagem em data-URL base64 e enviava esse texto
 * no campo caminhoFotoAntes/Depois — que no banco é VARCHAR2(300). Qualquer
 * foto real passava de 300 caracteres e a validação recusava o envio (e, sem
 * foto, o servidor não conseguia concluir um chamado). Agora o arquivo vai por
 * multipart para este serviço, é gravado em disco com nome aleatório e o banco
 * guarda só o caminho curto (/api/arquivos/{uuid}.jpg).
 *
 * Proteções: só JPG/PNG/WEBP, conferindo a assinatura binária ("magic bytes"),
 * e não só o Content-Type informado pelo cliente; limite de 5 MB; nome gerado
 * pelo servidor (UUID) e validado por expressão regular na leitura, o que
 * impede path traversal (../) e enumeração de arquivos.
 */
@Service
public class ArquivoService {

    private static final Logger log = LoggerFactory.getLogger(ArquivoService.class);

    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSAO_POR_TIPO = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private static final Pattern NOME_VALIDO = Pattern.compile("^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private final Path diretorio;

    public ArquivoService(@Value("${app.uploads.dir:uploads}") String diretorio) {
        this.diretorio = Paths.get(diretorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.diretorio);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar o diretório de uploads " + this.diretorio, e);
        }
    }

    /** Grava a imagem e devolve o caminho público a ser salvo na solicitação. */
    public String salvarImagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw new BusinessException("Selecione uma imagem");
        if (arquivo.getSize() > TAMANHO_MAXIMO) throw new BusinessException("Imagem maior que 5 MB");

        String extensao = EXTENSAO_POR_TIPO.get(arquivo.getContentType());
        if (extensao == null) throw new BusinessException("Formato não suportado: envie JPG, PNG ou WEBP");

        try {
            byte[] conteudo = arquivo.getBytes();
            if (!assinaturaConfere(conteudo, extensao)) {
                throw new BusinessException("O conteúdo do arquivo não corresponde a uma imagem " + extensao.toUpperCase());
            }
            String nome = UUID.randomUUID() + "." + extensao;
            Files.write(diretorio.resolve(nome), conteudo);
            log.info("Imagem recebida: {} ({} KB)", nome, conteudo.length / 1024);
            return "/api/arquivos/" + nome;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar a imagem", e);
        }
    }

    public Resource carregar(String nome) {
        if (nome == null || !NOME_VALIDO.matcher(nome).matches()) {
            throw new NotFoundException("Arquivo não encontrado");
        }
        Path caminho = diretorio.resolve(nome).normalize();
        if (!caminho.startsWith(diretorio) || !Files.isRegularFile(caminho)) {
            throw new NotFoundException("Arquivo não encontrado");
        }
        return new FileSystemResource(caminho);
    }

    public MediaType tipoDe(String nome) {
        if (nome.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (nome.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    /** Confere os primeiros bytes do arquivo contra a assinatura de cada formato. */
    static boolean assinaturaConfere(byte[] b, String extensao) {
        return switch (extensao) {
            case "jpg" -> b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "png" -> b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "webp" -> b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }
}
