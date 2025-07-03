package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoAPI;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoAPI consumo = new ConsumoAPI();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=8e771f20";
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private SerieRepository repositorio;

    private List<Serie> series = new ArrayList<>();
    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;
        while (opcao !=0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar série por titulo
                    5 - Buscar serie por ator
                    6 - Top 5 Series
                    7 - Buscar pro Categoria
                    8 - Filtrar series
                    9 - Buscar de EP por trecho
                    10 - Top 5 EP por Serie
                    11 - Buscar EP a partir de uma data
                    0 - Sair
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4: buscarSeriePorTitulo();
                    break;
                case 5: buscarSeriePorAtor();
                    break;
                case 6: buscarTop5Serie();
                    break;
                case 7: buscarSeriePorCategoria();
                    break;
                case 8: filtrarSeriesPorTemporadaEAvaliacao();
                    break;
                case 9: buscarEpisodioPorTecho();
                    break;
                case 10: topEpisodiosPorSerie();
                    break;
                case 11: buscarEpisodiosDepoisDeUmaData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }



    private void teste() {
        System.out.println("Teste");
    }


    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        //dadosSeries.add(dados);
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Escolha uma serie pelo nome");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serie.isPresent()){
        var serieEncontrada = serie.get();
        List<DadosTemporada> temporadas = new ArrayList<>();

        for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
            var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
            DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
            temporadas.add(dadosTemporada);
        }
        temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numeroTemporada(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        } else {
            System.out.println("Serie nao encontrada!");
        }
    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream().sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma serie pelo nome");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);
        if(serieBusca.isPresent()) {
            System.out.println("Dados da serie: " + serieBusca.get());
        } else {
            System.out.println("Serie nao encontrada!");
        }
    }

    private void buscarSeriePorAtor() {
        System.out.println("Qual o nome para efetuar a buscar? ");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliacoes a partir de que valor? ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Serie em que " + nomeAtor + " trabalhou: ");
        seriesEncontradas.forEach(s-> System.out.println(s.getTitulo() + " avaliação: " + s.getAvaliacao()));
    }

    private void buscarTop5Serie() {
        List<Serie> serioTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        serioTop.forEach(s-> System.out.println(s.getTitulo() + " avaliação: " + s.getAvaliacao()));
    }

    private void buscarSeriePorCategoria() {
        System.out.println("Deseja buscar serie de que categoria/genero? ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriePorCategoria = repositorio.findByGenero(categoria);
        System.out.println("Serie da categoria " + nomeGenero);
        seriePorCategoria.forEach(System.out::println);
    }

    private void filtrarSeriesPorTemporadaEAvaliacao() {
        System.out.println("Filtrar series ate quantas temporadas? ");
        var totalTemporadas = leitura.nextInt();
        System.out.println("A partir de qual avaliacao? ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
        //List<Serie> filtroSerie = repositorio.findByTotalTemporadasLessThanEqualAndAvaliacaoGreaterThanEqual(Integer.parseInt(totalTemporadas, avaliacao);
        List<Serie> filtroSerie = repositorio.seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);
        System.out.println("*** SERIES FILTRADAS ***");
        filtroSerie.forEach(s-> System.out.println(s.getTitulo() + " -avaliacao: " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTecho(){
        System.out.println("Qual o nome do episodio para buscar? ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(e -> System.out.printf("Serie: %s Temporada %s - Episodio %s - %s\n",
                e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo()));
    }
    private void topEpisodiosPorSerie() {
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);
            topEpisodios.forEach(e -> System.out.printf("Serie: %s Temporada %s - Episodio %s - %s\n",
                    e.getSerie().getTitulo(),e.getTemporada(),e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao()));
        }
    }
    private void buscarEpisodiosDepoisDeUmaData() {
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Qual o ano limite de lancamento? ");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodiosAno = repositorio.episodioPorSerieEAno(serie, anoLancamento);
            episodiosAno.forEach(System.out::println);
        }
    }

}