## 1. Geral
A Elly é um comando para TVs LG com webOS na rede local.

O programador desta app não recolhe, armazena nem trata quaisquer dados pessoais. A app não requer uma conta de utilizador.

## 2. Comunicação de rede
A app comunica exclusivamente dentro da tua rede local:

- Pesquisa de TVs: pedido SSDP e verificação dos endereços da tua rede local quanto às portas webOS 3000 e 3001.
- Controlo: ligação encriptada à TV emparelhada (TVs mais antigas sem encriptação através da porta 3000). A app transmite comandos de controlo, como teclas, movimentos do ponteiro, abertura de apps e mudanças de entrada, e consulta as apps instaladas, as entradas e os ícones das apps.
- Ligar: pacote Wake-on-LAN para o endereço MAC da TV.

Esta comunicação ocorre diretamente entre o teu dispositivo e a TV. Não são transmitidos dados para servidores externos.

## 3. Armazenamento de dados
Os seguintes dados são guardados localmente no teu dispositivo:

- Nome, modelo, endereço IP e endereço MAC das TVs emparelhadas
- Chave de emparelhamento e impressão digital do certificado da TV
- Definições da app (ecrãs, design, teclas de app, ponteiro, vibração, modo de demonstração)
- cópias das imagens que escolhes como fundo de ecrã; a app só recebe as imagens que selecionas no seletor de fotos do Android
- Ícones das apps carregados a partir da TV

Estes dados permanecem exclusivamente no teu dispositivo, estão excluídos das cópias de segurança do Android e são apagados quando a app é desinstalada.

## 4. Sensores
No modo de movimento do ponteiro, a app usa o giroscópio do teu dispositivo. Os dados do sensor só são tratados enquanto o modo de ponteiro está ativo e a app está visível, são convertidos diretamente em movimentos do ponteiro e não são guardados.

## 5. Permissões
- Internet: ligação à TV na rede local (o Android exige esta permissão para qualquer ligação de rede)
- Estado da rede e do Wi-Fi: determinar a rede local para a pesquisa
- Multicast Wi-Fi: receber as respostas ao procurar TVs
- Vibração: feedback curto ao premir uma tecla num widget do ecrã principal
- Serviço em primeiro plano: enviar para a TV uma tecla premida num widget do ecrã principal, também enquanto a app está em segundo plano

Não são usadas outras permissões, em particular nenhum acesso à localização, ao microfone, à câmara ou aos contactos.

## 6. Terceiros
Esta app não utiliza serviços de terceiros, ferramentas de análise nem publicidade.

## 7. Alterações a esta política de privacidade
Esta política de privacidade pode ser atualizada, se necessário, para refletir alterações na app ou requisitos legais.

## 8. Contacto
Se tiveres alguma questão sobre privacidade, podes contactar o programador.

E-mail: elly@robnice.de
