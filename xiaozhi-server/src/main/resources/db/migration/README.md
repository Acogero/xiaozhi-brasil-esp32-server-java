# Scripts de migração do banco de dados (Flyway)

## Descrição

- O Flyway executa automaticamente as migrações pendentes na inicialização da aplicação
- Os scripts são executados em ordem de versão (V1 → V2 → V3...)
- Os scripts já executados ficam registrados na tabela `flyway_schema_history`
- **É proibido alterar um script já executado**, ou a validação falhará

## Regra de nomenclatura

```
V{número da versão}__{descrição}.sql
```

Exemplos:
- `V1__init.sql` - inicialização do banco de dados
- `V2__add_xxx.sql` - adição de um campo

## Migração atual

- `V1__init.sql` - inicialização completa do banco de dados (versão base)

## Adicionando uma nova migração

1. Crie um novo script, incrementando o número da versão a partir de V2: `V2__description.sql`
2. Escreva o SQL (apenas a mudança, sem repetir a criação completa das tabelas)
3. Faça commit no Git
4. Inicie a aplicação; o Flyway executa automaticamente

## Banco de dados já existente

Se o banco de dados já existir, o Flyway irá:
1. Criar a tabela `flyway_schema_history`
2. Marcar V0 como baseline (pois `baseline-on-migrate: true` está configurado)
3. Executar apenas os novos scripts com número de versão > 0

> **Atenção**: se um banco de dados antigo já tiver a tabela `flyway_schema_history`, é necessário limpá-la antes de iniciar a aplicação.

## Consultando o histórico de migrações

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```
