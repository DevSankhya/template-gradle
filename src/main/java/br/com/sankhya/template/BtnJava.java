package br.com.sankhya.template;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import com.sankhya.ce.http.Http;
import com.sankhya.ce.tuples.Triple;
import okhttp3.Headers;

import java.util.List;


public class BtnJava implements AcaoRotinaJava {
    @Override
    public void doAction(ContextoAcao contexto) throws Exception {
        // Get java version
        String javaVersion = System.getProperty("java.version");
        Triple<String, Headers, List<String>> post = Http.client.get("https://jsonplaceholder.typicode.com/todos/1");
        contexto.setMensagemRetorno(String.join(" ", new String[]{post.getFirst(), javaVersion}));
    }
}
