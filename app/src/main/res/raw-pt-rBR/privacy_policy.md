## 1. Geral
A Elly é um controle remoto para TVs LG com webOS na rede local.

O desenvolvedor deste app não coleta, armazena nem processa nenhum dado pessoal. O app não exige uma conta de usuário.

## 2. Comunicação de rede
O app se comunica exclusivamente dentro da sua rede local:

- Busca de TVs: solicitação SSDP e verificação dos endereços da sua rede local quanto às portas webOS 3000 e 3001.
- Controle: conexão criptografada com a TV pareada (TVs mais antigas sem criptografia pela porta 3000). O app transmite comandos de controle, como botões, movimentos do ponteiro, abertura de apps e trocas de entrada, e consulta os apps instalados, as entradas e os ícones dos apps.
- Ligar: pacote Wake-on-LAN para o endereço MAC da TV.
- Guia de programação (opcional, apenas para TVs com acesso root configurado por você): conexão SSH com a TV (porta 22). O app lê listas de favoritos, canais, dados de programação e lembretes e gravações agendados da TV, agenda ou apaga lembretes e gravações na TV e lê logotipos de canais. Logotipos que a TV ainda não tem são baixados pela própria TV do servidor de logotipos da LG, como para o guia dela; seu dispositivo não se conecta a esse servidor.

Essa comunicação acontece diretamente entre o seu dispositivo e a TV. Nenhum dado é transmitido para servidores externos.

## 3. Armazenamento de dados
Os seguintes dados são armazenados localmente no seu dispositivo:

- Nome, modelo, endereço IP e endereço MAC das TVs pareadas
- Chave de pareamento e impressão digital do certificado da TV
- Configurações do app (telas, design, botões de app, ponteiro, vibração, modo demo)
- cópias das imagens que você escolhe como fundo de tela; o app só recebe as imagens que você seleciona no seletor de fotos do Android
- Ícones dos apps carregados da TV
- para o guia de programação: usuário SSH, porta e impressão digital da chave SSH da TV; a chave SSH do app, guardada no Android Keystore e que não pode ser extraída do dispositivo; os dados de programação (listas de canais e programas das últimas 24 horas e até 14 dias à frente) e logotipos de canais. A senha de root da TV é usada só uma vez para configurar o acesso e não é salva.

Esses dados ficam exclusivamente no seu dispositivo, são excluídos dos backups do Android e são apagados quando o app é desinstalado.

## 4. Sensores
No modo movimento do ponteiro, o app usa o giroscópio do seu dispositivo. Os dados do sensor só são processados enquanto o modo ponteiro está ativo e o app está visível, são convertidos diretamente em movimentos do ponteiro e não são armazenados.

## 5. Permissões
- Internet: conexão com a TV na rede local (o Android exige essa permissão para qualquer conexão de rede)
- Estado da rede e do Wi-Fi: identificar a rede local para a busca
- Multicast Wi-Fi: receber as respostas ao buscar TVs
- Vibração: feedback curto ao tocar em um botão de um widget da tela inicial
- Serviço em primeiro plano: enviar para a TV um botão tocado em um widget da tela inicial, também enquanto o app está em segundo plano

Nenhuma outra permissão é usada, em especial nenhum acesso à localização, ao microfone, à câmera ou aos contatos.

## 6. Terceiros
Este app não usa serviços de terceiros, ferramentas de análise nem publicidade.

## 7. Alterações nesta política de privacidade
Esta política de privacidade pode ser atualizada, se necessário, para refletir mudanças no app ou exigências legais.

## 8. Contato
Se você tiver alguma dúvida sobre privacidade, pode entrar em contato com o desenvolvedor.

E-mail: elly@robnice.de
