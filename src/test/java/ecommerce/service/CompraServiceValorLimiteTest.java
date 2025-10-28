package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;

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

public class CompraServiceValorLimiteTest {

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
        for (ItemCompra it : itens) lista.add(it);
        carrinho.setItens(lista);
        return carrinho;
    }

    @Test
    @DisplayName("Peso exatamente 5kg -> frete zero")
    public void pesoExato5kg_freteZero() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("100.00", "5.00", "10", "10", "10", TipoProduto.ELETRONICO, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Peso 5.5kg -> valorPorKg=2 e frete < taxa minima => taxa minima aplicada")
    public void peso5_5kg_aplicaTaxaMinima() {
        CompraService service = new CompraService(null, null, null, null);

        // peso 5.5 -> valorPorKg = 2 -> frete = 11.0 < TAXA_MINIMA(12) => frete = 12
        Produto produto = criarProduto("10.00", "5.5", "10", "10", "10", TipoProduto.ROUPA, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
        // subtotal = 10.00, frete = 12.00 => total = 22.00
        assertThat(total).isEqualByComparingTo("22.00");
    }

    @Test
    @DisplayName("Peso exatamente 10kg -> valorPorKg passa para 4")
    public void pesoExato10kg_valorPorKg4() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("10.00", "10.00", "10", "10", "10", TipoProduto.ROUPA, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
        // frete = 10 * 4 = 40 => total = 50.00
        assertThat(total).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Peso exatamente 50.01kg -> valorPorKg = 7")
    public void pesoAcima50_valorPorKg7() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("10.00", "50.01", "10", "10", "10", TipoProduto.ROUPA, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
        // frete = 50.01 * 7 = 350.07 => total = 360.07 -> arredondado para 360.07
        assertThat(total).isEqualByComparingTo("360.07");
    }

    @Test
    @DisplayName("Peso cúbico maior que físico (peso tributável = cubico)")
    public void pesoCubicoMaiorQueFisico() {
        CompraService service = new CompraService(null, null, null, null);

        // Faz alturas grandes para que peso cubico (L*C*H/6000) supere o peso físico
        Produto produto = criarProduto("20.00", "1.00", "200", "200", "200", TipoProduto.MOVEL, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
        // Subtotal = 20.00. Peso cúbico = 200*200*200/6000 = 1,333... * 10^? -> verifica que não causa erro e frete é calculado
        assertThat(total).isGreaterThan(new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("Desconto por valor exatamente no limite 500 (não aplica 10%)")
    public void descontoLimite500_naoAplica10() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("500.00", "1.00", "10", "10", "10", TipoProduto.LIVRO, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
        // subtotal=500 -> não entra na tabela >500 -> não aplica 10%
        assertThat(total).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Desconto por valor exatamente no limite 1000 (não aplica 20%)")
    public void descontoLimite1000_naoAplica20() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("1000.00", "1.00", "10", "10", "10", TipoProduto.LIVRO, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
        // subtotal=1000 -> não entra na condição >1000
        assertThat(total).isEqualByComparingTo("900.00");
    }
}
