package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Matchers.any;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;
import org.mockito.InOrder;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.infra.dao.LeilaoDao;
import br.com.caelum.leilao.servico.EncerradorDeLeilao;

public class EncerraLeilaoTest {
	
	@Test
    public void deveEncerrarLeiloesQueComecaramUmaSemanaAtras() {

        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
            .naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
            .naData(antiga).constroi();
        List<Leilao> leiloesAntigos = Arrays.asList(leilao1, leilao2);

        LeilaoDao daoFalso = mock(LeilaoDao.class);
        when(daoFalso.correntes()).thenReturn(leiloesAntigos);

        EnviadorDeEmail sender = mock(EnviadorDeEmail.class);

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, sender);
        encerrador.encerra();

        assertTrue(leilao1.isEncerrado());
        assertTrue(leilao2.isEncerrado());
    }
	
	@Test
    public void naoDeveEncerrarLeiloesQueComecaramOntem() {

        Calendar antiga = Calendar.getInstance();
        antiga.set(2018, 5, 2);

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
            .naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
            .naData(antiga).constroi();
        List<Leilao> leiloesAtuais = Arrays.asList(leilao1, leilao2);

        LeilaoDao daoFalso = mock(LeilaoDao.class);
        when(daoFalso.correntes()).thenReturn(leiloesAtuais);
        
        EnviadorDeEmail sender = mock(EnviadorDeEmail.class);

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, sender);
        encerrador.encerra();

        assertFalse(leilao1.isEncerrado());
        assertFalse(leilao2.isEncerrado());
    }
	
	@Test
	public void encerrarNaoFazNada() {
		
		LeilaoDao daoFalso = mock(LeilaoDao.class);
		when(daoFalso.correntes()).thenReturn(new ArrayList<Leilao>());

		EnviadorDeEmail sender = mock(EnviadorDeEmail.class);

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, sender);
        encerrador.encerra();
        
        assertEquals(0, encerrador.getTotalEncerrados());
	}
	
	@Test
	public void deveAtualizarLeiloesEncerrados() {
		
		Calendar data = Calendar.getInstance();
		data.set(1999, 12, 17);
		
		Leilao leilao = new CriadorDeLeilao().para("TV de plasma")
	            .naData(data).constroi();
		
		LeilaoDao daoFalso = mock(LeilaoDao.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao));
		
		EnviadorDeEmail sender = mock(EnviadorDeEmail.class);

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, sender);
		encerrador.encerra();
		
		 // passamos os mocks que serao verificados
        InOrder inOrder = inOrder(sender, daoFalso);
        // a primeira invocação
        inOrder.verify(daoFalso, times(1)).atualiza(leilao);    
        // a segunda invocação
        inOrder.verify(sender, times(1)).envia(leilao);
		
		assertTrue(leilao.isEncerrado());
	}
	
	@Test
    public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras() {

        Calendar ontem = Calendar.getInstance();
        ontem.add(Calendar.DAY_OF_MONTH, -1);
        
        Calendar dataAntiga = Calendar.getInstance();
        dataAntiga.set(1999, 12, 17);
        
        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
            .naData(ontem).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
            .naData(dataAntiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        EnviadorDeEmail sender = mock(EnviadorDeEmail.class);

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, sender);
        encerrador.encerra();

        assertEquals(1, encerrador.getTotalEncerrados());
        assertFalse(leilao1.isEncerrado());
        assertTrue(leilao2.isEncerrado());

        verify(daoFalso, never()).atualiza(leilao1);
        verify(daoFalso, times(1)).atualiza(leilao2);
	}
	
	@Test
	public void deveContinuarOLeilaoMesmoComException() {
		
        Calendar dataAntiga = Calendar.getInstance();
        dataAntiga.set(1999, 12, 17);
        
        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
            .naData(dataAntiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
            .naData(dataAntiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
        doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);

        EnviadorDeEmail sender = mock(EnviadorDeEmail.class);

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, sender);
        encerrador.encerra();
        
        verify(daoFalso).atualiza(leilao2);
        verify(sender).envia(leilao2);
        
        verify(sender, never()).envia(leilao1);
        
	}
	
	@Test
	public void deveContinuarOLeilaoMesmoComExceptionDoEviadorDeEmail() {
		
        Calendar dataAntiga = Calendar.getInstance();
        dataAntiga.set(1999, 12, 17);
        
        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
            .naData(dataAntiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
            .naData(dataAntiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
        
        EnviadorDeEmail sender = mock(EnviadorDeEmail.class);
        doThrow(new RuntimeException()).when(sender).envia(leilao1);

        
        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, sender);
        encerrador.encerra();
        
        verify(daoFalso).atualiza(leilao2);
        verify(sender).envia(leilao2);
        
	}
	
	@Test
	public void exceptionTodosElementosDaLista() {
		
        Calendar dataAntiga = Calendar.getInstance();
        dataAntiga.set(1999, 12, 17);
        
        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
            .naData(dataAntiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira")
            .naData(dataAntiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
        doThrow(new RuntimeException()).when(daoFalso).atualiza(any(Leilao.class));
        
        EnviadorDeEmail sender = mock(EnviadorDeEmail.class);
        
        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, sender);
        encerrador.encerra();
        
        verify(sender, never()).envia(any(Leilao.class));
        
	}


}
