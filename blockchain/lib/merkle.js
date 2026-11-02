// Árvore de Merkle com SHA-256 e pares ordenados — mesma regra de
// backend/.../integridade/MerkleSha256.java e de HackGovRegistro.verificarInclusao.
//  - folha  = SHA-256 do texto canônico do evento (UTF-8)
//  - nó     = SHA-256(menor || maior)  (pares ordenados: a prova não precisa dizer o lado)
//  - nível ímpar: o último nó sobe sem par
const crypto = require('crypto')

const sha256 = buf => crypto.createHash('sha256').update(buf).digest()
const hex = buf => '0x' + buf.toString('hex')
const deHex = h => Buffer.from(h.replace(/^0x/, ''), 'hex')

function par(a, b) {
  return Buffer.compare(a, b) <= 0 ? sha256(Buffer.concat([a, b])) : sha256(Buffer.concat([b, a]))
}

function folha(textoCanonico) {
  return sha256(Buffer.from(textoCanonico, 'utf8'))
}

function niveis(folhas) {
  if (!folhas.length) throw new Error('lote vazio')
  const n = [folhas]
  while (n[n.length - 1].length > 1) {
    const atual = n[n.length - 1]
    const prox = []
    for (let i = 0; i < atual.length; i += 2) prox.push(i + 1 < atual.length ? par(atual[i], atual[i + 1]) : atual[i])
    n.push(prox)
  }
  return n
}

const raiz = folhas => niveis(folhas).at(-1)[0]

function prova(folhas, indice) {
  const n = niveis(folhas)
  const p = []
  let i = indice
  for (let k = 0; k < n.length - 1; k++) {
    const irmao = i % 2 === 0 ? i + 1 : i - 1
    if (irmao < n[k].length) p.push(n[k][irmao])
    i = Math.floor(i / 2)
  }
  return p
}

function verificar(folhaBuf, provaBufs, raizBuf) {
  let h = folhaBuf
  for (const p of provaBufs) h = par(h, p)
  return h.equals(raizBuf)
}

module.exports = { sha256, hex, deHex, folha, raiz, prova, verificar }
