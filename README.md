# Project Reactor

Reactor é uma biblioteca interessada em disponibilizar uma API alternativa para lidar com o paradigma reativo e o uso de APIs não bloqueantes no universo JVM.

E ai você me pergunta, por que nós nos interessariamos por isso?

No nosso mundo atual, nós podemos estar lidando com milhares de usuários concorrentes, o que nos faz por várias vezes ter de lidar com questões de como melhor utilizar nossos recursos para minimizar a quantidade de tempo perdido de uma aplicação ou de um usuário.

Usando os nossos padrões bloqueantes, nós nos encontramos com alguns problemas ao tentar escalar uma aplicação: Ao utilizar chamadas IO(i.e. databases, requests HTTP, acessos a disco) nós criamos uma ineficiência em relação ao nosso tempos de máquina.

Isso se dá pois assim que fazemos a operação desejada, nós travamos a thread atual do programa para fazer o pedido. Como essas operações tendem a ter tempo de resposta maior que o usual, isso tira a oportunidade de executarmos outros fragmentos de códigos que tenham menos overhead, desperdiçando recursos que poderiam estar sendo utilizados por outras operações.

Ao usar o paradigma reativo, nós permitimos que enquanto nós esperamos o progresso das chamadas IO, nós possamos fazer progresso em outras frentes, pois o trabalho executado por padrão não será irá esperar parado pelos resultados, mas sim **reagirá** ao resultado uma vez que este tenha completado.

## Lidando com Reactor

Para criar um processo reativo, primeiro precisamos definir as caracteristicas do fluxo de dados que lidaremos. Quem define essas caracteristicas é o `Publisher`, que permite em cima dele definirmos as nossas respostas aos resultados anteriores.

Para iniciar, falaremos sobre o `Mono`, o `Mono` é um publicador que por definição somente irá enviar um dado, o qual chamarei de evento daqui em diante.

Quando criando ou lidando com esse tipo de publicador, ele não irá fazer nenhuma operação por padrão, para podermos executa-lo, nós precisamos inscrever nele.

```kotlin
private fun `creating and executing a Mono Stream`() {

    println("Creating Mono publisher justOne")
    val justOne = Mono.fromSupplier { 
        println("Generating the number one")
        1
    }

    println("Subscribing to justOne")
    justOne.subscribe { event -> printEvent(event) }
}

private fun printEvent(event: Any) {
    println("Event content is $event")
}
```

![Execução de justOne](images/justOne.png) 

Esse é um detalhe muito importante de se lembrar, pois no caso estramos lidando com um `Cold Publisher`. Esse tipo de publicador é bem próximo de um Runnable`, é uma sequência de código que será executado toda vez que alguém se inscrever. Isso pode ser observado no próximo fragmento de código:

```kotlin
private fun `creating and executing a Mono Stream that has logic inside`() {

    println("Creating Mono publisher randomNumber")
    val randomNumber = Mono.fromSupplier {
        println("Generating a random number")
        Random.nextInt(0, 255)
    }

    println("Subscribing to randomNumber one time")
    randomNumber.subscribe { event -> printEvent(event) }

    println("Subscribing to randomNumber a second time")
    randomNumber.subscribe { event -> printEvent(event) }
}
```

Nesse exemplo nós nos inscrevemos duas vezes ao mesmo publicador. Dado isso, ele executa o código dado toda vez que alguém se isncreve a este:

![Execução de multipleCallsCold](images/multipleCallsCold.png)

Porém além destes, nós temos também o conceito de `Hot Publisher`. Quando nós estamos lidando com estes, a operação só é executada uma vez e passada a frente toda vez que chamada.

```kotlin
private fun `creating and executing a hot Mono Stream that has logic inside`() {

    println("Creating Mono publisher randomNumber")
    val randomNumber = Mono.fromSupplier {
        println("Generating a random number")
        Random.nextInt(0, 255)
    }

    println("Caching a random generated number")
    val cachedRandomNumber = randomNumber.cache()

    println("Subscribing to cachedRandomNumber one time")
    cachedRandomNumber.subscribe { event -> printEvent(event) }

    println("Subscribing to cachedRandomNumber a second time")
    cachedRandomNumber.subscribe { event -> printEvent(event) }
}
```

![Execução de multipleCallsHot](images/multipleCallsHot.png)

Como perceptível, nós começamos nosso publicador como um `Cold Publisher` e por meio de um operador, no caso o `cached`, nós transformamos ele em um `Hot Publisher`, que já teve seu resultado pré-computado.

Isso ocorre pois a cada operador que adicionamos, nós criamos mais um publicador que está ligado ao anterior. Vide o próximo exemplo:

```kotlin
private fun `executing a hot Mono Stream and the cold stream that provides for it`() {

    println("Creating Mono publisher randomNumber")
    val randomNumber = Mono.fromSupplier {
        println("Generating a random number")
        Random.nextInt(0, 255)
    }

    println("Caching a random generated number")
    val cachedRandomNumber = randomNumber.cache()

    println("Subscribing to cachedRandomNumber one time")
    cachedRandomNumber.subscribe { event -> printEvent(event) }

    println("Subscribing to randomNumber one time")
    randomNumber.subscribe { event -> printEvent(event) }

    println("Subscribing to cachedRandomNumber a second time")
    cachedRandomNumber.subscribe { event -> printEvent(event) }
}
```

![Execução de twoLevelPublisher](images/twoLevelPublisher.png)

E após falarmos tanto sobre operações paralelas na introdução, nós nos perguntamos

```kotlin
private fun `executing a Mono Stream on another Thread`() {

    println("Creating Mono publisher originalStream")
    val originalStream = Mono.fromSupplier {
        println("Supplying on ${Thread.currentThread().name}")
        1
    }

    println("Declaring Subscription Thread on ${getThreadName()}")
    val subscribedOn = originalStream.subscribeOn(Schedulers.elastic())

    println("Declaring onNext on ${getThreadName()}")
    val onNexted = subscribedOn.doOnNext { event -> printThreaded(event, "doOnNext") }

    println("Declaring Publishing Thread on ${getThreadName()}")
    val publishedOn = onNexted.publishOn(Schedulers.newParallel("parallel"))

    println("Subscribing on ${getThreadName()}")
    val disposable = publishedOn.subscribe { event -> printThreaded(event, "subscribe") }

    while (!disposable.isDisposed) { }
}
```

![Execução de threading](images/threading.png)

Tudo isso é ótimo, porém temos uma preocupação: O que ocorre caso um erro ocorrer?

```kotlin
private fun `executing a failed Mono Stream`() {

    println("Creating Mono publisher randomNumber")
    val throwingStream = Mono.fromSupplier<Int> {
        throw IllegalAccessException("You shall not pass")
    }

    println("Subscribing to a not guarded throwingStream one time")

    try {
        throwingStream.subscribe { event -> printEvent(event) }
    } catch (e: IllegalAccessException) {
        println("Captured an IllegalAccessException: `${e.message}`")
    } catch (e: UnsupportedOperationException) {
        println("Captured an UnsupportedOperationException: `${e.message}`")
    }

    println("Subscribing to a guarded throwingStream one time")

    throwingStream
        .doOnError { error -> println("We successfully guarded our flow from `${error.message}`") }
        .onErrorReturn(1)
        .subscribe { event -> printEvent(event) }
}
```

![Execução de failingMono](images/failingMono.png)

Como percebido, o Reactor joga uma exceção em caso de erros internos, a não ser que nós nos defendamos com algum tratamento para erros.



Porém por várias vezes, temos de lidar com um fluxo de eventos, e não somente um evento.

# TL;DR

## Conceitos

### Modelos

- Event - Um sinal que será propagado pelo fluxo.
- Publisher - Um objeto que pode publicar Events. 
- Mono - Publisher que enviará somente um evento ou um erro.
- Flux - Publisher que enviará 0..N eventos e/ou um erro.

### Tipos de Inscrição

- Hot Stream/Publisher - Inscrição na qual os eventos são enviados independente de se alguém está ouvindo o publisher.
- Cold Stream/Publisher - Inscrição na qual os eventos são iniciados somente a partir do momento que alguém se inscreve no publisher.


## Pros and Cons

### Prós
- Observável
- Não bloqueante
- Fácil para quem entende `Rx`
- Baseado na mesma especificação que `RxJava` segue, a `Reactive Streams`

### Contras
- Fluxo confuso a quem acostumado com programação procedural, orientada a objetos ou com eager evaluation
- Stacktrace inchada e dificil de acompanhar


# Fontes
- [Documentação - Project Reactor](https://projectreactor.io/docs/core/release/reference/index.html#about-doc)

## Recomendações
- [Which operator do I need? - Project Reactor](https://projectreactor.io/docs/core/release/reference/index.html#which-operator)