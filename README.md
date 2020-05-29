# ReliableUDP


## Como compilar e executar

Para compilar o código, basta abrir um terminal, estar no diretório `src` dentro da estrutura de pasta do projeto e executar o comando `javac com/ufabc/Client.java com/ufabc/Server.java`, a compilação foi realizada tanto com a versão 8 quanto a 14 do java, ficando a critério do usuário qual utilizar. Após a compilação, é necessário abrir dois terminais e novamente no diretório `src`, em um terminal é necessário executar o comando `java com.ufabc.Server` para iniciar o servidor e no outro `java com.ufabc.Client`.

Para utilizar o código basta seguir as instruções exibitas no terminal executando o `Client`, no caso, o envio da mensagem segue o padrão de: número de caso + espaço + texto da mensagem. Algumas strings de exemplo podem ser: "5 Olá", "1 Olá Mundo", "2 Teste", etc.

## Formato da mensagem transferida

A mensagem nessa implementação está envolta por um objeto do tipo `Packet`, esse objeto junto com o conteúdo da mensagem possui alguns campos extras para uso tanto pelo cliente quanto pelo servidor. O primeiro campo é um vetor de bytes que contém a mensagem a ser enviada, o segundo um inteiro representando o número de sequência. Os dois valores booleanos servem para indicar ao cliente se o pacote foi *acked* ou não e se ele foi ou não enviado. Também importante notar que o objeto implementa a interface `Serializable`, para que seja possível transforma-lo em um vetor de bytes.

## Uso de buffers para representar janelas

- **Buffers no cliente:** O cliente utiliza duas estruturas de dados que podem ser identificadas como buffers, a primeira é uma lista ligada **FIFO** que serve como a fila de mensagens a serem enviadas e que ainda não entaram na janela do algoritmo. A segunda estrutura é uma lista de tamanho fixo e concorrente `ArrayBlockingQueue` utilizada como a janela propriamente dita do algoritmo *Selective Repeat*. Essa janela armazena os pacotes que foram enviados, estão prestes a serem enviados e que ainda aguardam a mensagem de **ACK** pelo servidor. Ao requisitar o envio de uma mensagem pelo método `sendMessage()` o cliente primeiro produz o pacote que encapsula a mensagem, em seguida coloca esse pacote no final da fila e chama o método `updateWindow()`, que por sua vez remove da janela qualquer pacote que já tenha recebido seu **ACK** assim como adiciona a partir da fila um pacote novo a janela, caso haja espaço nela.

-  **Buffer no servidor:** O servidor utiliza uma lista de tamanho fixo para representar a janela do destinatário no algoritmo *Selective Repeat*, essa janela armazena todos os pacotes recebidos até eles estarem prontos para entrega para as camadas superiores, no caso, isso significa que dentro da janela um pacote permanece armazenado até todos os pacotes com número de sequência inferior ao seu terem sido recebidos.

## Tratamento de mensagens fora de ordem

Uma mensagem ao chegar ao destinatário é armazenada em seu buffer de janela, nesse buffer, a cada mensagem recebida o servidor ordena elas de acordo com o número de sequência de cada pacote e verifica se a primeira mensagem possui o número de sequência base para essa janela, se sim, o servidor entrega essa mensagem para a camada superior, soma 1 ao número de sequência base da janela e verifica a próxima mensagem. Esse processo é repetido até não ter mais pacotes a serem avaliados ou ser encontrado um pacote com número de sequência superior ao número de sequência base da janela. Consequentemente, um pacote fora de ordem recebido, só sera entregue a camada superior quando todos os pacotes com número de sequência inferior a ele estiverem também sido recebidos.

## Tratamento de mensagens perdidas

Toda mensagem, ao ser enviada pelo remetente é acompanhada pela criação de um objeto `TimeoutTask`. Esse objeto é uma Thread que após um tempo pré-estabelecido verifica se o pacote ao qual está associado recebeu ou não o seu **ACK**, caso não tenha recebido, esse objeto marca o pacote ainda na janela como "não enviado". Consequentemente, na próxima iteração de envio de pacotes na janela o pacote será reenviado, inclusive com um novo `TimeoutTask` para acompanhar seu ciclo de vida dentro do cliente. Ou seja, caso um pacote seja perdido (simulado por não ser enviado), o seu timeout expirará e ele será reenviado na próxima oportunidade.

## Tratamento de mensagens duplicadas

O servidor, ao receber uma mensagem registra em uma estrutura de dado do tipo `HashSet` o número de sequência desse pacote e toda vez que um pacote é recebido verifica se o número de sequência está dentro dessa estrutura de dados. Caso o número de sequência de um pacote recém recebido esteja 

## Tratamento de mensagens lentas

O tratamento de mensagens lentas é essencialmente uma extensão do tratamento de mensagens duplicadas. Uma mensagem lenta é simulada por no momento de envio, o cliente primeiro ativar o timer de timeout e em seguida após esperar um período de tempo superior ao do timeout, envia o pacote. O que ocorre é que no evento de um timeout devido a demora de envio do pacote, o campo que indicaria que o pacote foi enviado é retornado ao estado de falso e consequentemente o pacote seria reenviado na próxima iteração de envio de pacotes. A partir dessa situação podem ocorrer duas sequẽncias de eventos. A primeira acontece no caso da próxima iteração de envio ser iniciada antes do remetente receber o **ACK** do pacote lento, consequentemente o pacote será enviado novamente sem lentidão, o que implica em um envio duplicado e cairá no tratamento de mensagens duplicads descrito anteriormente. O segundo caso é resultado do **ACK** do pacote lento ser recebido antes da próxima iteração de envio, nesse evento, o campo que indica se um pacote dentro da janela recebeu ou não o **ACK** terá valor `true` e ao início de toda iteração de envio, o cliente remove da janela pacotes com esse estado e insere pacotes novos que estão na fila, portanto, o pacote não é reenviado nessa sequência de eventos.

## Materiais auxiliares utilizados

- https://www.baeldung.com/java-timer-and-timertask
- https://www.baeldung.com/java-start-thread
- https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ArrayBlockingQueue.html
- Diversas questões do stackoverflow sobre uso das estruturas de dados, interfaces e bibliotecas utilizadas.