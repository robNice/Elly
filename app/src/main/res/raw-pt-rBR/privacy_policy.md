## 1. Geral
A Elly é um controle remoto para TVs LG com webOS na rede local, no celular e em um relógio Wear OS.

O desenvolvedor deste app não coleta, armazena nem processa nenhum dado pessoal. O app não exige uma conta de usuário.

## 2. Comunicação de rede
O app se comunica com a TV exclusivamente dentro da sua rede local:

- Busca de TVs: solicitação SSDP e verificação dos endereços da sua rede local quanto às portas webOS 3000 e 3001.
- Controle: conexão criptografada com a TV pareada (TVs mais antigas sem criptografia pela porta 3000). O app transmite comandos de controle, como botões, movimentos do ponteiro, abertura de apps e trocas de entrada, e consulta os apps instalados, as entradas e os ícones dos apps.
- Ligar: pacote Wake-on-LAN para o endereço MAC da TV.
- Guia de programação (opcional, apenas para TVs com acesso root configurado por você): conexão SSH com a TV (porta 22). O app lê listas de favoritos, canais, dados de programação e lembretes e gravações agendados da TV, agenda ou apaga lembretes e gravações na TV e lê logotipos de canais. Logotipos que a TV ainda não tem são baixados pela própria TV do servidor de logotipos da LG, como para o guia dela; seu dispositivo não se conecta a esse servidor. O mesmo vale para as imagens dos programas dos dados de programação online da LG: quando você abre os detalhes de um programa, a TV baixa a imagem do servidor da LG e a repassa; seu dispositivo a mantém apenas na memória enquanto o app está aberto.

Essa comunicação acontece diretamente entre o seu dispositivo e a TV. Nenhum dado é transmitido para servidores externos.

## 3. Relógio (Wear OS)
Assim como o celular, o app do relógio controla a TV diretamente pelo seu Wi-Fi (comandos, movimentos do ponteiro, ligar via Wake-on-LAN, abrir o seletor de entradas da TV).

Para que você não precise parear a TV de novo no relógio, o celular transfere os seguintes dados para o relógio pareado enquanto a Elly estiver instalada nele:

- nome, modelo, endereço IP e endereço MAC das TVs pareadas, a chave de pareamento e a impressão digital do certificado
- qual TV está ativa e se o modo demo está ligado
- as páginas e o layout dos botões do app do relógio e os ícones dos apps que os botões do relógio abrem

A transferência usa a interface Wearable dos serviços do Google Play, que conecta o celular e o relógio. Se o celular e o relógio não estiverem conectados diretamente por Bluetooth ou Wi-Fi, os serviços do Google Play podem transferir os dados entre seus dispositivos de forma criptografada pelos servidores do Google. Assim que nenhum relógio com a Elly for mais conhecido, o celular apaga os dados dessa interface. As credenciais do acesso root e o guia de programação não são transferidos para o relógio.

## 4. Armazenamento de dados
Os seguintes dados são armazenados localmente no seu dispositivo:

- Nome, modelo, endereço IP e endereço MAC das TVs pareadas
- Chave de pareamento e impressão digital do certificado da TV
- Configurações do app (telas, design, botões de app, ponteiro, vibração, modo demo, páginas e botões do relógio)
- cópias das imagens que você escolhe como fundo de tela; o app só recebe as imagens que você seleciona no seletor de fotos do Android
- Ícones dos apps carregados da TV
- para o guia de programação: usuário SSH, porta e impressão digital da chave SSH da TV; a senha de root da TV ou a chave SSH privada que você informou (com a frase secreta), criptografadas com uma chave guardada no Android Keystore que não pode ser extraída do dispositivo; os dados de programação (listas de canais e programas das últimas 24 horas e até 14 dias à frente) e logotipos de canais.

No relógio, a Elly armazena os dados transferidos do celular (veja a seção 3) e as configurações do relógio (vibração, gesto circular, efeito de luz, ponteiro, TV escolhida).

Esses dados ficam nos seus dispositivos, são excluídos dos backups do Android e são apagados quando o app é desinstalado.

## 5. Sensores
No modo de movimento do ponteiro, o app usa o giroscópio do seu dispositivo. No relógio, a página do ponteiro usa o giroscópio do relógio enquanto um dedo está apoiado na tela. Os dados do sensor só são processados nesse momento, convertidos diretamente em movimentos do ponteiro e não são armazenados.

## 6. Permissões
No celular:

- Internet: conexão com a TV na rede local (o Android exige essa permissão para qualquer conexão de rede)
- Estado da rede e do Wi-Fi: identificar a rede local para a busca
- Multicast Wi-Fi: receber as respostas ao buscar TVs
- Vibração: feedback curto ao tocar em um botão de um widget da tela inicial
- Serviço em primeiro plano: enviar para a TV um botão tocado em um widget da tela inicial, também enquanto o app está em segundo plano

No relógio:

- Internet e estado da rede: conexão com a TV na rede local
- Alterar a rede: o relógio se conecta ao seu Wi-Fi para o controle em vez de passar a conexão pelo celular
- Vibração: feedback nos botões e enquanto o volume muda
- Manter ativo: mantém o Wi-Fi respondendo rápido na página do ponteiro

Nenhuma outra permissão é usada, em especial nenhum acesso à localização, ao microfone, à câmera ou aos contatos.

## 7. Terceiros
Este app não usa ferramentas de análise nem publicidade. Para sincronizar o celular e o relógio, ele usa a interface Wearable dos serviços do Google Play (veja a seção 3); a política de privacidade do Google também se aplica a ela.

## 8. Alterações nesta política de privacidade
Esta política de privacidade pode ser atualizada, se necessário, para refletir mudanças no app ou exigências legais.

## 9. Contato
Se você tiver alguma dúvida sobre privacidade, pode entrar em contato com o desenvolvedor.

E-mail: elly@robnice.de
