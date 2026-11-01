package com.hotel4you.hotel4you.Utils;

import java.util.List;

/**
 * O ficheiro XML não é válido e por isso não foi lido. Guarda os erros (com linha e motivo)
 * para a View os poder mostrar.
 */
public class XmlInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<ErroXml> erros;

    /**
     * @param erros erros encontrados (pelo menos um)
     */
    public XmlInvalidoException(List<ErroXml> erros) {
        super(new ResultadoValidacao(erros).mensagem());
        this.erros = List.copyOf(erros);
    }

    /** @return os erros que tornam o ficheiro inválido */
    public List<ErroXml> getErros() {
        return erros;
    }
}
