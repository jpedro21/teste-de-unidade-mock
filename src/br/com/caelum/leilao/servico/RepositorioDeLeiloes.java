package br.com.caelum.leilao.servico;

import java.util.List;

import br.com.caelum.leilao.dominio.Leilao;

public interface RepositorioDeLeiloes {
	
	public void atualiza(Leilao leilao);
	public List<Leilao> correntes();
	public List<Leilao> encerrados();
	public void salva(Leilao leilao);
	
}
