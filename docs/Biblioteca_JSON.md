# Biblioteca JSON — S7-03 Importação de ficheiros JSON

O enunciado v1.1 pede que o Hotel4You aceite ficheiros JSON dos fornecedores que não conseguem exportar XML. A
leitura do JSON precisa de uma biblioteca externa (o JDK não traz nenhuma), por isso fica aqui a justificação,
como o enunciado exige para qualquer biblioteca externa.

## Escolha: Jackson (`com.fasterxml.jackson.core:jackson-databind`, versão 2.18.2)

Usa-se só o **modo de árvore** (`ObjectMapper.readTree` e `JsonNode`), sem mapear o JSON para classes por
reflexão. É o `LeitorEncomendaJson` que percorre a árvore, valida cada campo e constrói a mesma
`EncomendaFornecedor` que o `LeitorEncomendaXml` constrói a partir do XML.

Razões:

1. **Dinheiro sem `double`.** Com `USE_BIG_DECIMAL_FOR_FLOATS`, `0.25` e `42.40` são lidos como `BigDecimal`. O resto
   da aplicação já usa `BigDecimal` para todos os valores monetários, e um `double` daria diferenças de cêntimos.
2. **Erros de sintaxe com linha e coluna.** `JsonProcessingException.getLocation()` dá a linha e a coluna, que
   encaixam no `ErroXml` que as janelas já mostram para o XML.
3. **Validação mais rigorosa por omissão.** Recusa chaves repetidas (`STRICT_DUPLICATE_DETECTION`) e texto a mais
   depois do JSON (`FAIL_ON_TRAILING_TOKENS`).
4. **Funciona com módulos Java (JPMS).** Traz `module-info` próprio (`com.fasterxml.jackson.databind`); o projeto só
   precisa de `requires com.fasterxml.jackson.databind;`.
5. **Mantida e muito usada.** Licença Apache 2.0, poucas dependências (`jackson-core` e `jackson-annotations`).

## Alternativas consideradas

| Biblioteca | Porque não |
| --- | --- |
| `org.json.simple` (json-simple) | A versão clássica (1.1.1) não tem manutenção há muitos anos e lê os decimais como `Double`, o que não serve para dinheiro. |
| `javax.json` (JSON-P) | É só a API: precisa de uma implementação à parte (duas dependências) e o pacote `javax.json` foi substituído por `jakarta.json`. |
| Google Gson | Serviria (também lê `BigDecimal`), mas o erro de sintaxe só traz a linha e a coluna dentro do texto da mensagem e, no modo de árvore, aceita chaves repetidas (fica a última). |

## Onde se usa

- `Utils/LeitorEncomendaJson` — única classe que importa o Jackson.
- `module-info.java` — `requires com.fasterxml.jackson.databind;`
- `pom.xml` — dependência `jackson-databind` (propriedade `jackson.version`).

O resto da aplicação não sabe que existe Jackson: fala só com a interface `LeitorEncomenda`.
