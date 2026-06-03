# ArvoreB-Mais

Implementação didática de uma **Árvore B⁺** (B Plus Tree) em Java, com foco em
**inserção**, **remoção** (com empréstimo e fusão de nós), **consulta por
intervalo** (`rangeQuery`) e na **lista ligada de folhas**.

Trabalho de faculdade sobre a Árvore B⁺.

## Autores

- **Arthur Batelo Bastos**
- **Gabriel Boni**

---

## Sumário

- [O que é uma Árvore B⁺](#o-que-é-uma-árvore-b)
- [Conceitos-chave da implementação](#conceitos-chave-da-implementação)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Como executar](#como-executar)
- [API da classe `BPlusTree`](#api-da-classe-bplustree)
- [Detalhes dos algoritmos](#detalhes-dos-algoritmos)
- [Demonstração e saída esperada](#demonstração-e-saída-esperada)

---

## O que é uma Árvore B⁺

A Árvore B⁺ é uma estrutura de dados balanceada, muito usada em bancos de dados e
sistemas de arquivos, por ser eficiente em disco e ideal para **buscas por faixa**.
Suas principais características:

- **Todos os dados reais ficam nas folhas.** Os nós internos servem apenas para
  guiar a busca (roteamento).
- **As folhas são encadeadas em uma lista ligada**, o que permite percorrer todas
  as chaves em ordem crescente sem precisar voltar à raiz — perfeito para
  `rangeQuery`.
- A árvore é **sempre balanceada**: todas as folhas estão no mesmo nível.

### Ordem da árvore

Neste projeto, **ordem** (`m`) é o número **máximo de filhos** de um nó interno.
Para a ordem `m`:

| Parâmetro | Fórmula | Ordem 4 |
|-----------|---------|---------|
| Máximo de chaves por nó | `m - 1` | 3 |
| Mínimo de chaves por nó (exceto raiz) | `ceil(m/2) - 1` | 1 |
| Máximo de filhos (nó interno) | `m` | 4 |

Quando um nó ultrapassa o máximo de chaves, ocorre um **split** (divisão). Quando
uma remoção deixa um nó abaixo do mínimo, ocorre **empréstimo** (borrow) de um
irmão ou **fusão** (merge) com um irmão.

---

## Conceitos-chave da implementação

### 1. Chave de roteamento × chave real

Esta é a distinção central da Árvore B⁺ e recebe atenção especial no código:

- **Chave real:** existe somente na folha e possui um **valor** associado. É o dado
  de fato armazenado.
- **Chave de roteamento:** é uma **cópia** de uma chave que sobe para os nós
  internos durante um split. Ela serve apenas para decidir por qual filho descer
  na busca.

Consequência importante na **remoção**: ao remover uma chave, apagamos **apenas o
registro real na folha**. Uma cópia de roteamento idêntica pode **permanecer** em
um nó interno sem qualquer problema — ela continua roteando corretamente, mesmo
que o valor já não exista em nenhuma folha. Por isso a remoção **não** sai
"caçando" a chave nos nós internos; os nós internos só mudam por causa de
empréstimos/fusões estruturais.

### 2. Encadeamento entre folhas

Cada folha possui um ponteiro `next` para a folha seguinte, formando uma lista
ligada ordenada. Esse encadeamento é mantido íntegro em **todas** as operações:

- No **split de folha**: a nova folha à direita herda o `next` antigo e a folha
  original passa a apontar para ela.
- Na **fusão de folhas**: a folha da esquerda recebe as chaves da direita e assume
  o `next` da folha removida, sem quebrar a cadeia.

A `rangeQuery` aproveita exatamente esse encadeamento: localiza a folha do limite
inferior e segue pelos ponteiros `next` coletando as chaves até passar do limite
superior.

---

## Estrutura do projeto

```
ArvoreB-Mais/
├── .idea/                 # configuração do IntelliJ (módulo, SDK, etc.)
├── src/                   # código-fonte (Sources Root)
│   ├── BPlusTree.java     # implementação da Árvore B⁺
│   └── Main.java          # demonstração / ponto de entrada
├── README.md              # esta documentação
└── Tree
```

---

## Como executar

### Pelo IntelliJ IDEA

1. Abra o projeto. A pasta `src` deve aparecer **azul** (marcada como *Sources Root*).
2. Confirme o SDK em `File → Project Structure → Project → SDK = 17`.
3. Abra `src/Main.java` e clique na **setinha verde ▶** ao lado de
   `public static void main` → **Run 'Main.main()'**.

> Se a setinha verde não aparecer, faça `File → Reload All from Disk` ou
> `File → Invalidate Caches… → Invalidate and Restart`.

### Pelo terminal

A partir da raiz do projeto:

```powershell
javac -encoding UTF-8 -d out src\BPlusTree.java src\Main.java
java -cp out Main
```

> No Windows, se os acentos saírem quebrados no console, rode `chcp 65001` antes.

---

## API da classe `BPlusTree`

| Método | Descrição |
|--------|-----------|
| `BPlusTree(int order)` | Cria uma árvore com a ordem (máx. de filhos) informada. |
| `void insert(int key, int value)` | Insere/atualiza a chave com o valor dado. |
| `void insert(int key)` | Atalho: insere a chave usando ela mesma como valor. |
| `void delete(int key)` | Remove a chave (somente da folha; rebalanceia se necessário). |
| `boolean contains(int key)` | Indica se a chave existe na árvore. |
| `List<Integer> rangeQuery(int lo, int hi)` | Retorna, em ordem crescente, todas as chaves em `[lo, hi]`. |
| `void printLeafChain()` | Imprime cada folha e seu ponteiro `next` (comprova o encadeamento). |
| `List<Integer> leafChainKeys()` | Retorna todas as chaves percorrendo a lista ligada de folhas. |

---

## Detalhes dos algoritmos

### Inserção

1. Desce recursivamente até a folha correta.
2. Insere a chave em ordem na folha. Se a chave já existir, atualiza o valor.
3. Se a folha ultrapassar o máximo de chaves, faz **split**: a primeira chave da
   folha direita é **copiada** para o pai (cópia de roteamento) e o encadeamento é
   mantido.
4. Splits podem propagar para cima. Se a raiz dividir, uma nova raiz é criada,
   aumentando a altura da árvore.

No split de **nó interno**, a chave do meio **sobe** (é movida, não copiada),
pois nós internos não guardam dados — apenas roteamento.

### Remoção

1. Desce recursivamente e remove a chave **apenas da folha** (com seu valor).
2. Ao retornar, o pai verifica se o filho ficou abaixo do mínimo de chaves
   (*underflow*). Em caso afirmativo, `rebalance` decide:
   - **Empréstimo do irmão esquerdo** (`borrowFromLeft`), se ele tiver chaves
     sobrando;
   - senão, **empréstimo do irmão direito** (`borrowFromRight`);
   - senão, **fusão** (`merge`) com um irmão, descendo a chave separadora do pai.
3. Empréstimos e fusões atualizam corretamente as chaves de roteamento do pai.
4. Se a raiz interna ficar sem chaves, seu único filho passa a ser a nova raiz
   (a árvore diminui de altura).

Tudo isso é feito tratando **folha** e **nó interno** separadamente, e o ponteiro
`next` das folhas é sempre preservado.

### Consulta por intervalo (`rangeQuery`)

1. Localiza a folha onde estaria o limite inferior `lo`.
2. Percorre as folhas pelo ponteiro `next`, coletando as chaves `>= lo`.
3. Para assim que encontra uma chave `> hi` (as folhas são ordenadas).

---

## Demonstração e saída esperada

A `main` executa o roteiro exigido com uma **Árvore B⁺ de ordem 4**:

1. Insere os valores de **1 a 30**.
2. Executa `rangeQuery(1, 30)` e imprime o resultado.
3. Imprime a lista ligada de folhas com os ponteiros `next`.
4. Remove os valores **5, 10, 15, 20 e 25**.
5. Executa `rangeQuery(1, 30)` novamente.
6. Imprime novamente a lista ligada de folhas, comprovando a integridade.
7. Faz verificações automáticas de correção.

Saída esperada (resumo):

```
=== Após inserir os valores de 1 a 30 (ordem 4) ===
rangeQuery(1, 30) = [1, 2, 3, ..., 30]
Total de chaves: 30

Lista ligada de folhas (ponteiros next):
Folha[0] [1, 2]  --next-->  Folha[1] [3, 4]
...
Folha[14] [29, 30]  --next-->  null

=== Removendo os valores: 5, 10, 15, 20, 25 ===

rangeQuery(1, 30) = [1, 2, 3, 4, 6, 7, 8, 9, 11, ..., 30]
Total de chaves: 25

Lista ligada de folhas após as remoções (ponteiros next):
Folha[0] [1, 2]  --next-->  Folha[1] [3, 4]
Folha[1] [3, 4]  --next-->  Folha[2] [6]
...
Folha[14] [29, 30]  --next-->  null

=== Verificações de correção ===
Todos os valores removidos estão ausentes? true
rangeQuery == varredura da lista ligada? true
Lista ligada estritamente crescente? true

RESULTADO: correto e lista ligada íntegra.
```

As verificações comprovam que:

- nenhuma das chaves removidas (5, 10, 15, 20, 25) permanece nas folhas;
- o resultado da `rangeQuery` é idêntico à varredura direta da lista ligada;
- a lista ligada continua **ordenada e íntegra** após as remoções.
