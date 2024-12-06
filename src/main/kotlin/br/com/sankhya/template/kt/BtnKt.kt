package br.com.sankhya.template.kt

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava
import br.com.sankhya.extensions.actionbutton.ContextoAcao
import com.sankhya.ce.http.Http

class BtnKt : AcaoRotinaJava {
    @Throws(Exception::class)
    override fun doAction(contexto: ContextoAcao) {
        val post = Http.client["https://jsonplaceholder.typicode.com/todos/1"]
        // Get java version
        val javaVersion = System.getProperty("java.version")
        // Get kotlin version
        val kotlinVersion = KotlinVersion.CURRENT.toString()
        contexto.setMensagemRetorno("${post.first} Kotlin version:\n$kotlinVersion")
    }
}
