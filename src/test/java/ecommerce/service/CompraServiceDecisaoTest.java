package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ecommerce.entity.*;

public class CompraServiceDecisaoTest {

    private CompraService service;

    @BeforeEach
    public void setup() {
        service = new CompraService(null, null, null, null);
    }

    private Produto criarProduto(String preco, String peso, TipoProduto tipo, boolean fragil) {
        Produto p = new Produto();
        p.setPreco(new BigDecimal(preco));
        p.setPesoFisico(new BigDecimal(peso));
        p.setComprimento(BigDecimal.TEN);
        p.setLargura(BigDecimal.TEN);
        p.setAltura(BigDecimal.TEN);
        p.setTipo(tipo);
        p.setFragil(fragil);
        return p;
    }

    private ItemCompra criarItem(Produto produto, long quantidade) {
        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        return item;
    }

    private CarrinhoDeCompras criarCarrinho(ItemCompra... itens) {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        List<ItemCompra> lista = new ArrayList<>();
        for (ItemCompra it : itens) {
            lista.add(it);
        }
        carrinho.setItens(lista);
        return carrinho;
    }

    @DisplayName("Testes combinados das regras da tabela de decisão")
    @ParameterizedTest(name = "[{index}] subtotal={0}, fragil={1}, cliente={2}, regiao={3} -> total esperado {4}")
    @CsvSource({
            "1200.00, false, OURO, SUDESTE, 960.00",   // subtotal, frete zerado ouro
            "1200.00, true, BRONZE, NORDESTE, 978.70", // fragil + desconto 20%, frete regio nordeste, cliente bronze
            "800.00, true, PRATA, SUL, 728.93",        // subtotal < 1000 sem desconto, fragil, cliente prata
            "400.00, false, BRONZE, SUDESTE, 412.00"   // sem desconto, sem fragil, cliente bronze, região sudeste
    })
    public void calcularCustoTotaldecisoesCombinadas(String subtotalStr, boolean fragil, String tipoClienteStr, String regiaoStr, String esperadoStr) {
        BigDecimal subtotal = new BigDecimal(subtotalStr);
        TipoCliente tipoCliente = TipoCliente.valueOf(tipoClienteStr);
        Regiao regiao = Regiao.valueOf(regiaoStr);

        Produto produto = criarProduto(subtotalStr, "6.00", TipoProduto.ELETRONICO, fragil);
        ItemCompra item = criarItem(produto, 1);
        CarrinhoDeCompras carrinho = criarCarrinho(item);

        BigDecimal esperado = new BigDecimal(esperadoStr);

        BigDecimal resultado = service.calcularCustoTotal(carrinho, regiao, tipoCliente);

        assertThat(resultado).as("Resultado esperado da combinacaoo de regras").isEqualByComparingTo(esperado);
    }
}