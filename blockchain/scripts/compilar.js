// Compila contracts/HackGovRegistro.sol com o solc-js (sem baixar compilador nativo)
// e grava ABI + bytecode em artifacts/HackGovRegistro.json.
const fs = require('fs')
const path = require('path')
const solc = require('solc')

const raiz = path.join(__dirname, '..')
const arquivo = 'HackGovRegistro.sol'
const fonte = fs.readFileSync(path.join(raiz, 'contracts', arquivo), 'utf8')

const entrada = {
  language: 'Solidity',
  sources: { [arquivo]: { content: fonte } },
  settings: {
    optimizer: { enabled: true, runs: 200 },
    evmVersion: 'paris', // compatível com o ganache 7 (sem PUSH0)
    outputSelection: { '*': { '*': ['abi', 'evm.bytecode.object', 'evm.gasEstimates'] } },
  },
}

const saida = JSON.parse(solc.compile(JSON.stringify(entrada)))
const erros = (saida.errors || []).filter(e => e.severity === 'error')
for (const e of saida.errors || []) console.error(e.formattedMessage)
if (erros.length) process.exit(1)

const c = saida.contracts[arquivo].HackGovRegistro
fs.mkdirSync(path.join(raiz, 'artifacts'), { recursive: true })
fs.writeFileSync(path.join(raiz, 'artifacts', 'HackGovRegistro.json'),
  JSON.stringify({ compilador: solc.version(), abi: c.abi, bytecode: '0x' + c.evm.bytecode.object }, null, 2))
console.log(`compilado com solc ${solc.version()} — bytecode ${c.evm.bytecode.object.length / 2} bytes`)
