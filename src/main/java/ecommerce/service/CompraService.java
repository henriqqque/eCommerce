package ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ecommerce.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService {

	private static final BigDecimal PESO_CUBICO_DIVISOR = BigDecimal.valueOf(6000);
	private static final BigDecimal TAXA_FRAGIL_POR_ITEM = BigDecimal.valueOf(5);
	private static final BigDecimal TAXA_MINIMA_FRETE = BigDecimal.valueOf(12);

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;
	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
						 IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal) {
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;
		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId) {
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(ItemCompra::getQuantidade)
				.collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);
		if (!disponibilidade.disponivel()) {
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());
		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());
		if (!pagamento.autorizado()) {
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);
		if (!baixaDTO.sucesso()) {
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		return new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");
	}


	// ======= METODO PRINCIPAL =======
	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {

		if (carrinho != null && carrinho.getItens() != null) {
			for (ItemCompra item : carrinho.getItens()) {
				if (item == null || item.getProduto() == null) {
					throw new IllegalArgumentException("Item ou produto nulo não permitido.");
				}
				if (item.getQuantidade() <= 0) {
					throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
				}
				if (item.getProduto().getPreco() == null || item.getProduto().getPreco().compareTo(BigDecimal.ZERO) < 0) {
					throw new IllegalArgumentException("Preço inválido (nulo ou negativo).");
				}
				if (item.getProduto().getPesoFisico() == null || item.getProduto().getPesoFisico().compareTo(BigDecimal.ZERO) < 0) {
					throw new IllegalArgumentException("Peso inválido (nulo ou negativo).");
				}
			}
		}

		// 1. subtotal dos itens
		BigDecimal subtotal = calcularSubtotal(carrinho);

		// 2. desconto por múltiplos itens de mesmo tipo (desconto por tipo e subtotal com desconto por tipo)
		BigDecimal descontoPorTipo = calcularDescontoPorTipoProduto(carrinho);
		BigDecimal subtotalComDescontoTipo = subtotal.subtract(descontoPorTipo);

		// 3. desconto por valor de carrinho (desconto por valor e subtotal final)
		BigDecimal descontoPorValor = calcularDescontoPorValorCarrinho(subtotalComDescontoTipo);
		BigDecimal subtotalFinal = subtotalComDescontoTipo.subtract(descontoPorValor);

		// 4. cálculo do frete base por peso total (peso total, frete base, fator regional e frete com fator regional)
		BigDecimal pesoTotal = calcularPesoTotal(carrinho);
		BigDecimal freteBase = calcularFreteBase(pesoTotal, carrinho);
		BigDecimal multiplicadorRegiao = freteMultiplicadorPorRegiao(regiao);
		BigDecimal freteComMultiplicador = freteBase.multiply(multiplicadorRegiao);

		// 5. benefício por nível de cliente aplicado no frete
		BigDecimal freteFinal = aplicarDescontoFidelidadeNoFrete(freteComMultiplicador, tipoCliente);

		// 6. total da compra
		BigDecimal totalCompra = subtotalFinal.add(freteFinal);

		return totalCompra.setScale(2, RoundingMode.HALF_UP);
	}


	// ======= METODOS AUXILIARES =======
	private BigDecimal calcularSubtotal(CarrinhoDeCompras carrinho) {
		BigDecimal subtotal = BigDecimal.ZERO;

		if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
			return subtotal;
		}

		for (ItemCompra item : carrinho.getItens()) {
			BigDecimal preco = item.getProduto().getPreco();
			Long quantidade = item.getQuantidade();
			subtotal = subtotal.add(preco.multiply(BigDecimal.valueOf(quantidade)));
		}
		return subtotal;
	}

	private BigDecimal calcularDescontoPorTipoProduto(CarrinhoDeCompras carrinho) {
		BigDecimal descontoTotal = BigDecimal.ZERO;

		if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
			return descontoTotal;
		}

		List<TipoProduto> tiposVerificados = new ArrayList<>(); // registro de quais tipos já foram verificados

		for (ItemCompra item : carrinho.getItens()) {
			TipoProduto tipo = item.getProduto().getTipo();
			if (tiposVerificados.contains(tipo)) continue; // evita calcular o desconto mais de uma vez para o mesmo tipo de produto

			int qtdTotalTipo = 0;
			BigDecimal subtotalTipo = BigDecimal.ZERO;

			for (ItemCompra it : carrinho.getItens()) {
				if (it.getProduto().getTipo().equals(tipo)) {
					qtdTotalTipo += it.getQuantidade();
					subtotalTipo = subtotalTipo.add(it.getProduto().getPreco()
							.multiply(BigDecimal.valueOf(it.getQuantidade())));
				}
			}

			BigDecimal percentual = BigDecimal.ZERO;
			if (qtdTotalTipo >= 3 && qtdTotalTipo <= 4) {
				percentual = BigDecimal.valueOf(0.05);
			} else if (qtdTotalTipo >= 5 && qtdTotalTipo <= 7) {
				percentual = BigDecimal.valueOf(0.10);
			} else if (qtdTotalTipo >= 8){
				percentual = BigDecimal.valueOf(0.15);
			}

			descontoTotal = descontoTotal.add(subtotalTipo.multiply(percentual));
			tiposVerificados.add(tipo);
		}
		return descontoTotal;
	}

	private BigDecimal calcularDescontoPorValorCarrinho(BigDecimal subtotal) {
		if (subtotal == null) {
			return BigDecimal.ZERO;
		}

		if (subtotal.compareTo(BigDecimal.valueOf(1000)) > 0) {
			return subtotal.multiply(BigDecimal.valueOf(0.20));
		} else if (subtotal.compareTo(BigDecimal.valueOf(500)) > 0) {
			return subtotal.multiply(BigDecimal.valueOf(0.10));
		} else {
			return BigDecimal.ZERO;
		}
	}

	private BigDecimal calcularPesoTotal(CarrinhoDeCompras carrinho) {
		BigDecimal pesoTotal = BigDecimal.ZERO;

		if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
			return pesoTotal;
		}

		for (ItemCompra item : carrinho.getItens()) {
			Produto produto = item.getProduto();
			BigDecimal pesoCubico = produto.getComprimento()
					.multiply(produto.getLargura())
					.multiply(produto.getAltura())
					.divide(PESO_CUBICO_DIVISOR, 4, RoundingMode.HALF_UP);

			BigDecimal pesoTributavel = produto.getPesoFisico().max(pesoCubico);
			pesoTotal = pesoTotal.add(pesoTributavel.multiply(BigDecimal.valueOf(item.getQuantidade())));
		}
		return pesoTotal;
	}

	private BigDecimal calcularFreteBase(BigDecimal pesoTotal, CarrinhoDeCompras carrinho) {

		if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
			return BigDecimal.ZERO;
		}

		BigDecimal valorPorKg = BigDecimal.ZERO;
		if (pesoTotal.compareTo(BigDecimal.ZERO) >= 0 && pesoTotal.compareTo(BigDecimal.valueOf(5)) <= 0) {
			valorPorKg = BigDecimal.ZERO;
		} else if (pesoTotal.compareTo(BigDecimal.valueOf(5)) > 0 && pesoTotal.compareTo(BigDecimal.valueOf(10)) <= 0) {
			valorPorKg = BigDecimal.valueOf(2);
		} else if (pesoTotal.compareTo(BigDecimal.valueOf(10)) > 0 && pesoTotal.compareTo(BigDecimal.valueOf(50)) <= 0) {
			valorPorKg = BigDecimal.valueOf(4);
		} else if (pesoTotal.compareTo(BigDecimal.valueOf(50)) > 0) {
			valorPorKg = BigDecimal.valueOf(7);
		}

		BigDecimal frete = pesoTotal.multiply(valorPorKg);
		if (valorPorKg.compareTo(BigDecimal.ZERO) > 0 && frete.compareTo(TAXA_MINIMA_FRETE) < 0) {
			frete = TAXA_MINIMA_FRETE;
		}

		BigDecimal taxaFragil = BigDecimal.ZERO;
		for (ItemCompra item : carrinho.getItens()) {
			if (Boolean.TRUE.equals(item.getProduto().isFragil())) {
				taxaFragil = taxaFragil.add(TAXA_FRAGIL_POR_ITEM.multiply(BigDecimal.valueOf(item.getQuantidade())));
			}
		}
		return frete.add(taxaFragil);
	}

	private BigDecimal freteMultiplicadorPorRegiao(Regiao regiao) {
		if (regiao == null) {
			return BigDecimal.ONE;
		}

		switch (regiao) {
			case SUDESTE:
				return BigDecimal.valueOf(1.00);
			case SUL:
				return BigDecimal.valueOf(1.05);
			case NORDESTE:
				return BigDecimal.valueOf(1.10);
			case CENTRO_OESTE:
				return BigDecimal.valueOf(1.20);
			case NORTE:
				return BigDecimal.valueOf(1.30);
			default:
				return BigDecimal.ONE;
		}
	}

	private BigDecimal aplicarDescontoFidelidadeNoFrete(BigDecimal frete, TipoCliente tipoCliente) {
		if (frete == null) {
			return BigDecimal.ZERO;
		}

		switch (tipoCliente) {
			case OURO:
				return BigDecimal.ZERO;
			case PRATA:
				return frete.multiply(BigDecimal.valueOf(0.5));
			case BRONZE:
			default:
				return frete;
		}
	}
}