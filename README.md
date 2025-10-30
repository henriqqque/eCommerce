
# Projeto de Testes de Software — Sistema de Compras Online

**Discente:** Paulo Henrique Santos de Sousa  
**Docente:** Eiji Adachi  
**Disciplina:** Testes de Software I

**Relatório Automatizado de Análise e Testes - eCommerce**

---

## 1. Visão Geral do Projeto

Este projeto implementa um sistema de cálculo de custo total de compra para carrinho de compras online, considerando múltiplas regras de negócio e aplicando diversas técnicas de testes automatizados. Desenvolvido em Java 17 com JUnit 5 e AssertJ, realizou-se análise de cobertura de código via JaCoCo. O foco principal foi aplicar técnicas de teste de caixa-preta e caixa-branca, verificando partições de equivalência, valores-limite, tabelas de decisão, critérios MCDC e cobertura estrutural.

O objetivo principal é aplicar **técnicas de teste de caixa-preta e caixa-branca**, verificando partições, valores-limite, tabelas de decisão, critérios MC/DC e cobertura estrutural.

---

## 2. Estrutura do Projeto

**Arquivos principais:**
```
pom.xml                           → Configuração Maven
src/main/java/ecommerce/service/CompraService.java
src/test/java/ecommerce/service/CompraServiceParticaoTest.java
src/test/java/ecommerce/service/CompraServiceValorLimiteTest.java
src/test/java/ecommerce/service/CompraServiceDecisaoTest.java
src/test/java/ecommerce/service/CompraServiceExcecaoTest.java
src/test/java/ecommerce/service/CompraServiceTest.java
target/site/jacoco/jacoco.xml     → Relatório de cobertura JaCoCo
```

**Principais entidades:**
- `Produto`, `ItemCompra`, `CarrinhoDeCompras`, `Regiao`, `TipoCliente`, `TipoProduto`
- Serviços auxiliares: `ClienteService`, `CarrinhoDeComprasService`
- Simulações externas: `EstoqueSimulado`, `PagamentoSimulado`

---

## 3. Regras de Negócio Implementadas

1. Cálculo de subtotal realizado pelo método CompraService.calcularSubtotal.
2. Aplicação de desconto por tipo de produto, considerando quantidades 3+, 5+ e 8+, implementada em calcularDescontoPorTipoProduto.
3. Desconto por valor total do carrinho, com faixas de R$500 e R$1000, via calcularDescontoPorValorCarrinho.
4. Cálculo de peso cúbico e peso tributável através do método calcularPesoTotal.
5. Cálculo do frete baseado em faixas de peso e aplicação de taxa mínima, executado em calcularFreteBase.
6. Inclusão de taxa adicional por item frágil, também contemplada em calcularFreteBase.
7. Uso de multiplicador regional para ajuste do frete, implementado em freteMultiplicadorPorRegiao.
8. Benefícios de fidelidade para clientes classificados como Ouro, Prata e Bronze, aplicados por aplicarDescontoFidelidade.
9. Arredondamento final do valor total da compra com precisão de duas casas decimais e arredondamento HALF_UP através de setScale(2, RoundingMode.HALF_UP).
10. Tratamento de exceções e validações das entradas no bloco inicial do método principal calcularCustoTotal.

---
## 4. Técnicas de Teste Aplicadas
1. Particionamento em classes de equivalência realizado na classe de teste CompraServiceParticaoTest, abrangendo casos gerais de descontos, frete e fidelidade.
2. Análise de valores-limite aplicada na CompraServiceValorLimiteTest, testando limites de peso (5, 10, 50 kg) e valores monetários (R$500, R$1000).
3. Testes baseados em tabela de decisão implementados na CompraServiceDecisaoTest, cobrindo combinações variadas de descontos e programas de fidelidade.
4. Testes de exceção presentes em CompraServiceExcecaoTest, contemplando entradas inválidas como valores nulos e preços negativos.
5. Casos de uso gerais e testes de integração feitos em CompraServiceTest, avaliando fluxos completos com e sem aplicação de descontos.

## 5. Cobertura JaCoCo
1. Cobertura de instruções alcançou 61,78%, indicando percentual de linhas executadas durante os testes.
2. Cobertura de ramos atingiu 83,19%, demonstrando boa verificação das condições lógicas no código.
3. Cobertura de linhas foi de 56,58%, mostrando potencial para aumento com testes complementares.
4. Cobertura de complexidade alcançou 54,86%, enquanto métodos e classes tiveram cobertura mais baixa (42,35% e 38,89%, respectivamente).
5. A análise evidencia alta cobertura de ramos, mas sugere expansão dos testes para melhorar cobertura geral e integração.

## 6. Testes de Caixa-Preta
1. Partições de equivalência testaram cenários como frete isento até 5 kg, faixas de peso com taxações específicas e descontos por tipo e valor total de compra.
2. Valores-limite cobrem casos específicos, como transição entre faixas de peso e limites de subtotal para descontos aplicados.
3. Testes contemplam ainda clientes com status de fidelidade variado (Ouro, Prata, Bronze) e itens frágeis com taxa extra no frete.

## 7. Tabela de Decisão (Resumo)
1. Regras definem condições para aplicação de descontos por quantidade e subtotal, faixas de peso para cálculo de frete e benefícios conforme perfil do cliente.
2. Exemplos de regras incluem: sem desconto para até 2 itens por tipo, 5% de desconto para 3+ itens, até 20% para subtotal acima de R$1000.
3. Frete é calculado conforme peso e região, com isenção para clientes Ouro e desconto para clientes Prata.

## 8. Testes de Caixa-Branca (MC/DC)
1. Cobertura parcial da decisão principal envolvendo verificação de carrinho e itens.
2. Casos testados incluem carrinho nulo, carrinho com itens não nulos e parcialmente testado carrinho com itens nulos, recomendando a inclusão deste último.

## 9. Complexidade Ciclomática
1. O método principal calcularCustoTotal possui 10 decisões, gerando complexidade ciclomática 11.
2. Isso indica a necessidade de pelo menos 11 casos de teste independentes para cobertura completa.

---

## 10. Execução e Relatórios

### Executar testes
```bash
mvn clean test
```

```bash
mvn verify
```

### Gerar relatório de cobertura
```bash
mvn jacoco:report
# Abrir em:
target/site/jacoco/index.html
```

---
