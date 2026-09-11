import model.Projeto;

public class Main {

    public static void main(String[] args) {

        Projeto projeto1 = new Projeto(
            1,
            "Sistema Acadêmico",
            "Sistema para gerenciamento acadêmico",
            "Software",
            "Em desenvolvimento"
        );

        Projeto projeto2 = new Projeto(
            2,
            "Site Institucional",
            "Website de uma instituição",
            "Web",
            "Concluído"
        );

        System.out.println("PROJETO 1");
        System.out.println(projeto1.getNome());
        System.out.println(projeto1.getCategoria());
        System.out.println(projeto1.getStatus());

        System.out.println();

        System.out.println("PROJETO 2");
        System.out.println(projeto2.getNome());
        System.out.println(projeto2.getCategoria());
        System.out.println(projeto2.getStatus());
    }
}

