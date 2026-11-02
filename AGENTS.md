# AGENTS.md — manage-sound-app

> Este repositório faz parte do ecossistema **Manage Sound Mode**.  
> Além das regras locais abaixo, siga sempre as diretrizes globais da raiz em [`../../AGENTS.md`](../../AGENTS.md) e o estado global em [`../../STATE.md`](../../STATE.md).

---

## 1. Visão Geral do Repositório

* **Nome**: `manage-sound-app`
* **Descrição**: Aplicativo Android nativo (Kotlin + Jetpack Compose) para gerenciamento e automação de modos de som
* **Localização no Workspace**: `repos/manage-sound-app/`
* **Stack Principal**: *(Definir stack: ex. Kotlin / Swift / React Native / TypeScript / Node / Python)*

---

## 2. Comandos Essenciais

*(Atualize assim que o toolchain do repositório for inicializado)*

```bash
# Instalar dependências
# npm install / ./gradlew build / uv sync

# Rodar em desenvolvimento
# npm run dev

# Rodar testes e verificação de tipos
# npm test && npm run typecheck
```

---

## 3. Estrutura Interna

```text
manage-sound-app/
├── src/                # Código-fonte principal
├── tests/              # Testes automatizados
├── AGENTS.md           # Instruções locais para Agentes de IA
└── README.md           # Documentação do repositório
```

---

## 4. Regras Específicas deste Repositório

1. **Contratos**: Se este repositório expõe ou consome APIs/eventos de outro repo em `repos/`, mantenha o schema sincronizado em [`../../docs/contracts/`](../../docs/contracts/).
2. **Validação**: Nunca finalize uma tarefa sem rodar o comando de build/test deste repositório.
3. **Segredos**: Use `.env` local (nunca versionado) e documente novas variáveis em `.env.example`.
