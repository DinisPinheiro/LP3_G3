package com.hotel4you.hotel4you.Utils;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.ImpostoLinha;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.Montante;
import com.hotel4you.hotel4you.Model.encomenda.PrecoUnitario;
import com.hotel4you.hotel4you.Model.encomenda.Quantidade;
import com.hotel4you.hotel4you.Model.encomenda.TotaisEncomenda;
import com.hotel4you.hotel4you.Model.enums.FormatoFicheiro;
import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lê um ficheiro JSON de encomenda de um fornecedor e converte-o em objetos Java
 * ({@link EncomendaFornecedor}), tal como o {@link LeitorEncomendaXml} faz com o XML (S7-03, enunciado v1.1).
 * <p>
 * O JSON tem a mesma estrutura do XML (cabeçalho {@code Header}, linhas {@code Line}, quantidades, unidades,
 * preços e impostos), com estas diferenças de forma: a data é um texto {@code "aaaa-mm-dd"}, os atributos do
 * XML (por exemplo {@code UOM} e {@code CurrencyType}) são campos do próprio objeto e {@code Line},
 * {@code ProductIdentifier} e {@code InformationalQuantity} são listas.
 * <p>
 * <b>Validação.</b> Como não existe um XSD para JSON, a validação está aqui, no código. Primeiro o texto tem de
 * ser JSON bem formado (um erro de sintaxe indica a linha e a coluna). Depois confere-se cada campo
 * obrigatório e o seu tipo (texto, número inteiro, número decimal, lista, objeto), os intervalos que o XSD do
 * XML impõe (quantidades entre 0 e 65535, taxa e número de linha entre 0 e 255), a unidade de medida
 * ({@code Box}, {@code Unit} ou {@code Kilogram}) e a data. Todos os erros encontrados são devolvidos de uma
 * vez, cada um com o caminho do campo (por exemplo {@code Order.Line[2].Quantity.Value.UOM}), e nada é lido
 * se houver algum. Campos opcionais: {@code Address1}, {@code Address2}, {@code City}, {@code PostalCode},
 * {@code Country.Value}, {@code QuantityType}, {@code InformationalQuantity} e {@code OrderSummary}.
 * Campos desconhecidos são ignorados. Se {@code ProductIdentifier} tiver mais do que um código, usa-se o primeiro.
 * <p>
 * <b>Biblioteca.</b> Usa-se o Jackson ({@code jackson-databind}), em modo de árvore ({@link JsonNode}), porque:
 * lê os números com casas decimais como {@link BigDecimal} (o dinheiro nunca passa por {@code double}, como no
 * resto da aplicação); indica a linha e a coluna dos erros de sintaxe, como o {@link ErroXml}; recusa chaves
 * repetidas e texto a mais depois do JSON; é mantida ativamente, trata de módulos Java (JPMS) e traz poucas
 * dependências (só {@code jackson-core} e {@code jackson-annotations}). Ver {@code docs/Biblioteca_JSON.md}.
 * <p>
 * Esta classe não guarda nada na base de dados: só converte o JSON em objetos. Os erros usam a mesma
 * {@link XmlInvalidoException} e {@link ErroXml} do XML, para as janelas os mostrarem da mesma maneira.
 */
public final class LeitorEncomendaJson implements LeitorEncomenda {

    /** Número máximo de erros devolvidos de uma vez, para um ficheiro muito errado não encher o ecrã. */
    private static final int MAX_ERROS = 50;
    private static final int MAX_USHORT = 65535;
    private static final int MAX_UBYTE = 255;

    private static final ObjectMapper MAPEADOR = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    @Override
    public FormatoFicheiro formato() {
        return FormatoFicheiro.JSON;
    }

    @Override
    public EncomendaFornecedor ler(byte[] conteudo) throws IOException {
        JsonNode raiz = lerArvore(conteudo);
        return new Leitura().converter(raiz);
    }

    private static JsonNode lerArvore(byte[] conteudo) throws IOException {
        try {
            JsonNode raiz = MAPEADOR.readTree(conteudo);
            if (raiz == null || raiz.isMissingNode()) {
                throw new XmlInvalidoException(List.of(new ErroXml(0, 0, "O ficheiro JSON está vazio.")));
            }
            return raiz;
        } catch (JsonProcessingException e) {
            JsonLocation local = e.getLocation();
            int linha = local == null ? 0 : Math.max(local.getLineNr(), 0);
            int coluna = local == null ? 0 : Math.max(local.getColumnNr(), 0);
            throw new XmlInvalidoException(List.of(new ErroXml(linha, coluna,
                    "JSON mal formado: " + e.getOriginalMessage())));
        }
    }

    /**
     * Uma leitura de um ficheiro: vai juntando os erros de validação e só constrói a encomenda se não houver
     * nenhum. Cada método devolve {@code null} quando o campo falta ou é inválido (o erro fica registado).
     */
    private static final class Leitura {

        private final List<ErroXml> erros = new ArrayList<>();
        private boolean cortada;

        EncomendaFornecedor converter(JsonNode raiz) {
            EncomendaFornecedor encomenda = lerEncomenda(raiz);
            if (cortada) {
                erros.add(new ErroXml(0, 0, "Há mais erros; corrija estes e volte a escolher o ficheiro."));
            }
            if (!erros.isEmpty() || encomenda == null) {
                throw new XmlInvalidoException(erros);
            }
            return encomenda;
        }

        private EncomendaFornecedor lerEncomenda(JsonNode raiz) {
            JsonNode ordem = objeto(raiz, "", "Order");
            if (ordem == null) {
                return null;
            }
            String caminho = "Order";
            JsonNode cabecalho = objeto(ordem, caminho, "Header");
            String numero = null;
            LocalDate data = null;
            FornecedorEncomenda fornecedor = null;
            if (cabecalho != null) {
                String caminhoCabecalho = caminho(caminho, "Header");
                numero = texto(cabecalho, caminhoCabecalho, "OrderNumber");
                data = data(cabecalho, caminhoCabecalho, "OrderDate");
                fornecedor = lerFornecedor(cabecalho, caminhoCabecalho);
            }

            List<LinhaEncomenda> linhas = new ArrayList<>();
            JsonNode listaLinhas = lista(ordem, caminho, "Line", true);
            if (listaLinhas != null) {
                for (int i = 0; i < listaLinhas.size(); i++) {
                    JsonNode linha = elemento(listaLinhas, caminho(caminho, "Line"), i);
                    LinhaEncomenda lida = linha == null ? null : lerLinha(linha, caminho(caminho, "Line") + "[" + (i + 1) + "]");
                    if (lida != null) {
                        linhas.add(lida);
                    }
                }
            }
            TotaisEncomenda resumo = lerResumo(ordem, caminho);

            if (!erros.isEmpty() || numero == null || data == null || fornecedor == null) {
                return null;
            }
            return new EncomendaFornecedor(numero, data, fornecedor, linhas, resumo);
        }

        private FornecedorEncomenda lerFornecedor(JsonNode cabecalho, String caminhoCabecalho) {
            JsonNode parte = objeto(cabecalho, caminhoCabecalho, "SupplierParty");
            if (parte == null) {
                return null;
            }
            String caminhoParte = caminho(caminhoCabecalho, "SupplierParty");
            String identificador = texto(parte, caminhoParte, "PartyIdentifier");
            JsonNode morada = objeto(parte, caminhoParte, "NameAddress");
            if (morada == null) {
                return null;
            }
            String caminhoMorada = caminho(caminhoParte, "NameAddress");
            String nome = texto(morada, caminhoMorada, "Name");
            String morada1 = textoOpcional(morada, caminhoMorada, "Address1");
            String morada2 = textoOpcional(morada, caminhoMorada, "Address2");
            String cidade = textoOpcional(morada, caminhoMorada, "City");
            String codigoPostal = textoOpcional(morada, caminhoMorada, "PostalCode");
            JsonNode pais = objeto(morada, caminhoMorada, "Country");
            String codigoPais = null;
            String nomePais = null;
            if (pais != null) {
                String caminhoPais = caminho(caminhoMorada, "Country");
                codigoPais = texto(pais, caminhoPais, "ISOCountryCode");
                nomePais = textoOpcional(pais, caminhoPais, "Value");
            }
            if (identificador == null || nome == null || morada1 == null || morada2 == null || cidade == null
                    || codigoPostal == null || codigoPais == null || nomePais == null) {
                return null;
            }
            return new FornecedorEncomenda(identificador, nome, morada1, morada2, cidade, codigoPostal,
                    codigoPais, nomePais);
        }

        private LinhaEncomenda lerLinha(JsonNode linha, String caminho) {
            Integer numero = inteiro(linha, caminho, "LineNumber", 0, MAX_UBYTE);

            JsonNode produto = objeto(linha, caminho, "Product");
            String codigo = null;
            String agencia = null;
            String descricao = null;
            if (produto != null) {
                String caminhoProduto = caminho(caminho, "Product");
                descricao = texto(produto, caminhoProduto, "ProductDescription");
                JsonNode identificadores = lista(produto, caminhoProduto, "ProductIdentifier", true);
                JsonNode identificador = identificadores == null ? null
                        : elemento(identificadores, caminho(caminhoProduto, "ProductIdentifier"), 0);
                if (identificador != null) {
                    String caminhoIdentificador = caminho(caminhoProduto, "ProductIdentifier") + "[1]";
                    agencia = texto(identificador, caminhoIdentificador, "Agency");
                    codigo = texto(identificador, caminhoIdentificador, "Code");
                }
            }

            Quantidade quantidade = quantidade(objeto(linha, caminho, "Quantity"), caminho(caminho, "Quantity"));
            List<Quantidade> informativas = lerInformativas(linha, caminho);
            PrecoUnitario preco = lerPreco(linha, caminho);
            ImpostoLinha imposto = lerImposto(linha, caminho);
            Montante valorBase = montante(linha, caminho, "LineBaseAmount");

            if (numero == null || codigo == null || agencia == null || descricao == null || quantidade == null
                    || informativas == null || preco == null || imposto == null || valorBase == null) {
                return null;
            }
            return new LinhaEncomenda(numero, codigo, agencia, descricao, quantidade, informativas, preco,
                    imposto, valorBase);
        }

        /** As quantidades informativas são opcionais; {@code null} só se alguma estiver errada. */
        private List<Quantidade> lerInformativas(JsonNode linha, String caminho) {
            List<Quantidade> informativas = new ArrayList<>();
            JsonNode lista = lista(linha, caminho, "InformationalQuantity", false);
            if (lista == null) {
                // Sem lista: ou o campo não existe (válido, é opcional) ou existe e já tem o erro registado.
                return naoExiste(linha.get("InformationalQuantity")) ? informativas : null;
            }
            String caminhoLista = caminho(caminho, "InformationalQuantity");
            boolean valido = true;
            for (int i = 0; i < lista.size(); i++) {
                JsonNode elemento = elemento(lista, caminhoLista, i);
                Quantidade informativa = elemento == null ? null
                        : quantidade(elemento, caminhoLista + "[" + (i + 1) + "]");
                if (informativa == null) {
                    valido = false;
                } else {
                    informativas.add(informativa);
                }
            }
            return valido ? informativas : null;
        }

        private PrecoUnitario lerPreco(JsonNode linha, String caminho) {
            JsonNode preco = objeto(linha, caminho, "PricePerUnit");
            if (preco == null) {
                return null;
            }
            String caminhoPreco = caminho(caminho, "PricePerUnit");
            Montante montante = valorMonetario(preco, caminhoPreco);
            JsonNode valor = objeto(preco, caminhoPreco, "Value");
            Integer porQuantidade = null;
            UnidadeMedida unidade = null;
            if (valor != null) {
                String caminhoValor = caminho(caminhoPreco, "Value");
                porQuantidade = inteiro(valor, caminhoValor, "Value", 0, MAX_USHORT);
                unidade = unidade(valor, caminhoValor, "UOM");
            }
            if (montante == null || porQuantidade == null || unidade == null) {
                return null;
            }
            return new PrecoUnitario(montante, porQuantidade, unidade);
        }

        private ImpostoLinha lerImposto(JsonNode linha, String caminho) {
            JsonNode ajuste = objeto(linha, caminho, "MonetaryAdjustment");
            if (ajuste == null) {
                return null;
            }
            String caminhoAjuste = caminho(caminho, "MonetaryAdjustment");
            String tipoAjuste = texto(ajuste, caminhoAjuste, "AdjustmentType");
            Integer numeroAjuste = inteiro(ajuste, caminhoAjuste, "MonetaryAdjustmentLine", 0, MAX_UBYTE);
            Montante baseCalculo = montante(ajuste, caminhoAjuste, "MonetaryAdjustmentStartAmount");

            JsonNode imposto = objeto(ajuste, caminhoAjuste, "TaxAdjustment");
            String tipoImposto = null;
            Integer percentagem = null;
            Montante valorImposto = null;
            String local = null;
            if (imposto != null) {
                String caminhoImposto = caminho(caminhoAjuste, "TaxAdjustment");
                tipoImposto = texto(imposto, caminhoImposto, "TaxType");
                percentagem = inteiro(imposto, caminhoImposto, "TaxPercent", 0, MAX_UBYTE);
                valorImposto = montante(imposto, caminhoImposto, "TaxAmount");
                local = texto(imposto, caminhoImposto, "TaxLocation");
            }
            if (tipoAjuste == null || numeroAjuste == null || baseCalculo == null || tipoImposto == null
                    || percentagem == null || valorImposto == null || local == null) {
                return null;
            }
            return new ImpostoLinha(tipoAjuste, numeroAjuste, baseCalculo, tipoImposto, percentagem,
                    valorImposto, local);
        }

        /** Lê os totais do campo opcional {@code OrderSummary}; {@code null} se não existe (ou está errado). */
        private TotaisEncomenda lerResumo(JsonNode ordem, String caminho) {
            if (naoExiste(ordem.get("OrderSummary"))) {
                return null;
            }
            JsonNode resumo = objeto(ordem, caminho, "OrderSummary");
            if (resumo == null) {
                return null;
            }
            String caminhoResumo = caminho(caminho, "OrderSummary");
            Montante linhas = montante(resumo, caminhoResumo, "TotalLinesAmount");
            Montante taxas = montante(resumo, caminhoResumo, "TotalTaxAmount");
            Montante total = montante(resumo, caminhoResumo, "OrderTotalAmount");
            if (linhas == null || taxas == null || total == null) {
                return null;
            }
            return new TotaisEncomenda(linhas.valor(), taxas.valor(), total.valor(), total.moeda());
        }

        private Quantidade quantidade(JsonNode quantidade, String caminho) {
            if (quantidade == null) {
                return null;
            }
            String tipo = textoOpcional(quantidade, caminho, "QuantityType");
            JsonNode valor = objeto(quantidade, caminho, "Value");
            Integer numero = null;
            UnidadeMedida unidade = null;
            if (valor != null) {
                String caminhoValor = caminho(caminho, "Value");
                numero = inteiro(valor, caminhoValor, "Value", 0, MAX_USHORT);
                unidade = unidade(valor, caminhoValor, "UOM");
            }
            if (tipo == null || numero == null || unidade == null) {
                return null;
            }
            return new Quantidade(tipo, numero, unidade);
        }

        /** Lê o objeto {@code nome} que contém um {@code CurrencyValue}. */
        private Montante montante(JsonNode pai, String caminhoPai, String nome) {
            JsonNode contentor = objeto(pai, caminhoPai, nome);
            return contentor == null ? null : valorMonetario(contentor, caminho(caminhoPai, nome));
        }

        /** Lê o {@code CurrencyValue} (moeda e valor) que está dentro do objeto indicado. */
        private Montante valorMonetario(JsonNode contentor, String caminhoContentor) {
            JsonNode valor = objeto(contentor, caminhoContentor, "CurrencyValue");
            if (valor == null) {
                return null;
            }
            String caminhoValor = caminho(caminhoContentor, "CurrencyValue");
            String moeda = texto(valor, caminhoValor, "CurrencyType");
            BigDecimal quantia = decimal(valor, caminhoValor, "Value");
            return moeda == null || quantia == null ? null : new Montante(quantia, moeda);
        }

        private UnidadeMedida unidade(JsonNode pai, String caminhoPai, String nome) {
            String texto = texto(pai, caminhoPai, nome);
            if (texto == null) {
                return null;
            }
            try {
                return UnidadeMedida.deXml(texto);
            } catch (IllegalArgumentException e) {
                String validas = Arrays.stream(UnidadeMedida.values()).map(UnidadeMedida::getValorXml)
                        .collect(Collectors.joining(", "));
                erro(caminho(caminhoPai, nome), "unidade de medida desconhecida '" + texto + "' (use " + validas + ").");
                return null;
            }
        }

        private LocalDate data(JsonNode pai, String caminhoPai, String nome) {
            String texto = texto(pai, caminhoPai, nome);
            if (texto == null) {
                return null;
            }
            try {
                return LocalDate.parse(texto);
            } catch (DateTimeParseException e) {
                erro(caminho(caminhoPai, nome), "data inválida '" + texto + "' (use o formato aaaa-mm-dd, por exemplo 2026-11-06).");
                return null;
            }
        }

        // ---- campos simples ---------------------------------------------------------------------------

        /** Objeto que tem de existir; {@code null} (com erro) se faltar ou não for um objeto. */
        private JsonNode objeto(JsonNode pai, String caminhoPai, String nome) {
            String caminho = caminho(caminhoPai, nome);
            JsonNode no = pai.get(nome);
            if (naoExiste(no)) {
                erro(caminho, "falta o campo obrigatório.");
                return null;
            }
            if (!no.isObject()) {
                erro(caminho, "tem de ser um objeto { ... }.");
                return null;
            }
            return no;
        }

        /**
         * Lista que tem de existir se {@code obrigatoria}; {@code null} se não existir (com erro, só se
         * obrigatória) ou não for uma lista. Uma lista obrigatória não pode estar vazia.
         */
        private JsonNode lista(JsonNode pai, String caminhoPai, String nome, boolean obrigatoria) {
            String caminho = caminho(caminhoPai, nome);
            JsonNode no = pai.get(nome);
            if (naoExiste(no)) {
                if (obrigatoria) {
                    erro(caminho, "falta o campo obrigatório.");
                }
                return null;
            }
            if (!no.isArray()) {
                erro(caminho, "tem de ser uma lista [ ... ].");
                return null;
            }
            if (obrigatoria && no.isEmpty()) {
                erro(caminho, "tem de ter pelo menos um elemento.");
                return null;
            }
            return no;
        }

        /** O elemento {@code posicao} (a partir de 0) de uma lista, que tem de ser um objeto. */
        private JsonNode elemento(JsonNode lista, String caminhoLista, int posicao) {
            JsonNode no = lista.get(posicao);
            if (no == null || !no.isObject()) {
                erro(caminhoLista + "[" + (posicao + 1) + "]", "tem de ser um objeto { ... }.");
                return null;
            }
            return no;
        }

        /** Texto obrigatório e não vazio. */
        private String texto(JsonNode pai, String caminhoPai, String nome) {
            String caminho = caminho(caminhoPai, nome);
            JsonNode no = pai.get(nome);
            if (naoExiste(no)) {
                erro(caminho, "falta o campo obrigatório.");
                return null;
            }
            if (!no.isTextual()) {
                erro(caminho, "tem de ser um texto.");
                return null;
            }
            String texto = no.asText().trim();
            if (texto.isEmpty()) {
                erro(caminho, "não pode estar vazio.");
                return null;
            }
            return texto;
        }

        /** Texto opcional: devolve {@code ""} se não existir; {@code null} (com erro) se existir e não for texto. */
        private String textoOpcional(JsonNode pai, String caminhoPai, String nome) {
            JsonNode no = pai.get(nome);
            if (naoExiste(no)) {
                return "";
            }
            if (!no.isTextual()) {
                erro(caminho(caminhoPai, nome), "tem de ser um texto.");
                return null;
            }
            return no.asText().trim();
        }

        /** Número inteiro obrigatório dentro de um intervalo (o mesmo que o XSD do XML impõe). */
        private Integer inteiro(JsonNode pai, String caminhoPai, String nome, int minimo, int maximo) {
            String caminho = caminho(caminhoPai, nome);
            JsonNode no = pai.get(nome);
            if (naoExiste(no)) {
                erro(caminho, "falta o campo obrigatório.");
                return null;
            }
            if (!no.isIntegralNumber() || !no.canConvertToInt()) {
                erro(caminho, "tem de ser um número inteiro (encontrado: " + resumir(no) + ").");
                return null;
            }
            int valor = no.intValue();
            if (valor < minimo || valor > maximo) {
                erro(caminho, "tem de estar entre " + minimo + " e " + maximo + " (encontrado: " + valor + ").");
                return null;
            }
            return valor;
        }

        /** Número decimal obrigatório (lido como {@link BigDecimal}, sem perder cêntimos). */
        private BigDecimal decimal(JsonNode pai, String caminhoPai, String nome) {
            String caminho = caminho(caminhoPai, nome);
            JsonNode no = pai.get(nome);
            if (naoExiste(no)) {
                erro(caminho, "falta o campo obrigatório.");
                return null;
            }
            if (!no.isNumber()) {
                erro(caminho, "tem de ser um número (encontrado: " + resumir(no) + ").");
                return null;
            }
            return no.decimalValue();
        }

        // ---- apoio ------------------------------------------------------------------------------------

        private void erro(String caminho, String motivo) {
            if (erros.size() >= MAX_ERROS) {
                cortada = true;
                return;
            }
            erros.add(new ErroXml(0, 0, caminho + ": " + motivo));
        }

        private static boolean naoExiste(JsonNode no) {
            return no == null || no.isNull() || no.isMissingNode();
        }

        private static String caminho(String pai, String nome) {
            return pai.isEmpty() ? nome : pai + "." + nome;
        }

        /** O valor encontrado, curto, para a mensagem de erro. */
        private static String resumir(JsonNode no) {
            String texto = no.isTextual() ? "\"" + no.asText() + "\"" : no.toString();
            return texto.length() > 30 ? texto.substring(0, 30) + "..." : texto;
        }
    }
}
