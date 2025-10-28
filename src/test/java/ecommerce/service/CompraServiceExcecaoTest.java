package ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.TipoProduto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;

public class CompraServiceExcecaoTest {

    private Produto criarProduto(String preco, String peso) {
        Produto p = new Produto();
        p.setPreco(preco == null ? null : new BigDecimal(preco));
        p.setPesoFisico(peso == null ? null : new BigDecimal(peso));
        p.setComprimento(new BigDecimal("10"));
        p.setLargura(new BigDecimal("10"));
        p.setAltura(new BigDecimal("10"));
        p.setTipo(TipoProduto.ROUPA);
        p.setFragil(false);
        return p;
    }

    private ItemCompra criarItem(Produto produto, long quantidade) {
        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        return item;
    }

    @Test
    @DisplayName("Quantidade zero lança IllegalArgumentException")
    public void quantidadeZero_lancaExcecao() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("10.00", "1.00");
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        ItemCompra it = criarItem(produto, 0);
        carrinho.setItens(new java.util.ArrayList<>());
        carrinho.getItens().add(it);

        assertThrows(IllegalArgumentException.class, () ->
                service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE));
    }

    @Test
    @DisplayName("Preço negativo lança IllegalArgumentException")
    public void precoNegativo_lancaExcecao() {
        CompraService service = new CompraService(null, null, null, null);

        Produto produto = criarProduto("-1.00", "1.00");
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        ItemCompra it = criarItem(produto, 1);
        carrinho.setItens(new ArrayList<>());
        carrinho.getItens().add(it);

        assertThrows(IllegalArgumentException.class, () ->
                service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE));
    }

    @Test
    @DisplayName("Produto nulo no item lança IllegalArgumentException")
    public void produtoNulo_lancaExcecao() {
        CompraService service = new CompraService(null, null, null, null);

        ItemCompra it = new ItemCompra();
        it.setProduto(null);
        it.setQuantidade(1L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(new ArrayList<>());
        carrinho.getItens().add(it);

        assertThrows(IllegalArgumentException.class, () ->
                service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE));
    }

    @Test
    @DisplayName("Carrinho com itens nulos lança IllegalArgumentException")
    public void itemNulo_naLista_lancaExcecao() {
        CompraService service = new CompraService(null, null, null, null);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(new ArrayList<>());
        carrinho.getItens().add(null);

        assertThrows(IllegalArgumentException.class, () ->
                service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE));
    }
}
