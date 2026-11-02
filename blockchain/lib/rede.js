// Sobe uma blockchain Ethereum em memória (ganache) e implanta o contrato.
// Nada sai da máquina: serve para os testes e para a demonstração.
// o ganache tenta carregar um binário nativo opcional e avisa no console quando não acha;
// o fallback em JavaScript funciona igual, então o aviso é silenciado
const _console = { error: console.error, warn: console.warn, log: console.log }
console.error = console.warn = console.log = () => {}
const _escrever = process.stderr.write.bind(process.stderr)
process.stderr.write = () => true
const ganache = require('ganache')
process.stderr.write = _escrever
Object.assign(console, _console)
const { ethers } = require('ethers')
const artefato = require('../artifacts/HackGovRegistro.json')

async function subirRede() {
  const g = ganache.provider({ logging: { quiet: true }, wallet: { totalAccounts: 4 }, chain: { hardfork: 'merge', vmErrorsOnRPCResponse: true } })
  const provider = new ethers.BrowserProvider(g, undefined, { polling: true, pollingInterval: 50 })
  provider.pollingInterval = 50
  const contas = await Promise.all([0, 1, 2, 3].map(i => provider.getSigner(i)))
  return { g, provider, contas }
}

async function implantar(signer) {
  const fabrica = new ethers.ContractFactory(artefato.abi, artefato.bytecode, signer)
  const contrato = await fabrica.deploy()
  const recibo = await contrato.deploymentTransaction().wait()
  return { contrato, recibo }
}

// avança o relógio da cadeia (dias) e minera um bloco
async function avancarDias(g, dias) {
  await g.request({ method: 'evm_increaseTime', params: [Math.round(dias * 86400)] })
  await g.request({ method: 'evm_mine', params: [] })
}

const STATUS = ['RECEBIDO', 'EM_ANALISE', 'AGENDADO', 'EM_EXECUCAO', 'CONCLUIDO', 'CANCELADO']
const SLA = ['INEXISTENTE', 'NO_PRAZO', 'VENCIDO', 'CUMPRIDO', 'CUMPRIDO_COM_ATRASO', 'CANCELADO']
const hashProtocolo = p => ethers.sha256(ethers.toUtf8Bytes(p))
const hashTexto = t => ethers.sha256(ethers.toUtf8Bytes(t))

module.exports = { subirRede, implantar, avancarDias, STATUS, SLA, hashProtocolo, hashTexto, ethers }
