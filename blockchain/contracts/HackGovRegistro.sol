// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

/// @title  HackGov — registro público e auditável dos chamados de zeladoria
/// @notice Protótipo da Fase 7. O contrato não guarda dado pessoal: cada chamado
///         entra só pelo hash do protocolo, e cada evento pelo hash do registro
///         de auditoria (HISTORICO_STATUS). O texto, a foto e o cidadão continuam
///         no Oracle; aqui fica a prova de que esse histórico não foi alterado.
/// @dev    Dois modos de uso, complementares:
///         1. Por chamado: registra abertura e cada mudança de status, aplica a
///            mesma máquina de estados do back-end e calcula o SLA na própria
///            cadeia, sem depender da prefeitura para dizer se o prazo venceu.
///         2. Por lote: o back-end calcula, por dia, a raiz de uma árvore de
///            Merkle (SHA-256) com todos os eventos de auditoria e ancora só a
///            raiz. Uma transação por dia, qualquer que seja o volume; e qualquer
///            pessoa verifica um evento com uma prova de O(log n) hashes.
contract HackGovRegistro {
    // Mesma ordem do enum StatusSolicitacao do back-end.
    enum Status { RECEBIDO, EM_ANALISE, AGENDADO, EM_EXECUCAO, CONCLUIDO, CANCELADO }

    enum SituacaoSla { INEXISTENTE, NO_PRAZO, VENCIDO, CUMPRIDO, CUMPRIDO_COM_ATRASO, CANCELADO }

    struct Chamado {
        uint64 abertura;      // timestamp do bloco que registrou a abertura
        uint64 prazo;         // abertura + SLA do tipo de serviço
        uint64 conclusao;     // 0 enquanto não concluído
        uint16 tipo;          // ID_TIPO do back-end
        Status status;
        uint32 eventos;       // quantos eventos já foram encadeados
        bytes32 ultimoHash;   // cabeça da cadeia de hashes do chamado
        bool existe;
    }

    struct Lote {
        bytes32 raiz;         // raiz de Merkle (SHA-256) dos eventos do período
        uint64 inicio;        // período coberto (epoch, segundos)
        uint64 fim;
        uint32 quantidade;    // número de folhas (eventos)
        uint64 registradoEm;  // timestamp do bloco
    }

    address public prefeitura;
    mapping(address => bool) public operadores;

    mapping(bytes32 => Chamado) private chamados;
    Lote[] private lotes;

    uint32 public totalChamados;
    uint32 public totalConcluidosComAtraso;

    event OperadorAlterado(address indexed conta, bool autorizado);
    event ChamadoRegistrado(bytes32 indexed protocoloHash, uint16 tipo, uint64 prazo, bytes32 cabeca);
    event EventoRegistrado(bytes32 indexed protocoloHash, Status anterior, Status novo, uint32 numero, bytes32 cabeca);
    event SlaDescumprido(bytes32 indexed protocoloHash, uint64 prazo, uint64 conclusao);
    event LoteAncorado(uint256 indexed indice, bytes32 raiz, uint64 inicio, uint64 fim, uint32 quantidade);

    error NaoAutorizado();
    error ChamadoJaExiste(bytes32 protocoloHash);
    error ChamadoInexistente(bytes32 protocoloHash);
    error TransicaoInvalida(Status de, Status para);
    error ParametroInvalido();
    error LoteForaDeOrdem(uint64 fimAnterior, uint64 inicioNovo);

    modifier somentePrefeitura() {
        if (msg.sender != prefeitura) revert NaoAutorizado();
        _;
    }

    modifier somenteOperador() {
        if (!operadores[msg.sender]) revert NaoAutorizado();
        _;
    }

    constructor() {
        prefeitura = msg.sender;
        operadores[msg.sender] = true;
        emit OperadorAlterado(msg.sender, true);
    }

    // ── Governança ────────────────────────────────────────────────

    /// @notice A prefeitura autoriza (ou revoga) as contas do back-end que podem escrever.
    function definirOperador(address conta, bool autorizado) external somentePrefeitura {
        if (conta == address(0)) revert ParametroInvalido();
        operadores[conta] = autorizado;
        emit OperadorAlterado(conta, autorizado);
    }

    // ── Modo 1: por chamado ──────────────────────────────────────

    /// @param protocoloHash SHA-256 do protocolo (ex.: ZEL-2026-1A2B3C4D), nunca o protocolo em claro.
    /// @param tipo          ID_TIPO do serviço.
    /// @param slaDias       SLA_DIAS do tipo no momento da abertura.
    /// @param hashAbertura  hash do registro de abertura no back-end.
    function registrarChamado(bytes32 protocoloHash, uint16 tipo, uint16 slaDias, bytes32 hashAbertura)
        external
        somenteOperador
    {
        if (protocoloHash == bytes32(0) || slaDias == 0) revert ParametroInvalido();
        Chamado storage c = chamados[protocoloHash];
        if (c.existe) revert ChamadoJaExiste(protocoloHash);

        uint64 agora = uint64(block.timestamp);
        c.existe = true;
        c.abertura = agora;
        c.prazo = agora + uint64(slaDias) * 1 days;
        c.tipo = tipo;
        c.status = Status.RECEBIDO;
        c.eventos = 1;
        c.ultimoHash = encadear(bytes32(0), protocoloHash, Status.RECEBIDO, hashAbertura, agora);
        totalChamados++;

        emit ChamadoRegistrado(protocoloHash, tipo, c.prazo, c.ultimoHash);
    }

    /// @notice Encadeia uma mudança de status. Recusa transições que o back-end também recusaria.
    function registrarEvento(bytes32 protocoloHash, Status novo, bytes32 hashEvento) external somenteOperador {
        Chamado storage c = chamados[protocoloHash];
        if (!c.existe) revert ChamadoInexistente(protocoloHash);
        Status anterior = c.status;
        if (!transicaoValida(anterior, novo)) revert TransicaoInvalida(anterior, novo);

        uint64 agora = uint64(block.timestamp);
        c.status = novo;
        c.eventos += 1;
        c.ultimoHash = encadear(c.ultimoHash, protocoloHash, novo, hashEvento, agora);

        if (novo == Status.CONCLUIDO) {
            c.conclusao = agora;
            if (agora > c.prazo) {
                totalConcluidosComAtraso++;
                emit SlaDescumprido(protocoloHash, c.prazo, agora);
            }
        }
        emit EventoRegistrado(protocoloHash, anterior, novo, c.eventos, c.ultimoHash);
    }

    /// @notice Mesma tabela de StatusSolicitacao.TRANSICOES: avança uma etapa ou cancela.
    function transicaoValida(Status de, Status para) public pure returns (bool) {
        if (de == Status.CONCLUIDO || de == Status.CANCELADO) return false;
        if (para == Status.CANCELADO) return true;
        return uint8(para) == uint8(de) + 1;
    }

    /// @notice Situação do SLA calculada pela própria cadeia: ninguém precisa confiar na prefeitura.
    function situacaoSla(bytes32 protocoloHash) public view returns (SituacaoSla) {
        Chamado storage c = chamados[protocoloHash];
        if (!c.existe) return SituacaoSla.INEXISTENTE;
        if (c.status == Status.CANCELADO) return SituacaoSla.CANCELADO;
        if (c.status == Status.CONCLUIDO) {
            return c.conclusao <= c.prazo ? SituacaoSla.CUMPRIDO : SituacaoSla.CUMPRIDO_COM_ATRASO;
        }
        return block.timestamp <= c.prazo ? SituacaoSla.NO_PRAZO : SituacaoSla.VENCIDO;
    }

    function consultarChamado(bytes32 protocoloHash) external view returns (Chamado memory) {
        Chamado memory c = chamados[protocoloHash];
        if (!c.existe) revert ChamadoInexistente(protocoloHash);
        return c;
    }

    /// @notice Recalcula um elo da cadeia (público, para quem quiser conferir off-chain).
    function encadear(bytes32 anterior, bytes32 protocoloHash, Status status, bytes32 hashEvento, uint64 quando)
        public
        pure
        returns (bytes32)
    {
        return keccak256(abi.encodePacked(anterior, protocoloHash, uint8(status), hashEvento, quando));
    }

    // ── Modo 2: ancoragem em lote ────────────────────────────────

    /// @notice Ancora a raiz de Merkle de um período de auditoria. Os períodos precisam ser contíguos ou
    ///         crescentes, para que ninguém reescreva um dia já ancorado com um lote novo.
    function ancorarLote(bytes32 raiz, uint64 inicio, uint64 fim, uint32 quantidade)
        external
        somenteOperador
        returns (uint256 indice)
    {
        if (raiz == bytes32(0) || fim < inicio || quantidade == 0) revert ParametroInvalido();
        if (lotes.length > 0) {
            uint64 fimAnterior = lotes[lotes.length - 1].fim;
            if (inicio < fimAnterior) revert LoteForaDeOrdem(fimAnterior, inicio);
        }
        lotes.push(Lote(raiz, inicio, fim, quantidade, uint64(block.timestamp)));
        indice = lotes.length - 1;
        emit LoteAncorado(indice, raiz, inicio, fim, quantidade);
    }

    function totalLotes() external view returns (uint256) {
        return lotes.length;
    }

    function consultarLote(uint256 indice) external view returns (Lote memory) {
        if (indice >= lotes.length) revert ParametroInvalido();
        return lotes[indice];
    }

    /// @notice Prova de inclusão: confere que `folha` (SHA-256 de um evento de auditoria) faz parte do
    ///         lote `indice`. Pares ordenados antes de cada hash, como no back-end (MerkleSha256.java).
    ///         Custo O(log n): 11 hashes bastam para um dia com 2.000 eventos.
    function verificarInclusao(uint256 indice, bytes32 folha, bytes32[] calldata prova) external view returns (bool) {
        if (indice >= lotes.length) revert ParametroInvalido();
        bytes32 h = folha;
        for (uint256 i = 0; i < prova.length; i++) {
            bytes32 p = prova[i];
            h = h <= p ? sha256(abi.encodePacked(h, p)) : sha256(abi.encodePacked(p, h));
        }
        return h == lotes[indice].raiz;
    }
}
