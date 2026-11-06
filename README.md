# Simulador de disputa de pênaltis

Aplicação desktop em JavaFX: cadastro e gerenciamento de jogadores e times. Inclui associação, transferência e desvinculação de jogadores.

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

O banco configurado é `simulador_penaltis`. O EclipseLink cria as tabelas necessárias e estende o esquema ao iniciar a aplicação, sem apagar as tabelas existentes. O usuário da aplicação precisa de permissões nesse banco para criar/alterar as tabelas.

## Executar pelo terminal

Na raiz do projeto, rode:

```bash
./mvnw clean test
./mvnw javafx:run
```

O primeiro comando compila e executa os testes das regras de negócio sem conectar ao MySQL. O segundo inicia a interface e precisa de um `.env` válido e do MySQL em execução.

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
- `service`: validações e regras de negócio
- `repository`: consultas e operações JPA
- `facade`: ponto de entrada dos controllers para o gerenciamento
- `persistence`: unidade JPA, ciclo de vida da `EntityManagerFactory` e transações
