# HackGov · blockchain (Fase 7)

`contracts/HackGovRegistro.sol` — registro público e auditável dos chamados, sem dado pessoal na cadeia.

- **Por chamado:** `registrarChamado` / `registrarEvento` aplicam a mesma máquina de estados do back-end
  (`RECEBIDO → EM_ANALISE → AGENDADO → EM_EXECUCAO → CONCLUIDO`, cancelamento em qualquer etapa) e
  `situacaoSla` calcula o prazo pelo relógio da cadeia.
- **Em lote:** `ancorarLote` grava a raiz de Merkle (SHA-256) de um dia de auditoria; `verificarInclusao`
  confere a prova de um evento em O(log n).

```bash
npm install
npm test        # 7 testes (solc-js 0.8.28 + ganache em memória)
npm run demo    # ou: npm run demo -- --api http://localhost:8081
```

Opções do demo: `--dias N` (quantos dias pendentes ancorar), `--rpc URL` (nó externo, ex.: `npx ganache`
ou Hyperledger Besu), `--email` / `--senha` (gestor).
