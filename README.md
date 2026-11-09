# Simulador de disputa de pênaltis

Aplicação desktop em JavaFX: cadastro e gerenciamento de jogadores e times, escalação ordenada de batedores e goleiro, e disputa de pênaltis contra a CPU.

## Requisitos

- JDK 17 ou superior
- MySQL 8 em execução local (`localhost:3306`)
- IntelliJ IDEA (opcional; também é possível executar pelo terminal)

O Maven Wrapper já está incluído no projeto; não é necessário instalar Maven. As bibliotecas JPA, EclipseLink e MySQL Connector estão na pasta `lib/`. O JavaFX e demais dependências são baixados pelo Maven na primeira compilação, portanto é necessário acesso à internet nessa etapa.

## Preparar o MySQL

1. Abra uma conexão administrativa ao MySQL:

   ```bash
   sudo mysql
   ```

   Se esse comando não funcionar, use `mysql -u root -p` e informe a senha administrativa do MySQL.

2. No prompt `mysql>`, crie um banco para o projeto e um usuário próprio da aplicação. Troque o texto da senha por uma senha local forte:

   ```sql
   CREATE DATABASE IF NOT EXISTS simulador_penaltis;
   CREATE USER IF NOT EXISTS 'penalty_app'@'localhost' IDENTIFIED BY 'troque_esta_senha';
   GRANT ALL PRIVILEGES ON simulador_penaltis.* TO 'penalty_app'@'localhost';
   FLUSH PRIVILEGES;
   EXIT;
   ```

   Se o usuário `penalty_app` já existir e você quiser definir uma nova senha, execute também:

   ```sql
   ALTER USER 'penalty_app'@'localhost' IDENTIFIED BY 'troque_esta_senha';
   ```

3. Na raiz do projeto, crie seu arquivo local de credenciais copiando o exemplo:

   ```bash
   cp .env.example .env
   ```

4. Abra `.env` e preencha com o mesmo usuário e senha criados no MySQL:

   ```properties
   PENALTY_DB_USER=penalty_app
   PENALTY_DB_PASSWORD=troque_esta_senha
   ```

   Não coloque aspas nos valores. O arquivo `.env` é local, ignorado pelo Git, e não deve ser enviado aos colegas. Cada pessoa cria o seu.

O banco configurado é `simulador_penaltis`. Ao iniciar, a aplicação cria as tabelas ausentes usando `CREATE TABLE IF NOT EXISTS`, preservando as tabelas e os dados que já existem. O usuário da aplicação precisa de permissão para criar tabelas nesse banco.

## Preparar elencos e disputar uma partida

1. Na aba **Times**, cadastre dois times e escolha as cores do goleiro e dos batedores; elas são usadas na partida.
2. Na aba **Jogadores**, cadastre jogadores, informe a altura em metros (de 1,00 m a 2,50 m) e associe-os ao time correspondente.
3. Na aba **Escalações**, selecione cada time, monte a lista de batedores e use **Subir**/**Descer** para definir a ordem. Escolha também o goleiro e salve a escalação. O goleiro também pode bater.
4. Na aba **Partida**, escolha seu time e o time da CPU e clique em **Iniciar partida**.
5. Na sua cobrança, o campo aparece em uma área maior, com o gol ao fundo e a bola mais distante; os marcadores dos jogadores mudam de tamanho proporcionalmente à altura informada, usando um gol de 2,50 m como referência e ficando apoiados pelos pés. O marcador do cobrador avança até a bola antes do chute. O eixo X se move primeiro. Clique em **Fixar X** no momento desejado; então o eixo Y começa a se mover, e o mesmo botão fixa Y. Maior Finalização deixa o movimento da mira mais lento e a bola mais rápida. Ajuste a força e clique em **Cobrar**: mais força também aumenta a velocidade da bola e reduz o tempo de reação do goleiro. Finalização e força afetam o tempo de voo; o nível de Goleiro determina a velocidade com que o goleiro se desloca para defender e também seu alcance. Na cobrança da CPU, escolha **Defender à esquerda**, **Defender no meio** ou **Defender à direita**. Cada equipe usa suas próprias cores: a cor do papel de goleiro ou de batedor, inclusive quando o goleiro cobra.

A disputa segue cinco cobranças por time, com encerramento antecipado quando o adversário não puder mais alcançar o placar; se empatar, continua em morte súbita. A ordem dos cobradores reinicia e se repete se um time tiver menos de cinco jogadores na lista. O andamento e os dados de cada cobrança ficam visíveis na partida atual; histórico persistente de partidas/replay será uma etapa posterior.

## Executar pelo terminal

Na raiz do projeto, rode:

```bash
./mvnw clean test
./mvnw javafx:run
```

O primeiro comando compila e executa os testes das regras de negócio e da mecânica da partida sem conectar ao MySQL. O segundo inicia a interface e precisa de um `.env` válido e do MySQL em execução.

No Windows, use o wrapper correspondente:

```bat
mvnw.cmd clean test
mvnw.cmd javafx:run
```

Se o wrapper do Linux/macOS não tiver permissão de execução, conceda-a uma vez com `chmod +x mvnw`.

## Executar pelo IntelliJ IDEA

1. Abra o projeto a partir do arquivo `pom.xml` e aceite a importação Maven.
2. No painel **Maven**, clique em **Reload All Maven Projects** para sincronizar as dependências.
3. Confira se o JDK do projeto é 17 ou superior em **File > Project Structure > Project SDK**.
4. Selecione **Run > Edit Configurations...** e crie uma configuração **Application**:
   - **Main class:** `com.example.projra3ds.Launcher`
   - **Use classpath of module:** selecione o módulo do projeto (`ProjRA3DS`)
   - **Working directory:** a pasta raiz do projeto, onde está o `.env`
5. Salve e execute a configuração.

O projeto usa classpath, não JPMS/module-path. Execute pelo Maven ou por essa configuração Application; não use configurações antigas com `-m` ou `--module-path`. Se o IntelliJ ainda mostrar esses parâmetros, recarregue o projeto Maven e remova a configuração de execução antiga.

Como alternativa à leitura do `.env`, é possível definir `PENALTY_DB_USER` e `PENALTY_DB_PASSWORD` em **Run > Edit Configurations... > Environment variables**.

## Organização do código

- `model`: entidades JPA `Player` e `Team`
- `controller`: controllers JavaFX do MVC
- `src/main/resources/com/example/projra3ds`: Views FXML
- `game`: motor independente da interface e registros de cada cobrança
- `service`: validações e regras de negócio
- `repository`: consultas e operações JPA
- `facade`: ponto de entrada dos controllers para o gerenciamento
- `persistence`: unidade JPA, ciclo de vida da `EntityManagerFactory` e transações
