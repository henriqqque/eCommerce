package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

public class CompraServiceTest {

    private Produto criarProduto(String preco, String peso, String comprimento, String largura, String altura,
                                 TipoProduto tipo, boolean fragil) {
        Produto p = new Produto();
        p.setPreco(new BigDecimal(preco));
        p.setPesoFisico(new BigDecimal(peso));
        p.setComprimento(new BigDecimal(comprimento));
        p.setLargura(new BigDecimal(largura));
        p.setAltura(new BigDecimal(altura));
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

    @Test
    @DisplayName("Deve retornar total 0.00 para carrinho vazio")
    public void calcularCustoTotalcarrinhoVazio() {
        CompraService service = new CompraService(null, null, null, null);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(new ArrayList<>());

        BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        BigDecimal esperado = new BigDecimal("0.00");
        assertEquals(0, custoTotal.compareTo(esperado), "Valor calculado incorreto: " + custoTotal);
        assertThat(custoTotal).as("Custo total de carrinho vazio").isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Deve calcular subtotal simples sem frete (peso <= 5kg)")
    public void calcularCustoTotalsemDescontos_freteIsento() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("100.00", "2.00", "10", "10", "10",
                TipoProduto.ELETRONICO, false);
        ItemCompra item = criarItem(produto, 1);
        CarrinhoDeCompras carrinho = criarCarrinho(item);

        BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        BigDecimal esperado = new BigDecimal("100.00");
        assertEquals(0, custoTotal.compareTo(esperado), "Valor calculado incorreto: " + custoTotal);
        assertThat(custoTotal).as("Compra sem descontos e frete isento").isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Deve aplicar 5% de desconto para 3 itens do mesmo tipo")
    public void calcularCustoTotaldescontoPorTipo3itens() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("10.00", "1.00", "10", "10", "10",
                TipoProduto.LIVRO, false);

        CarrinhoDeCompras carrinho = criarCarrinho(
                criarItem(produto, 1),
                criarItem(produto, 1),
                criarItem(produto, 1)
        );

        BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        // subtotal = 30.00, desconto 5% = 1.50, total esperado = 28.50
        BigDecimal esperado = new BigDecimal("28.50");
        assertEquals(0, custoTotal.compareTo(esperado), "Valor calculado incorreto: " + custoTotal);
        assertThat(custoTotal).as("Desconto de 5% aplicado corretamente").isEqualByComparingTo("28.50");
    }

    @Test
    @DisplayName("Deve aplicar 20% de desconto em subtotal > 1000")
    public void calcularCustoTotal_descontoPorValorMaiorQue1000() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("1100.00", "2.00", "10", "10", "10",
                TipoProduto.LIVRO, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        // subtotal = 1100.00, desconto 20% = 220.00, total = 880.00
        BigDecimal esperado = new BigDecimal("880.00");
        assertEquals(0, custoTotal.compareTo(esperado), "Desconto incorreto: " + custoTotal);
        assertThat(custoTotal).as("Desconto de 20% aplicado corretamente").isEqualByComparingTo("880.00");
    }

    @Test
    @DisplayName("Deve calcular frete com taxa mínima e adicional de frágil, multiplicador da região e desconto de cliente PRATA")
    public void calcularCustoTotal_frete_com_taxas_e_fidelidade() {
        CompraService service = new CompraService(null, null, null, null);

        // Peso total = 6kg => faixa B (2/kg) = 12.00 (igual à taxa mínima)
        // Produto frágil: +5.00
        Produto produto = criarProduto("100.00", "6.00", "10", "10", "10",
                TipoProduto.MOVEL, true);

        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUL, TipoCliente.PRATA);

        /*
         subtotal = 100.00
         frete base = (6 * 2) = 12.00 (>= taxa mínima)
         taxa frágil = +5.00 → total frete base = 17.00
         multiplicador SUL = 1.05 → frete = 17.85
         cliente PRATA paga 50% → 8.925 ≈ 8.93
         total final = 108.93
        */
        BigDecimal esperado = new BigDecimal("108.93");
        assertEquals(0, custoTotal.compareTo(esperado), "Frete calculado incorreto: " + custoTotal);
        assertThat(custoTotal).as("Frete e fidelidade calculados corretamente").isEqualByComparingTo("108.93");
    }

    @Test
    @DisplayName("Cliente OURO deve ter frete zerado após cálculo")
    public void calcularCustoTotalclienteOuro_freteZerado() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("200.00", "8.00", "10", "10", "10",
                TipoProduto.ELETRONICO, true);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, TipoCliente.OURO);

        // Subtotal 200.00, frete calculado mas zerado no final (frete Ouro = 0)
        BigDecimal esperado = new BigDecimal("200.00");
        assertEquals(0, custoTotal.compareTo(esperado), "Frete Ouro não zerado corretamente: " + custoTotal);
        assertThat(custoTotal).as("Cliente Ouro deve pagar apenas o subtotal").isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Carrinho nulo deve retornar total 0.00")
    public void calcularCustoTotalcarrinhoNulo() {
        CompraService service = new CompraService(null, null, null, null);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(null);

        BigDecimal custoTotal = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        BigDecimal esperado = new BigDecimal("0.00");
        assertEquals(0, custoTotal.compareTo(esperado), "Valor incorreto para carrinho nulo: " + custoTotal);
        assertThat(custoTotal).as("Carrinho nulo deve retornar 0.00").isEqualByComparingTo("0.00");
    }
}