# Staging do NovelCopilot em uma VM do Azure

Este guia explica a implantação de staging como material de estudo. Ele parte de
uma VM Ubuntu 24.04 `Standard_B2ats_v2`, com 1 GiB de RAM, 2 GiB de swap, disco
Premium P6 de 64 GiB e Docker. O MySQL continua no Aiven e a autenticação no
Supabase.

## O modelo mental

Há quatro peças independentes:

1. **GitHub**: guarda o código e executa os testes.
2. **GitHub Actions**: constrói a imagem, para não compilar Java na VM pequena.
3. **GitHub Container Registry (GHCR)**: armazena a imagem Docker pronta.
4. **VM do Azure**: baixa a imagem e mantém o contêiner em execução.

Uma imagem é um pacote imutável com Java, o JAR e as instruções de inicialização.
Um contêiner é uma execução dessa imagem. Apagar um contêiner não apaga o código
no GitHub, o banco no Aiven nem a imagem no GHCR.

## Por que a imagem é construída fora da VM

O Maven e o compilador Java consomem memória e CPU. A VM possui somente 1 GiB de
RAM, então compilar nela concorreria com o sistema operacional e poderia acionar
a swap intensamente ou ser encerrado por falta de memória. O workflow
`backend-image.yml` usa uma máquina temporária do GitHub para:

1. baixar o código;
2. preparar Java 21;
3. executar os testes do backend;
4. construir a imagem para `linux/amd64`;
5. publicar duas tags: `latest` e `sha-<commit>`.

`latest` facilita o staging. A tag `sha-...` identifica exatamente o commit e
permite voltar para uma versão anterior sem reconstruí-la.

## Segurança das credenciais

O arquivo `.env.example` contém apenas nomes e valores fictícios. Na VM ele deve
ser copiado para `.env`, que nunca deve ser enviado ao Git. Credenciais reais não
entram na imagem, no workflow, no histórico do terminal compartilhado ou em
capturas de tela.

Permissões recomendadas para o arquivo real:

```bash
chmod 600 .env
```

Isso permite leitura e escrita apenas pelo proprietário. Se uma credencial for
exposta, ela deve ser revogada no provedor e substituída; apagar a mensagem ou o
arquivo não desfaz a exposição.

## Limites adequados à VM pequena

O `compose.yaml` aplica quatro proteções:

- `mem_limit: 768m`: impede o contêiner de consumir toda a memória da VM;
- `-Xmx480m`: limita o heap, a principal área de memória do Java;
- `UseSerialGC`: usa um coletor de lixo simples, adequado a pouca memória;
- `restart: unless-stopped`: reinicia o backend após falha ou reboot da VM.

A swap de 2 GiB é uma rede de proteção, não uma substituição de RAM. Uso contínuo
de swap significa que a aplicação precisa ser otimizada ou a VM redimensionada.

Os logs do Docker são limitados a três arquivos de 10 MB. Sem rotação, logs
podem crescer até ocupar o disco e derrubar o servidor.

## Primeira publicação da imagem

Depois que estes arquivos estiverem na branch `main`, abra no GitHub:

1. **Actions**;
2. **Publicar imagem do backend**;
3. **Run workflow**, se o push não tiver iniciado automaticamente;
4. aguarde todos os passos ficarem verdes.

Abra o pacote `novelcopilot-backend` no perfil do GitHub. Para a VM baixar sem
credenciais, a visibilidade do pacote precisa ser **Public**. Se o repositório ou
o pacote permanecer privado, use um token de acesso somente com `read:packages`
e faça `docker login ghcr.io`; nunca use a senha da conta.

## Preparação do diretório na VM

Crie um diretório pertencente ao usuário administrativo:

```bash
sudo mkdir -p /opt/novelcopilot
sudo chown azureuser:azureuser /opt/novelcopilot
cd /opt/novelcopilot
```

Coloque nesse diretório `compose.yaml` e uma cópia de `.env.example` chamada
`.env`. Preencha o `.env` diretamente na VM e proteja-o:

```bash
chmod 600 .env
```

Antes de iniciar, valide a sintaxe sem imprimir os valores secretos:

```bash
docker compose config --quiet
```

## Iniciar e validar

### Bloqueio atual: banco do Aiven

O banco do Aiven está defasado em relação ao ambiente de desenvolvimento. Antes
do primeiro `docker compose up -d`, faça backup e compare a tabela de histórico
do Flyway com as migrations do repositório. Na inicialização, o Flyway aplica
migrations de schema pendentes, mas não copia dados do banco de desenvolvimento.
Qualquer carga ou substituição de dados deve ser planejada e validada
separadamente para evitar perda ou mistura entre ambientes.

```bash
docker compose pull
docker compose up -d
docker compose ps
docker compose logs --tail=100 backend
curl --fail http://127.0.0.1:8080/health
```

O `pull` baixa a imagem pronta. O `up -d` reconcilia o estado desejado em segundo
plano. O teste em `127.0.0.1` funciona apenas dentro da VM, pois a porta 8080 não
é publicada diretamente na Internet.

## Por que a API ainda não fica pública

O mapeamento `127.0.0.1:8080:8080` impede acesso externo direto ao Spring Boot.
Na próxima etapa, um proxy reverso receberá conexões nas portas 80 e 443,
providenciará HTTPS e encaminhará internamente para a porta 8080. Assim, a API
não expõe uma porta de desenvolvimento nem trafega credenciais sem criptografia.

## Atualizar, voltar e parar

Atualizar para a imagem mais recente:

```bash
cd /opt/novelcopilot
docker compose pull
docker compose up -d
docker image prune -f
```

Para fixar ou restaurar uma versão, altere `IMAGE_TAG` no `.env` para a tag
`sha-...` desejada e execute novamente `docker compose pull` e
`docker compose up -d`.

Parar o aplicativo sem apagar a configuração:

```bash
docker compose stop
```

Remover apenas o contêiner e a rede criada pelo Compose:

```bash
docker compose down
```

## Diagnóstico básico

```bash
docker compose ps
docker compose logs --tail=200 backend
docker stats --no-stream
free -h
swapon --show
df -h /
```

- `ps` mostra se o contêiner está executando ou reiniciando;
- `logs` mostra o motivo de falhas de inicialização;
- `stats` mostra CPU e memória do contêiner;
- `free` e `swapon` mostram pressão de memória;
- `df` mostra quanto resta no disco.

Nunca publique uma captura de logs sem verificar se ela contém URLs, tokens,
usuários ou outros dados sensíveis.

## Manutenção que deve virar hábito

- Verificar custos e alertas de orçamento no Azure semanalmente.
- Instalar atualizações de segurança do Ubuntu regularmente.
- Acompanhar RAM, swap, disco e reinícios do contêiner.
- Manter somente SSH, HTTP e HTTPS nas regras de entrada.
- Restringir SSH ao IP administrativo sempre que possível.
- Não guardar banco, obras ou backups somente no disco da VM.
- Testar restauração do banco externo antes de chamar o ambiente de produção.
- Excluir VM, disco, NIC e IP quando o ambiente não for mais necessário.

## O que aprender com este exercício

Ao final, você deve conseguir explicar:

1. a diferença entre computador local, VM, imagem e contêiner;
2. por que build e execução são responsabilidades separadas;
3. por que segredos não pertencem ao Git nem à imagem;
4. como DNS, IP, portas, firewall, proxy reverso e HTTPS se relacionam;
5. como limites de RAM, heap Java e swap evitam indisponibilidade;
6. como observar logs e recursos antes de tentar corrigir um problema;
7. como atualizar e reverter uma versão de forma reproduzível;
8. por que excluir a VM não basta se disco e IP continuarem existindo.

Este ambiente é staging de portfólio. Torná-lo produção exigiria política de
backup e restauração testada, monitoramento, gestão de segredos, revisão de
custos, proteção adicional de rede e um processo formal de incidentes.
