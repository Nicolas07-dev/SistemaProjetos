import model.Projeto;
import service.ProjetoService;

public class Main {

    public static void main(String[] args) {

        ProjetoService service =
            new ProjetoService();

        Projeto p1 = new Projeto(
            1,
            "Sistema Acadêmico",
            "Sistema para gerenciamento acadêmico",
            "Software",
            "Em desenvolvimento"
        );

        Projeto p2 = new Projeto(
            2,
            "Site Institucional",
            "Website institucional",
            "Web",
            "Concluído"
        );

        Projeto p3 = new Projeto(
            3,
            "Aplicativo Mobile",
            "Aplicativo para prestação de serviços",
            "Mobile",
            "Planejado"
        );

        service.adicionar(p1);
        service.adicionar(p2);
        service.adicionar(p3);

        System.out.println(
            "TOTAL DE PROJETOS: "
            + service.listar().size()
        );

        System.out.println();

        System.out.println("LISTA DE PROJETOS");

        for (Projeto projeto : service.listar()) {
            projeto.exibirDados();
            System.out.println("----------------");

        }

        System.out.println();

        System.out.println("BUSCA PELO ID 2");

        Projeto encontrado =
            service.buscarPorId(2);

        if (encontrado != null) {
            encontrado.exibirDados();

        }

        System.out.println();

        System.out.println("PROJETOS WEB");

        for (Projeto projeto :
            service.buscarPorCategoria("Web")
        ) {

            projeto.exibirDados();

        }

        System.out.println();

        System.out.println("PROJETOS CONCLUÍDOS");

        for (Projeto projeto :
            service.buscarPorStatus("Concluído")
        ) {
            projeto.exibirDados();

        }
    }
}