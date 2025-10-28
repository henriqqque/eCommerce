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

public class CompraServiceParticaoTest {

    private CompraService service;
    private Produto produtoEletronico;
    private Produto produtoLivro;
    private Produto produtoMovelFragil;

    @BeforeEach
    public void setup() {
        service = new CompraService(null, null, null, null);

        produtoEletronico = criarProduto("200.00", "3.00", "10", "10", "10",
                TipoProduto.ELETRONICO, false);

        produtoLivro = criarProduto("50.00", "2.00", "15", "15", "15",
                TipoProduto.LIVRO, false);

        produtoMovelFragil = criarProduto("1000.00", "27.00", "50", "30", "25",
                TipoProduto.MOVEL, true);
    }

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

    @DisplayName("Teste carrinho vazio retorna 0")
    @ParameterizedTest
    @CsvSource({
            "'',",
            "'null',"
    })
    public void calcularCustoTotalcarrinhoVazio(String dummy) {
        CarrinhoDeCompras carrinho = null;
        if (!"null".equals(dummy)) {
            carrinho = new CarrinhoDeCompras();
            carrinho.setItens(new ArrayList<>());
        }
        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).isEqualByComparingTo("0.00");
    }

    @DisplayName("Testa subtotal e frete básico para diferentes preços")
    @ParameterizedTest
    @CsvSource({
            "200.00, SUDESTE, PRATA, 200.00",
            "600.00, NORTE, BRONZE, 540.00",
            "1200.00, NORDESTE, OURO, 960.00"
    })
    public void calcularCustoTotalcomVariedadesDePreco(String precoStr, String regiaoStr, String tipoClienteStr, String esperadoStr) {
        Produto produto = criarProduto(precoStr, "5.00", "10", "10", "10", TipoProduto.ROUPA, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));
        Regiao regiao = Regiao.valueOf(regiaoStr);
        TipoCliente tipoCliente = TipoCliente.valueOf(tipoClienteStr);
        BigDecimal esperado = new BigDecimal(esperadoStr);

        BigDecimal resultado = service.calcularCustoTotal(carrinho, regiao, tipoCliente);
        assertThat(resultado).isEqualByComparingTo(esperado);
    }

    @DisplayName("Teste de desconto por tipo para múltiplos itens")
    @ParameterizedTest
    @CsvSource({
            "3, 28.50",
            "5, 45.00",
            "8, 84.00"
    })
    public void descontoPorTipoparaVariasQuantidades(int quantidade, String esperadoStr) {
        Produto produto = criarProduto("10.00", "1.00", "10", "10", "10", TipoProduto.LIVRO, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, quantidade));
        BigDecimal esperado = new BigDecimal(esperadoStr);

        BigDecimal resultado = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(resultado).isEqualByComparingTo(esperado);
    }

    @DisplayName("Teste de frete com taxa para itens frágeis")
    @ParameterizedTest
    @CsvSource({
            "1, 117.85",
            "2, 260.90"
    })
    public void freteTaxaParaFragil(int quantidade, String esperadoStr) {
        Produto produto = criarProduto("100.00", "6.00", "10", "10", "10", TipoProduto.ELETRONICO, true);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, quantidade));
        BigDecimal esperado = new BigDecimal(esperadoStr);

        BigDecimal resultado = service.calcularCustoTotal(carrinho, Regiao.SUL, TipoCliente.BRONZE);
        assertThat(resultado).isEqualByComparingTo(esperado);
    }

    @DisplayName("Teste de frete zerado para cliente Ouro")
    @ParameterizedTest
    @CsvSource({
            "OURO, 250.00, 250.00",
            "PRATA, 250.00, 258.80",
            "BRONZE, 250.00, 267.60"
    })
    public void clienteDiferentesFretes(String tipoClienteStr, String precoStr, String esperadoStr) {
        Produto produto = criarProduto(precoStr, "8.00", "10", "10", "10", TipoProduto.ALIMENTO, false);
        CarrinhoDeCompras carrinho = criarCarrinho(criarItem(produto, 1));
        TipoCliente tipoCliente = TipoCliente.valueOf(tipoClienteStr);
        BigDecimal esperado = new BigDecimal(esperadoStr);

        BigDecimal resultado = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, tipoCliente);
        assertThat(resultado).isEqualByComparingTo(esperado);
    }
}
