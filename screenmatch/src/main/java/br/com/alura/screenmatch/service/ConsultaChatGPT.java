package br.com.alura.screenmatch.service;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;


public class ConsultaChatGPT {

        private static final String API_KEY = "";

        public static String obterTraducao(String texto) {
            if (API_KEY == null || API_KEY.isEmpty()) {
                return "[ERRO] API KEY não configurada.";
            }

            OpenAiService service = new OpenAiService(API_KEY);

            CompletionRequest requisicao = CompletionRequest.builder()
                    .model("gpt-3.5-turbo-instruct")
                    .prompt("traduza para o português o texto: " + texto)
                    .maxTokens(1000)
                    .temperature(0.7)
                    .build();

            try {
                var resposta = service.createCompletion(requisicao);
                return resposta.getChoices().get(0).getText().trim();
            } catch (OpenAiHttpException e) {
                if (e.getMessage().contains("quota")) {
                    return "[ERRO] Limite de uso da API foi excedido. Verifique seu plano no site da OpenAI.";
                }
                return "[ERRO] Falha ao consultar a API: " + e.getMessage();
            } catch (Exception e) {
                return "[ERRO] Erro inesperado: " + e.getMessage();
            }
        }
}