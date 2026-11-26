// Projeto raiz: apenas declara a versão do plugin Kotlin para os submódulos.
// Cada módulo aplica o plugin e configura suas próprias dependências.
plugins {
    kotlin("jvm") version "2.4.20" apply false
}

allprojects {
    group = "br.ufg.inf"
    version = "1.0-SNAPSHOT"
}
