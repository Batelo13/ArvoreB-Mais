import java.util.ArrayList;
import java.util.List;

public class BPlusTree {

    /* ------------------------------------------------------------------ */
    /* Estrutura dos nós                                                  */
    /* ------------------------------------------------------------------ */

    private abstract static class Node {
        final List<Integer> keys = new ArrayList<>();
        abstract boolean isLeaf();
    }

    private static class InternalNode extends Node {
        final List<Node> children = new ArrayList<>();
        @Override boolean isLeaf() { return false; }
    }

    private static class LeafNode extends Node {
        final List<Integer> values = new ArrayList<>();
        LeafNode next;                 // encadeamento da lista ligada de folhas
        @Override boolean isLeaf() { return true; }
    }

    private static class Split {
        final int key;
        final Node right;
        Split(int key, Node right) { this.key = key; this.right = right; }
    }

    /* ------------------------------------------------------------------ */
    /* Campos                                                             */
    /* ------------------------------------------------------------------ */

    private final int order;     // m = número máximo de filhos
    private final int maxKeys;   // m - 1
    private final int minKeys;   // ceil(m/2) - 1
    private Node root;
    private LeafNode firstLeaf;  // cabeça da lista ligada de folhas

    public BPlusTree(int order) {
        if (order < 3) {
            throw new IllegalArgumentException("A ordem da Árvore B+ deve ser >= 3");
        }
        this.order = order;
        this.maxKeys = order - 1;
        this.minKeys = (int) Math.ceil(order / 2.0) - 1;
        LeafNode leaf = new LeafNode();
        this.root = leaf;
        this.firstLeaf = leaf;
    }

    /* ------------------------------------------------------------------ */
    /* Busca                                                              */
    /* ------------------------------------------------------------------ */

    private LeafNode findLeaf(int key) {
        Node node = root;
        while (!node.isLeaf()) {
            InternalNode in = (InternalNode) node;
            int i = childIndex(in, key);
            node = in.children.get(i);
        }
        return (LeafNode) node;
    }

    private int childIndex(InternalNode in, int key) {
        int i = 0;
        while (i < in.keys.size() && key >= in.keys.get(i)) {
            i++;
        }
        return i;
    }

    public boolean contains(int key) {
        LeafNode leaf = findLeaf(key);
        return leaf.keys.contains(key);
    }

    /* ------------------------------------------------------------------ */
    /* Inserção                                                           */
    /* ------------------------------------------------------------------ */

    public void insert(int key, int value) {
        Split split = insertRec(root, key, value);
        if (split != null) {
            // a raiz foi dividida -> cria uma nova raiz
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(split.key);
            newRoot.children.add(root);
            newRoot.children.add(split.right);
            root = newRoot;
        }
    }

    public void insert(int key) {
        insert(key, key);
    }

    private Split insertRec(Node node, int key, int value) {
        if (node.isLeaf()) {
            return insertIntoLeaf((LeafNode) node, key, value);
        }
        InternalNode in = (InternalNode) node;
        int i = childIndex(in, key);
        Split childSplit = insertRec(in.children.get(i), key, value);
        if (childSplit == null) {
            return null;
        }
        // insere a chave promovida e o novo filho na posição correta
        in.keys.add(i, childSplit.key);
        in.children.add(i + 1, childSplit.right);
        if (in.keys.size() > maxKeys) {
            return splitInternal(in);
        }
        return null;
    }

    private Split insertIntoLeaf(LeafNode leaf, int key, int value) {
        int pos = 0;
        while (pos < leaf.keys.size() && leaf.keys.get(pos) < key) {
            pos++;
        }
        if (pos < leaf.keys.size() && leaf.keys.get(pos) == key) {
            leaf.values.set(pos, value); // chave já existe -> atualiza
            return null;
        }
        leaf.keys.add(pos, key);
        leaf.values.add(pos, value);
        if (leaf.keys.size() > maxKeys) {
            return splitLeaf(leaf);
        }
        return null;
    }

    private Split splitLeaf(LeafNode leaf) {
        int total = leaf.keys.size();
        int mid = total / 2;
        LeafNode right = new LeafNode();
        // move a metade direita para a nova folha
        right.keys.addAll(leaf.keys.subList(mid, total));
        right.values.addAll(leaf.values.subList(mid, total));
        leaf.keys.subList(mid, total).clear();
        leaf.values.subList(mid, total).clear();
        // mantém o encadeamento da lista ligada
        right.next = leaf.next;
        leaf.next = right;
        // CÓPIA de roteamento: a 1ª chave da folha direita sobe (mas continua na folha)
        return new Split(right.keys.get(0), right);
    }

    private Split splitInternal(InternalNode node) {
        int total = node.keys.size();
        int mid = total / 2;                 // chave que sobe (movida, não copiada)
        int upKey = node.keys.get(mid);
        InternalNode right = new InternalNode();
        // chaves à direita de mid vão para o novo nó
        right.keys.addAll(node.keys.subList(mid + 1, total));
        // filhos correspondentes
        right.children.addAll(node.children.subList(mid + 1, node.children.size()));
        // limpa do nó original
        node.keys.subList(mid, total).clear();                  // remove mid e à direita
        node.children.subList(mid + 1, node.children.size()).clear();
        return new Split(upKey, right);
    }

    /* ------------------------------------------------------------------ */
    /* Remoção                                                            */
    /* ------------------------------------------------------------------ */

    public void delete(int key) {
        deleteRec(root, key);
        // se a raiz interna ficou sem chaves, o único filho vira a nova raiz
        if (!root.isLeaf() && root.keys.isEmpty()) {
            root = ((InternalNode) root).children.get(0);
        }
    }

    private void deleteRec(Node node, int key) {
        if (node.isLeaf()) {
            // remove SOMENTE o registro real da folha.
            // Cópias de roteamento iguais em nós internos podem permanecer.
            LeafNode leaf = (LeafNode) node;
            int idx = leaf.keys.indexOf(key);
            if (idx >= 0) {
                leaf.keys.remove(idx);
                leaf.values.remove(idx);
            }
            return;
        }
        InternalNode in = (InternalNode) node;
        int i = childIndex(in, key);
        Node child = in.children.get(i);
        deleteRec(child, key);
        if (child.keys.size() < minKeys) {
            rebalance(in, i);
        }
    }

    private void rebalance(InternalNode parent, int i) {
        Node child = parent.children.get(i);
        Node left  = (i > 0) ? parent.children.get(i - 1) : null;
        Node right = (i < parent.children.size() - 1) ? parent.children.get(i + 1) : null;

        if (left != null && left.keys.size() > minKeys) {
            borrowFromLeft(parent, i);
        } else if (right != null && right.keys.size() > minKeys) {
            borrowFromRight(parent, i);
        } else if (left != null) {
            merge(parent, i - 1);            // funde o irmão esquerdo com o filho
        } else {
            merge(parent, i);                // funde o filho com o irmão direito
        }
    }

    private void borrowFromLeft(InternalNode parent, int i) {
        Node child = parent.children.get(i);
        Node left  = parent.children.get(i - 1);
        if (child.isLeaf()) {
            LeafNode c = (LeafNode) child, l = (LeafNode) left;
            int last = l.keys.size() - 1;
            c.keys.add(0, l.keys.remove(last));
            c.values.add(0, l.values.remove(last));
            parent.keys.set(i - 1, c.keys.get(0)); // atualiza a cópia de roteamento
        } else {
            InternalNode c = (InternalNode) child, l = (InternalNode) left;
            // desce a chave separadora do pai e sobe a última chave do irmão esquerdo
            c.keys.add(0, parent.keys.get(i - 1));
            c.children.add(0, l.children.remove(l.children.size() - 1));
            parent.keys.set(i - 1, l.keys.remove(l.keys.size() - 1));
        }
    }

    private void borrowFromRight(InternalNode parent, int i) {
        Node child = parent.children.get(i);
        Node right = parent.children.get(i + 1);
        if (child.isLeaf()) {
            LeafNode c = (LeafNode) child, r = (LeafNode) right;
            c.keys.add(r.keys.remove(0));
            c.values.add(r.values.remove(0));
            parent.keys.set(i, r.keys.get(0)); // nova 1ª chave da folha direita
        } else {
            InternalNode c = (InternalNode) child, r = (InternalNode) right;
            c.keys.add(parent.keys.get(i));
            c.children.add(r.children.remove(0));
            parent.keys.set(i, r.keys.remove(0));
        }
    }

    private void merge(InternalNode parent, int sep) {
        Node left  = parent.children.get(sep);
        Node right = parent.children.get(sep + 1);
        if (left.isLeaf()) {
            LeafNode l = (LeafNode) left, r = (LeafNode) right;
            l.keys.addAll(r.keys);
            l.values.addAll(r.values);
            l.next = r.next;                 // preserva o encadeamento
        } else {
            InternalNode l = (InternalNode) left, r = (InternalNode) right;
            l.keys.add(parent.keys.get(sep));   // desce a separadora
            l.keys.addAll(r.keys);
            l.children.addAll(r.children);
        }
        parent.keys.remove(sep);
        parent.children.remove(sep + 1);
    }

    /* ------------------------------------------------------------------ */
    /* Consulta por intervalo                                             */
    /* ------------------------------------------------------------------ */

    public List<Integer> rangeQuery(int lo, int hi) {
        List<Integer> result = new ArrayList<>();
        LeafNode leaf = findLeaf(lo);
        while (leaf != null) {
            for (int k : leaf.keys) {
                if (k > hi) {
                    return result;       // como as folhas são ordenadas, podemos parar
                }
                if (k >= lo) {
                    result.add(k);
                }
            }
            leaf = leaf.next;            // segue pelo ponteiro next
        }
        return result;
    }

    /* ------------------------------------------------------------------ */
    /* Depuração / comprovação da lista ligada de folhas                  */
    /* ------------------------------------------------------------------ */

    public void printLeafChain() {
        LeafNode leaf = firstLeaf;
        int idx = 0;
        while (leaf != null) {
            String thisLeaf = "Folha[" + idx + "] " + leaf.keys;
            String nextStr = (leaf.next == null)
                    ? "null"
                    : "Folha[" + (idx + 1) + "] " + leaf.next.keys;
            System.out.println(thisLeaf + "  --next-->  " + nextStr);
            leaf = leaf.next;
            idx++;
        }
    }

    public List<Integer> leafChainKeys() {
        List<Integer> all = new ArrayList<>();
        LeafNode leaf = firstLeaf;
        while (leaf != null) {
            all.addAll(leaf.keys);
            leaf = leaf.next;
        }
        return all;
    }
}
