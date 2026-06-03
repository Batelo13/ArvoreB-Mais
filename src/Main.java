import java.util.List;

public class Main {

    public static void main(String[] args) {
        BPlusTree tree = new BPlusTree(4); // Árvore B+ de ordem 4

        // 1) Inserções de 1 a 30
        for (int i = 1; i <= 30; i++) {
            tree.insert(i);
        }

        System.out.println("=== Após inserir os valores de 1 a 30 (ordem 4) ===");
        List<Integer> antes = tree.rangeQuery(1, 30);
        System.out.println("rangeQuery(1, 30) = " + antes);
        System.out.println("Total de chaves: " + antes.size());

        System.out.println();
        System.out.println("Lista ligada de folhas (ponteiros next):");
        tree.printLeafChain();

        // 3) Remoções
        int[] remover = {5, 10, 15, 20, 25};
        System.out.println();
        System.out.print("=== Removendo os valores: ");
        for (int i = 0; i < remover.length; i++) {
            System.out.print(remover[i] + (i < remover.length - 1 ? ", " : ""));
            tree.delete(remover[i]);
        }
        System.out.println(" ===");

        // 4) Nova consulta por intervalo
        System.out.println();
        List<Integer> depois = tree.rangeQuery(1, 30);
        System.out.println("rangeQuery(1, 30) = " + depois);
        System.out.println("Total de chaves: " + depois.size());

        // 5) Comprovação da lista ligada
        System.out.println();
        System.out.println("Lista ligada de folhas após as remoções (ponteiros next):");
        tree.printLeafChain();

        // Verificações automáticas de correção
        System.out.println();
        System.out.println("=== Verificações de correção ===");

        boolean removidosAusentes = true;
        for (int r : remover) {
            if (tree.contains(r)) {
                removidosAusentes = false;
                break;
            }
        }
        System.out.println("Todos os valores removidos estão ausentes? " + removidosAusentes);

        // rangeQuery deve bater exatamente com a varredura da lista ligada
        boolean listaIntegra = depois.equals(tree.leafChainKeys());
        System.out.println("rangeQuery == varredura da lista ligada? " + listaIntegra);

        // a lista ligada deve estar estritamente crescente (encadeamento correto)
        List<Integer> cadeia = tree.leafChainKeys();
        boolean crescente = true;
        for (int i = 1; i < cadeia.size(); i++) {
            if (cadeia.get(i - 1) >= cadeia.get(i)) {
                crescente = false;
                break;
            }
        }
        System.out.println("Lista ligada estritamente crescente? " + crescente);
        System.out.println("Chaves pela lista ligada = " + cadeia);

        System.out.println();
        System.out.println(removidosAusentes && listaIntegra && crescente
                ? "RESULTADO: correto e lista ligada íntegra."
                : "RESULTADO: ERRO detectado!");
    }
}
