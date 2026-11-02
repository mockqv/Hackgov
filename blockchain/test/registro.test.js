const { test, before } = require('node:test')
const assert = require('node:assert/strict')
const { subirRede, implantar, avancarDias, SLA, hashProtocolo, hashTexto, ethers } = require('../lib/rede')
const merkle = require('../lib/merkle')

const S = { RECEBIDO: 0, EM_ANALISE: 1, AGENDADO: 2, EM_EXECUCAO: 3, CONCLUIDO: 4, CANCELADO: 5 }
let g, contas, c, prefeitura, backend, cidadao

before(async () => {
  ;({ g, contas } = await subirRede())
  ;[prefeitura, backend, cidadao] = contas
  ;({ contrato: c } = await implantar(prefeitura))
  await (await c.definirOperador(await backend.getAddress(), true)).wait()
})

// o ganache devolve o motivo do revert em info.error.data.result; decodificamos pelo ABI
function nomeDoErro(e) {
  const dados = e.data || e.info?.error?.data?.result || e.error?.data?.result
  try { return c.interface.parseError(dados).name } catch { return e.revert?.name || e.shortMessage || e.message }
}
const rejeita = (promessa, erro) => assert.rejects(promessa, e => nomeDoErro(e) === erro)

test('só operador autorizado escreve; só a prefeitura autoriza', async () => {
  const p = hashProtocolo('ZEL-2026-AAAA0001')
  await rejeita(c.connect(cidadao).registrarChamado(p, 1, 7, hashTexto('x')), 'NaoAutorizado')
  await rejeita(c.connect(backend).definirOperador(await cidadao.getAddress(), true), 'NaoAutorizado')
})

test('ciclo completo dentro do prazo: SLA cumprido e cadeia de hashes conferível', async () => {
  const cb = c.connect(backend)
  const p = hashProtocolo('ZEL-2026-AAAA0002')
  await (await cb.registrarChamado(p, 2, 5, hashTexto('abertura'))).wait()
  assert.equal(SLA[await c.situacaoSla(p)], 'NO_PRAZO')
  for (const s of [S.EM_ANALISE, S.AGENDADO, S.EM_EXECUCAO, S.CONCLUIDO]) {
    await avancarDias(g, 1)
    await (await cb.registrarEvento(p, s, hashTexto('evento ' + s))).wait()
  }
  const ch = await c.consultarChamado(p)
  assert.equal(ch.eventos, 5n)
  assert.equal(SLA[await c.situacaoSla(p)], 'CUMPRIDO')
  assert.notEqual(ch.ultimoHash, ethers.ZeroHash)
})

test('máquina de estados igual à do back-end: não pula etapa nem reabre', async () => {
  const cb = c.connect(backend)
  const p = hashProtocolo('ZEL-2026-AAAA0003')
  await (await cb.registrarChamado(p, 1, 7, hashTexto('abertura'))).wait()
  await rejeita(cb.registrarEvento(p, S.CONCLUIDO, hashTexto('pulo')), 'TransicaoInvalida')
  await (await cb.registrarEvento(p, S.CANCELADO, hashTexto('cancelado'))).wait()
  await rejeita(cb.registrarEvento(p, S.EM_ANALISE, hashTexto('reabrir')), 'TransicaoInvalida')
  assert.equal(SLA[await c.situacaoSla(p)], 'CANCELADO')
  assert.equal(await c.transicaoValida(S.RECEBIDO, S.EM_ANALISE), true)
  assert.equal(await c.transicaoValida(S.AGENDADO, S.CANCELADO), true)
  assert.equal(await c.transicaoValida(S.RECEBIDO, S.AGENDADO), false)
})

test('SLA vence sozinho na cadeia e a conclusão atrasada emite SlaDescumprido', async () => {
  const cb = c.connect(backend)
  const p = hashProtocolo('ZEL-2026-AAAA0004')
  await (await cb.registrarChamado(p, 1, 7, hashTexto('abertura'))).wait()
  await avancarDias(g, 8)
  assert.equal(SLA[await c.situacaoSla(p)], 'VENCIDO') // ninguém precisou "marcar" o atraso
  for (const s of [S.EM_ANALISE, S.AGENDADO, S.EM_EXECUCAO]) await (await cb.registrarEvento(p, s, hashTexto('e' + s))).wait()
  const recibo = await (await cb.registrarEvento(p, S.CONCLUIDO, hashTexto('fim'))).wait()
  const ev = recibo.logs.map(l => c.interface.parseLog(l)).find(l => l?.name === 'SlaDescumprido')
  assert.ok(ev, 'evento SlaDescumprido emitido')
  assert.equal(SLA[await c.situacaoSla(p)], 'CUMPRIDO_COM_ATRASO')
  assert.equal(await c.totalConcluidosComAtraso(), 1n)
})

test('não registra o mesmo protocolo duas vezes', async () => {
  const p = hashProtocolo('ZEL-2026-AAAA0002')
  await rejeita(c.connect(backend).registrarChamado(p, 2, 5, hashTexto('de novo')), 'ChamadoJaExiste')
})

test('ancoragem em lote: prova de Merkle confere na cadeia e detecta adulteração', async () => {
  const eventos = Array.from({ length: 13 }, (_, i) => `${i + 1}|ZEL-2026-B${i}|RECEBIDO|EM_ANALISE|2026-10-15T10:${String(i).padStart(2, '0')}:00|3|triagem`)
  const folhas = eventos.map(merkle.folha)
  const raiz = merkle.raiz(folhas)
  const cb = c.connect(backend)
  await (await cb.ancorarLote(merkle.hex(raiz), 1792022400, 1792108799, folhas.length)).wait()
  const indice = (await c.totalLotes()) - 1n
  for (const i of [0, 6, 12]) {
    const prova = merkle.prova(folhas, i).map(merkle.hex)
    assert.equal(await c.verificarInclusao(indice, merkle.hex(folhas[i]), prova), true)
    assert.ok(merkle.verificar(folhas[i], merkle.prova(folhas, i), raiz))
  }
  // alguém muda a justificativa do evento 7 no banco: a folha muda e a prova falha
  const adulterado = merkle.folha(eventos[6].replace('triagem', 'triagem (editado)'))
  assert.equal(await c.verificarInclusao(indice, merkle.hex(adulterado), merkle.prova(folhas, 6).map(merkle.hex)), false)
})

test('lotes não podem reescrever um período já ancorado', async () => {
  const cb = c.connect(backend)
  const r = merkle.hex(merkle.folha('qualquer'))
  await rejeita(cb.ancorarLote(r, 1792022400, 1792050000, 1), 'LoteForaDeOrdem')
  await rejeita(cb.ancorarLote(ethers.ZeroHash, 1792108800, 1792195199, 1), 'ParametroInvalido')
  await (await cb.ancorarLote(r, 1792108800, 1792195199, 1)).wait()
  assert.equal(await c.totalLotes(), 2n)
})
