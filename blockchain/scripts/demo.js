// Demonstração do HackGovRegistro (Fase 7).
//
//   npm run demo                                  -> só a blockchain (em memória)
//   npm run demo -- --api http://localhost:8081   -> também ancora os dias de auditoria do HackGov rodando
//
// Opções:
//   --api <url>        back-end do HackGov (login como gestor e ancoragem dos dias pendentes)
//   --dias <n>         quantos dias pendentes ancorar (padrão: todos; os mais antigos primeiro)
//   --rpc <url>        usar um nó Ethereum externo (ex.: npx ganache, Hardhat, Besu) em vez da cadeia em memória
//   --email / --senha  credenciais do gestor (padrão: gestor@hackgov.local / Hackgov@123)
const { ethers } = require('ethers')
const { subirRede, implantar, avancarDias, STATUS, SLA, hashProtocolo, hashTexto } = require('../lib/rede')
const artefato = require('../artifacts/HackGovRegistro.json')
const merkle = require('../lib/merkle')

const arg = (nome, padrao) => {
  const i = process.argv.indexOf('--' + nome)
  return i > 0 && process.argv[i + 1] ? process.argv[i + 1] : padrao
}
const API = arg('api', null)
const DIAS = Number(arg('dias', 100000))
const RPC = arg('rpc', null)

const titulo = t => console.log('\n\x1b[36m━━ ' + t + ' ' + '━'.repeat(Math.max(0, 66 - t.length)) + '\x1b[0m')
const ok = t => console.log('  \x1b[32m✔\x1b[0m ' + t)
const falha = t => console.log('  \x1b[31m✘\x1b[0m ' + t)
const info = t => console.log('    ' + t)
const curto = h => h.slice(0, 10) + '…' + h.slice(-6)

async function conectar() {
  if (RPC) {
    const provider = new ethers.JsonRpcProvider(RPC)
    const contas = await Promise.all([0, 1].map(i => provider.getSigner(i)))
    return { g: null, contas, rede: `rpc ${RPC}` }
  }
  const { g, contas } = await subirRede()
  return { g, contas, rede: 'ganache em memória' }
}

async function api(caminho, { metodo = 'GET', corpo, token } = {}) {
  const r = await fetch(API.replace(/\/$/, '') + caminho, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: corpo ? JSON.stringify(corpo) : undefined,
  })
  const json = await r.json().catch(() => ({}))
  if (!r.ok || json.success === false) throw new Error(`${metodo} ${caminho} -> ${r.status} ${json.message || ''}`)
  return json.data
}

async function main() {
  titulo('1. Rede e implantação do contrato')
  const { g, contas, rede } = await conectar()
  const [prefeitura, backend] = contas
  const { contrato, recibo } = await implantar(prefeitura)
  ok(`HackGovRegistro implantado em ${await contrato.getAddress()} (${rede})`)
  info(`compilador ${artefato.compilador.split('+')[0]} · gás da implantação: ${recibo.gasUsed}`)
  await (await contrato.definirOperador(await backend.getAddress(), true)).wait()
  ok(`prefeitura autorizou a conta do back-end ${curto(await backend.getAddress())} como operadora`)
  const c = contrato.connect(backend)

  titulo('2. Um chamado na cadeia: máquina de estados e SLA')
  const protocolo = 'ZEL-2026-DEMO0001'
  const p = hashProtocolo(protocolo)
  info(`protocolo ${protocolo} entra só como hash: ${curto(p)} (nenhum dado pessoal na cadeia)`)
  let tx = await (await c.registrarChamado(p, 2, 5, hashTexto('abertura: poste apagado'))).wait()
  ok(`registrarChamado · tipo ILUMINACAO, SLA 5 dias · gás ${tx.gasUsed}`)
  const gasChamado = tx.gasUsed
  let gasEventos = 0n
  if (g) {
    for (const [dias, s] of [[1, 1], [1, 2], [2, 3]]) {
      await avancarDias(g, dias)
      tx = await (await c.registrarEvento(p, s, hashTexto('evento ' + STATUS[s]))).wait()
      gasEventos += tx.gasUsed
      ok(`+${dias} dia(s) → ${STATUS[s].padEnd(11)} · SLA: ${SLA[await c.situacaoSla(p)]} · gás ${tx.gasUsed}`)
    }
    try {
      await c.registrarEvento.staticCall(p, 1, hashTexto('voltar'))
      falha('a cadeia aceitou voltar de EM_EXECUCAO para EM_ANALISE')
    } catch {
      ok('tentativa de voltar para EM_ANALISE recusada pelo contrato (TransicaoInvalida)')
    }
    await avancarDias(g, 3)
    info(`+3 dias sem conclusão → SLA na cadeia: \x1b[33m${SLA[await c.situacaoSla(p)]}\x1b[0m (ninguém precisou marcar o atraso)`)
    tx = await (await c.registrarEvento(p, 4, hashTexto('concluído com foto do depois'))).wait()
    gasEventos += tx.gasUsed
    const atraso = tx.logs.map(l => c.interface.parseLog(l)).find(l => l?.name === 'SlaDescumprido')
    ok(`CONCLUIDO · evento SlaDescumprido ${atraso ? 'emitido' : 'não emitido'} · situação final: ${SLA[await c.situacaoSla(p)]}`)
    const ch = await c.consultarChamado(p)
    info(`cadeia de hashes do chamado: ${ch.eventos} elos, cabeça ${curto(ch.ultimoHash)}`)
  } else {
    info('(com --rpc externo, o avanço de tempo é pulado; use a cadeia em memória para ver o SLA vencer)')
  }

  titulo('3. Ancoragem em lote da trilha de auditoria')
  let lotes = []
  let token = null
  if (API) {
    const login = await api('/api/auth/login', {
      metodo: 'POST',
      corpo: { email: arg('email', 'gestor@hackgov.local'), senha: arg('senha', 'Hackgov@123') },
    })
    token = login.token
    ok(`login no HackGov como ${login.user?.email || 'gestor'}`)
    const resumo = await api('/api/integridade/lotes', { token })
    info(`${resumo.dias} dias de auditoria, ${resumo.eventos} eventos · ${resumo.pendentes} pendentes de ancoragem`)
    lotes = resumo.lotes.filter(l => l.situacao === 'PENDENTE').sort((a, b) => a.data.localeCompare(b.data))
    // só dias encerrados; o dia de hoje ainda recebe eventos
    const hoje = new Date().toISOString().slice(0, 10)
    lotes = lotes.filter(l => l.data < hoje).slice(0, DIAS)
  } else {
    info('sem --api: usando um dia de exemplo com 40 eventos fictícios')
    const eventos = Array.from({ length: 40 }, (_, i) =>
      `v1|${i + 1}|ZEL-2026-EX${String(i).padStart(4, '0')}|RECEBIDO|EM_ANALISE|2026-09-30T${String(8 + (i % 10)).padStart(2, '0')}:00:00|3|`)
    const folhas = eventos.map(merkle.folha)
    lotes = [{ data: '2026-09-30', eventos: folhas.length, raizAtual: merkle.hex(merkle.raiz(folhas)),
               inicioEpoch: 1790737200, fimEpoch: 1790823599, _folhas: folhas }]
  }

  let gasLotes = 0n
  const indicePorDia = {}
  const verbose = lotes.length <= 12
  if (!verbose) info(`ancorando ${lotes.length} dias (mostrando o primeiro e os 3 últimos)…`)
  for (const [k, l] of lotes.entries()) {
    const recibo = await (await c.ancorarLote(l.raizAtual, l.inicioEpoch, l.fimEpoch, l.eventos)).wait()
    const indice = Number((await c.totalLotes()) - 1n)
    gasLotes += recibo.gasUsed
    indicePorDia[l.data] = indice
    const mostrar = verbose || k === 0 || k >= lotes.length - 3
    if (mostrar) ok(`${l.data} · ${String(l.eventos).padStart(3)} eventos · raiz ${curto(l.raizAtual)} · lote #${indice} · gás ${recibo.gasUsed}`)
    if (API) {
      await api(`/api/integridade/lotes/${l.data}/ancora`, {
        metodo: 'POST', token,
        corpo: { raiz: l.raizAtual, rede, txHash: recibo.hash, indiceLote: indice },
      })
      if (mostrar) info(`registrado no HackGov · tx ${curto(recibo.hash)}`)
    }
  }
  if (!lotes.length) info('nenhum dia pendente para ancorar')

  titulo('4. Prova de inclusão conferida pelo próprio contrato')
  if (!API && lotes.length) {
    const folhas = lotes[0]._folhas
    const i = 17
    const prova = merkle.prova(folhas, i)
    const valido = await c.verificarInclusao(0, merkle.hex(folhas[i]), prova.map(merkle.hex))
    ok(`evento 18 de 40 está no lote #0: ${valido} (prova com ${prova.length} hashes = ⌈log₂ 40⌉)`)
    const adulterado = merkle.folha('v1|18|ZEL-2026-EX0017|RECEBIDO|CANCELADO|2026-09-30T15:00:00|3|')
    const aceito = await c.verificarInclusao(0, merkle.hex(adulterado), prova.map(merkle.hex))
    ;(aceito ? falha : ok)(`mesmo evento com o status trocado no banco: ${aceito} → adulteração detectada`)
  } else if (API && lotes.length) {
    const ultimo = lotes[lotes.length - 1]
    if (ultimo.exemploProtocolo) {
      // a prova vem do back-end (Java) e quem confere é o contrato: as duas implementações têm de concordar
      const prova = await api(`/api/integridade/prova/${encodeURIComponent(ultimo.exemploProtocolo)}`, { token })
      const ev = prova.eventos.find(e => e.dataHora.slice(0, 10) === ultimo.data)
      if (ev) {
        const valido = await c.verificarInclusao(indicePorDia[ultimo.data], ev.folha, ev.prova)
        ;(valido ? ok : falha)(`${ultimo.exemploProtocolo}, evento ${ev.statusAnterior} → ${ev.statusNovo} de ${ultimo.data}: ` +
          `prova do back-end (${ev.prova.length} hashes) aceita pelo contrato = ${valido}`)
      }
    }
    info(`abra a tela Integridade do HackGov: ${lotes.length === 1 ? 'o dia agora aparece' : lotes.length + ' dias agora aparecem'} como ÍNTEGRO, até ${ultimo.data}.`)
    info('altere um evento direto no Oracle (ex.: UPDATE historico_status SET justificativa = ...) e clique em')
    info('"Verificar agora": o dia muda para DIVERGENTE, porque a raiz recalculada não bate com a ancorada.')
  }

  titulo('5. Custo: por chamado × em lote')
  const porChamado = gasChamado + gasEventos
  info(`ciclo completo de 1 chamado na cadeia: ${porChamado} de gás (abertura + 4 eventos)`)
  info(`1.869 chamados da massa demo nesse modo: ~${(porChamado * 1869n / 1000000n)} milhões de gás`)
  if (lotes.length) info(`ancoragem em lote: ~${gasLotes / BigInt(lotes.length)} de gás por dia, qualquer que seja o volume`)
  info('numa rede permissionada (ex.: Hyperledger Besu entre órgãos públicos) o gás não tem custo em moeda;')
  info('o lote diário é o que escala: uma transação por dia, prova individual de O(log n).')
  console.log('')
  process.exit(0)
}

main().catch(e => {
  console.error('\n\x1b[31mErro:\x1b[0m', e.message)
  process.exit(1)
})
