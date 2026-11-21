package com.hotel4you.hotel4you.Utils;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Corre operações demoradas (acessos à base de dados) fora da thread da interface JavaFX (S6-01),
 * para que os ecrãs não bloqueiem enquanto se espera pelo servidor.
 * <p>
 * O trabalho corre numa thread própria. O resultado (ou o erro) volta depois à thread do JavaFX,
 * que é a única onde se pode mexer no ecrã. As operações correm uma de cada vez, pela ordem em que
 * foram pedidas, para que uma consulta antiga nunca apareça por cima de uma mais recente.
 * O tempo de resposta de cada operação fica registado na consola.
 */
public final class SegundoPlano {

    private static final Logger LOG = Logger.getLogger(SegundoPlano.class.getName());

    /** Uma só thread "de fundo"; é daemon para não impedir a aplicação de fechar. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(trabalho -> {
        Thread thread = new Thread(trabalho, "hotel4you-base-de-dados");
        thread.setDaemon(true);
        return thread;
    });

    private SegundoPlano() {
    }

    /**
     * Corre {@code trabalho} em segundo plano e entrega o resultado na thread do JavaFX.
     *
     * @param descricao  nome da operação, usado no registo do tempo (ex.: "Reservas ativas")
     * @param trabalho   operação demorada; não pode mexer no ecrã
     * @param aoTerminar recebe o resultado, já na thread do JavaFX
     * @param aoFalhar   recebe o erro, já na thread do JavaFX (por exemplo, sem ligação à base de dados)
     * @param <T>        tipo do resultado
     */
    public static <T> void executar(String descricao, Supplier<T> trabalho,
                                    Consumer<T> aoTerminar, Consumer<RuntimeException> aoFalhar) {
        Task<T> tarefa = new Task<>() {
            @Override
            protected T call() {
                long inicio = System.nanoTime();
                try {
                    return trabalho.get();
                } finally {
                    long ms = (System.nanoTime() - inicio) / 1_000_000;
                    LOG.info(() -> "Tempo de resposta - " + descricao + ": " + ms + " ms");
                }
            }
        };
        tarefa.setOnSucceeded(evento -> aoTerminar.accept(tarefa.getValue()));
        tarefa.setOnFailed(evento -> {
            // O ecrã só mostra uma mensagem curta; o erro completo (com a causa) fica na consola.
            LOG.log(Level.WARNING, "Falhou - " + descricao, tarefa.getException());
            aoFalhar.accept(comoRuntime(tarefa.getException()));
        });
        EXECUTOR.execute(tarefa);
    }

    /** Os erros chegam como {@link Throwable}; os ecrãs só tratam {@link RuntimeException}. */
    private static RuntimeException comoRuntime(Throwable erro) {
        if (erro instanceof RuntimeException e) {
            return e;
        }
        return new IllegalStateException("Ocorreu um erro inesperado. Tente novamente.", erro);
    }
}
