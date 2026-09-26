import java.util.Scanner;
import model.Projeto;
import service.ProjetoService;

public class Main {

    public static void main(String[] args)
        throws Exception {

        Scanner scanner = new Scanner(System.in);

        ProjetoService service =
            new ProjetoService();

        service.carregar();

        int opcao = -1;

        while (opcao != 0) {

            System.out.println();
            System.out.println(
                "========================================"
            );
            System.out.println(
                "       SISTEMA DE PROJETOS"
            );
            System.out.println(
                "========================================"
            );

            System.out.println(
                    "Total de projetos: "
                    + service.listar().size()
                    );

            System.out.println();

            System.out.println(
                "1 - Listar projetos"
            );

            System.out.println(
                "2 - Buscar projeto"
            );

            System.out.println(
                "3 - Cadastrar projeto"
            );

            System.out.println(
                "4 - Alterar projeto"
            );

            System.out.println(
                "5 - Excluir projeto"
            );

            System.out.println(
                "6 - Buscar por categoria"
            );

            System.out.println(
                "7 - Buscar por status"
            );

            System.out.println(
                "0 - Sair"
            );

            System.out.print(
                "Escolha: "
            );

            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {

                case 1: {

                    System.out.println(
                        "\n--- LISTA DE PROJETOS ---"
                    );

                    for (Projeto projeto : service.listar()) {

                        projeto.exibirDados();

                        System.out.println(
                            "-------------------------"
                        );
                    }

                    break;
                }

                case 2: {

                    System.out.print(
                        "Digite o ID: "
                    );

                    int idBusca = scanner.nextInt();
                    scanner.nextLine();

                    Projeto encontrado =
                        service.buscarPorId(idBusca);

                    if (encontrado != null) {

                        encontrado.exibirDados();

                    } else {

                        System.out.println(
                            "Projeto não encontrado."
                        );
                    }

                    break;
                }

                case 3: {

                    System.out.println(
                        "\n--- CADASTRO ---"
                    );

                    System.out.print("ID: ");

                    int id = scanner.nextInt();
                    scanner.nextLine();

                    System.out.print("Nome: ");

                    String nome = scanner.nextLine();

                    System.out.print(
                        "Descrição: "
                    );

                    String descricao =
                        scanner.nextLine();

                    System.out.print(
                        "Categoria: "
                    );

                    String categoria =
                        scanner.nextLine();

                    System.out.print(
                        "Status: "
                    );

                    String status =
                        scanner.nextLine();

                    Projeto projeto =
                        new Projeto(
                            id,
                            nome,
                            descricao,
                            categoria,
                            status
                        );

                    boolean cadastrado =
                        service.adicionar(projeto);

                    if (cadastrado) {

                        service.salvar();

                        System.out.println(
                            "Projeto cadastrado."
                        );

                    } else {

                        System.out.println(
                            "Não foi possível cadastrar."
                        );
                    }

                    break;
                }

                        case 4: {

                            System.out.println(
                                "\n--- ALTERAÇÃO ---"
                            );

                            System.out.print(
                                "ID do projeto: "
                            );

                            int idAlterar = scanner.nextInt();
                            scanner.nextLine();

                            Projeto existente =
                                service.buscarPorId(idAlterar);

                            if (existente == null) {

                                System.out.println(
                                    "Projeto não encontrado."
                                );

                                break;
                            }

                            existente.exibirDados();

                            System.out.print(
                                "Deseja realmente alterar este projeto? (S/N): "
                            );

                            String confirmacao =
                                scanner.nextLine();

                            if (!confirmacao.equalsIgnoreCase("S")) {

                                System.out.println(
                                    "Alteração cancelada."
                                );

                                break;
                            }

                            System.out.print(
                                "Novo nome: "
                            );

                            String novoNome =
                                scanner.nextLine();

                            System.out.print(
                                "Nova descrição: "
                            );

                            String novaDescricao =
                                scanner.nextLine();

                            System.out.print(
                                "Nova categoria: "
                            );

                            String novaCategoria =
                                scanner.nextLine();

                            System.out.print(
                                "Novo status: "
                            );

                            String novoStatus =
                                scanner.nextLine();

                            Projeto atualizado =
                                new Projeto(
                                    idAlterar,
                                    novoNome,
                                    novaDescricao,
                                    novaCategoria,
                                    novoStatus
                                );

                            boolean alterado =
                                service.alterar(atualizado);

                            if (alterado) {

                                service.salvar();

                                System.out.println(
                                    "Projeto alterado."
                                );

                            } else {

                                System.out.println(
                                    "Erro ao alterar."
                                );
                            }

                            break;
                        }


                case 5: {

                    System.out.println(
                        "\n--- EXCLUSÃO ---"
                    );

                    System.out.print(
                        "ID do projeto: "
                    );

                    int idExcluir =
                        scanner.nextInt();

                    scanner.nextLine();

                    Projeto projetoExcluir =
                        service.buscarPorId(idExcluir);

                    if (projetoExcluir == null) {

                        System.out.println(
                            "Projeto não encontrado."
                        );

                        break;
                    }

                    projetoExcluir.exibirDados();

                    System.out.print(
                        "Confirma? (S/N): "
                    );

                    String confirmacao =
                        scanner.nextLine();

                    if (
                        confirmacao.equalsIgnoreCase("S")
                    ) {

                        boolean removido =
                            service.removerPorId(
                                idExcluir
                            );

                        if (removido) {

                            service.salvar();

                            System.out.println(
                                "Projeto excluído."
                            );

                        } else {

                            System.out.println(
                                "Erro ao excluir."
                            );
                        }

                    } else {

                        System.out.println(
                            "Exclusão cancelada."
                        );
                    }

                    break;
                }

               case 6: {

                    System.out.println(
                        "\n--- BUSCAR POR CATEGORIA ---"
                    );

                    System.out.print(
                        "Digite a categoria: "
                    );

                    String categoria = scanner.nextLine();

                    for (
                        Projeto projeto :
                        service.buscarPorCategoria(categoria)
                    ) {

                        projeto.exibirDados();

                        System.out.println(
                            "-------------------------"
                        );
                    }

                    if (categoria.isBlank()) {

                        System.out.println(
                            "A categoria é obrigatória."
                        );

                        break;
                    }

                    break;
                }

                case 7: {

                    System.out.println(
                        "\n--- BUSCAR POR STATUS ---"
                    );

                    System.out.print(
                        "Digite o status: "
                    );

                    String status =
                        scanner.nextLine();

                    for (
                        Projeto projeto :
                        service.buscarPorStatus(status)
                    ) {

                        projeto.exibirDados();

                        System.out.println(
                            "-------------------------"
                        );
                    }

                    break;
                }

                case 0: {

                    System.out.println(
                        "Encerrando..."
                    );

                    break;
                }

                default: {

                    System.out.println(
                        "Opção inválida."
                    );
                }
            }
        }

        scanner.close();
    }
}

