## 1. Geral
A Elly é um comando para TVs LG com webOS na rede local, no telemóvel e num relógio Wear OS.

O programador desta app não recolhe, armazena nem trata quaisquer dados pessoais. A app não requer uma conta de utilizador.

## 2. Comunicação de rede
A app comunica com a TV exclusivamente dentro da tua rede local:

- Pesquisa de TVs: pedido SSDP e verificação dos endereços da tua rede local quanto às portas webOS 3000 e 3001.
- Controlo: ligação encriptada à TV emparelhada (TVs mais antigas sem encriptação através da porta 3000). A app transmite comandos de controlo, como teclas, movimentos do ponteiro, abertura de apps e mudanças de entrada, e consulta as apps instaladas, as entradas e os ícones das apps.
- Ligar: pacote Wake-on-LAN para o endereço MAC da TV.
- Guia de programação (opcional, apenas para TVs com acesso root configurado por ti): ligação SSH à TV (porta 22). A aplicação lê listas de favoritos, canais, dados de programação e lembretes e gravações agendados da TV, agenda ou apaga lembretes e gravações na TV e lê logótipos de canais. Os logótipos que a TV ainda não tem são descarregados pela própria TV a partir do servidor de logótipos da LG, tal como para o seu próprio guia; o teu dispositivo não se liga a esse servidor. O mesmo se aplica às imagens dos programas provenientes dos dados de programação online da LG: quando abres os detalhes de um programa, a TV descarrega a imagem a partir do servidor da LG e transmite-a; o teu dispositivo apenas a mantém na memória enquanto a aplicação está aberta.

Esta comunicação ocorre diretamente entre o teu dispositivo e a TV. Não são transmitidos dados para servidores externos.

## 3. Relógio (Wear OS)
Tal como o telemóvel, a app do relógio controla a TV diretamente através do teu Wi-Fi (comandos de controlo, movimentos do ponteiro, ligar via Wake-on-LAN, abrir o seletor de entradas da TV).

Para que não tenhas de emparelhar a TV outra vez no relógio, o telemóvel transfere os seguintes dados para o relógio emparelhado enquanto a Elly estiver lá instalada:

- nome, modelo, endereço IP e endereço MAC das TVs emparelhadas, a sua chave de emparelhamento e a impressão digital do certificado
- que TV está ativa e se o modo de demonstração está ligado
- as páginas e a disposição das teclas da app do relógio e os ícones das apps que as teclas do relógio abrem

A transferência utiliza a interface Wearable dos serviços Google Play, que liga o telemóvel e o relógio. Se o telemóvel e o relógio não estiverem ligados diretamente por Bluetooth ou Wi-Fi, os serviços Google Play podem transferir os dados entre os teus dispositivos de forma encriptada através dos servidores da Google. Assim que deixar de ser conhecido um relógio com a Elly, o telemóvel apaga os dados desta interface. As credenciais do acesso root e o guia de programação não são transferidos para o relógio.

## 4. Armazenamento de dados
Os seguintes dados são guardados localmente no teu dispositivo:

- Nome, modelo, endereço IP e endereço MAC das TVs emparelhadas
- Chave de emparelhamento e impressão digital do certificado da TV
- Definições da app (ecrãs, design, teclas de app, ponteiro, vibração, modo de demonstração, páginas e teclas do relógio)
- cópias das imagens que escolhes como fundo de ecrã; a app só recebe as imagens que selecionas no seletor de fotos do Android
- Ícones das apps carregados a partir da TV
- para o guia de programação: utilizador SSH, porta e impressão digital da chave SSH da TV; a palavra-passe de root da TV ou a chave SSH privada que introduziste (com a respetiva frase-passe), encriptadas com uma chave guardada no Android Keystore que não pode ser lida para fora do dispositivo; os dados de programação (listas de canais e programas das últimas 24 horas e até 14 dias à frente) e logótipos de canais.

No relógio, a Elly guarda os dados transferidos do telemóvel (ver a secção 3) e as definições do relógio (vibração, gesto circular, efeito de luz, ponteiro, TV escolhida).

Estes dados permanecem nos teus dispositivos, estão excluídos das cópias de segurança do Android e são apagados quando a app é desinstalada.

## 5. Sensores
No modo de movimento do ponteiro, a app utiliza o giroscópio do teu dispositivo. No relógio, a página do ponteiro utiliza o giroscópio do relógio enquanto um dedo está pousado no ecrã. Os dados do sensor só são processados nesse momento, convertidos diretamente em movimentos do ponteiro e não são guardados.

## 6. Permissões
No telemóvel:

- Internet: ligação à TV na rede local (o Android exige esta permissão para qualquer ligação de rede)
- Estado da rede e do Wi-Fi: determinar a rede local para a pesquisa
- Multicast Wi-Fi: receber as respostas ao procurar TVs
- Vibração: feedback curto ao premir uma tecla num widget do ecrã principal
- Serviço em primeiro plano: enviar para a TV uma tecla premida num widget do ecrã principal, também enquanto a app está em segundo plano

No relógio:

- Internet e estado da rede: ligação à TV na rede local
- Alterar a rede: o relógio liga-se ao teu Wi-Fi para o controlo em vez de encaminhar a ligação pelo telemóvel
- Vibração: feedback nas teclas e enquanto o volume muda
- Manter ativo: mantém o Wi-Fi com resposta rápida na página do ponteiro

Não são usadas outras permissões, em particular nenhum acesso à localização, ao microfone, à câmara ou aos contactos.

## 7. Terceiros
Esta app não utiliza ferramentas de análise nem publicidade. Para sincronizar o telemóvel e o relógio utiliza a interface Wearable dos serviços Google Play (ver a secção 3); a política de privacidade da Google também se aplica a ela.

## 8. Alterações a esta política de privacidade
Esta política de privacidade pode ser atualizada, se necessário, para refletir alterações na app ou requisitos legais.

## 9. Contacto
Se tiveres alguma questão sobre privacidade, podes contactar o programador.

E-mail: elly@robnice.de
