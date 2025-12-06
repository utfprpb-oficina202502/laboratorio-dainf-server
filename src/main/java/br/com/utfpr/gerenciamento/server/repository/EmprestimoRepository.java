package br.com.utfpr.gerenciamento.server.repository;

import br.com.utfpr.gerenciamento.server.dto.dashboards.EstatisticasEmprestimoProjection;
import br.com.utfpr.gerenciamento.server.dto.dashboards.ItemFrequenteUsuarioDto;
import br.com.utfpr.gerenciamento.server.model.Emprestimo;
import br.com.utfpr.gerenciamento.server.model.Usuario;
import br.com.utfpr.gerenciamento.server.model.dashboards.DashboardEmprestimoCountRange;
import br.com.utfpr.gerenciamento.server.model.dashboards.DashboardEmprestimoDia;
import br.com.utfpr.gerenciamento.server.model.dashboards.DashboardItensEmprestados;
import br.com.utfpr.gerenciamento.server.repository.projection.EmprestimoListProjection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmprestimoRepository
    extends JpaRepository<Emprestimo, Long>, JpaSpecificationExecutor<Emprestimo> {

  @Query(
      """
      SELECT new br.com.utfpr.gerenciamento.server.model.dashboards.DashboardEmprestimoDia(COUNT(e), e.dataEmprestimo)
      FROM Emprestimo e
      WHERE e.dataEmprestimo BETWEEN :dtIni AND :dtFim
      GROUP BY e.dataEmprestimo
      ORDER BY e.dataEmprestimo ASC
      """)
  List<DashboardEmprestimoDia> countByDataEmprestimo(
      @Param("dtIni") LocalDate dtIni, @Param("dtFim") LocalDate dtFim);

  List<Emprestimo> findAllByDataEmprestimoBetween(LocalDate dtIni, LocalDate dtFim);

  /**
   * Query otimizada para dashboard com agregação no banco de dados.
   *
   * <p>Substitui o carregamento de todos emprestimos + 4 iterações stream por uma única query com
   * agregação.
   *
   * @param dtIni Data inicial do range
   * @param dtFim Data final do range
   * @return Objeto com totalizadores (total, emAndamento, emAtraso, finalizado)
   */
  @Query(
      """
              SELECT new br.com.utfpr.gerenciamento.server.model.dashboards.DashboardEmprestimoCountRange(
                  COUNT(e),
                  COALESCE(SUM(CASE WHEN e.dataDevolucao IS NULL AND e.prazoDevolucao >= CURRENT_DATE THEN 1L ELSE 0L END), 0L),
                  COALESCE(SUM(CASE WHEN e.dataDevolucao IS NULL AND e.prazoDevolucao < CURRENT_DATE THEN 1L ELSE 0L END), 0L),
                  COALESCE(SUM(CASE WHEN e.dataDevolucao IS NOT NULL THEN 1L ELSE 0L END), 0L))
              FROM Emprestimo e
              WHERE e.dataEmprestimo BETWEEN :dtIni AND :dtFim
              """)
  DashboardEmprestimoCountRange countEmprestimosByStatusInRange(
      @Param("dtIni") LocalDate dtIni, @Param("dtFim") LocalDate dtFim);

  @Query(
      """
      SELECT new br.com.utfpr.gerenciamento.server.model.dashboards.DashboardItensEmprestados(SUM(e.qtde), i.nome)
      FROM EmprestimoItem e
      LEFT JOIN Emprestimo em ON em.id = e.emprestimo.id
      LEFT JOIN Item i ON i.id = e.item.id
      WHERE em.dataEmprestimo BETWEEN :dtIni AND :dtFim
      GROUP BY i.nome
      """)
  List<DashboardItensEmprestados> findItensMaisEmprestados(
      @Param("dtIni") LocalDate dtIni, @Param("dtFim") LocalDate dtFim);

  List<Emprestimo> findAllByUsuarioEmprestimo(Usuario usuario);

  List<Emprestimo> findAllByDataDevolucaoIsNullOrderById();

  List<Emprestimo> findByDataDevolucaoIsNullAndPrazoDevolucaoEquals(LocalDate dt);

  List<Emprestimo> findAllByUsuarioEmprestimoAndDataDevolucaoIsNull(Usuario usuarioEmprestimo);

  /**
   * Busca Emprestimo por ID com eager loading de todas as relações necessárias para email.
   *
   * <p>Elimina N+1 queries ao carregar todas as associações em uma única query com JOIN FETCH.
   *
   * <p>Performance: 5 queries → 1 query (redução de 80%)
   *
   * @param id ID do empréstimo
   * @return Optional com Emprestimo e todas as relações carregadas, ou empty se não encontrado
   */
  @EntityGraph(
      attributePaths = {
        "usuarioEmprestimo",
        "usuarioResponsavel",
        "emprestimoItem",
        "emprestimoItem.item",
        "emprestimoDevolucaoItem",
        "emprestimoDevolucaoItem.item"
      })
  @Query("SELECT e FROM Emprestimo e WHERE e.id = :id")
  Optional<Emprestimo> findEmprestimoByIdWithRelations(@Param("id") Long id);

  @Query("SELECT DISTINCT e FROM Emprestimo e JOIN e.emprestimoItem ei WHERE ei.item.id = :itemId")
  List<Emprestimo> findAllByItemId(@Param("itemId") Long itemId);

  /**
   * Busca paginada de empréstimos para listagem com campos otimizados.
   *
   * @param pageable paginação e ordenação
   * @return Page de projeções com apenas campos necessários para tabela
   */
  @Query(
      """
      SELECT e.id as id,
             e.dataEmprestimo as dataEmprestimo,
             e.prazoDevolucao as prazoDevolucao,
             e.dataDevolucao as dataDevolucao,
             ue.nome as usuarioEmprestimoNome
      FROM Emprestimo e
      LEFT JOIN e.usuarioEmprestimo ue
      """)
  Page<EmprestimoListProjection> findAllProjected(Pageable pageable);

  /**
   * Busca paginada de empréstimos com filtro de texto.
   *
   * @param filter texto para filtrar por id ou nome do usuário
   * @param pageable paginação e ordenação
   * @return Page de projeções filtradas
   */
  @Query(
      """
      SELECT e.id as id,
             e.dataEmprestimo as dataEmprestimo,
             e.prazoDevolucao as prazoDevolucao,
             e.dataDevolucao as dataDevolucao,
             ue.nome as usuarioEmprestimoNome
      FROM Emprestimo e
      LEFT JOIN e.usuarioEmprestimo ue
      WHERE CAST(e.id AS string) LIKE CONCAT('%', :filter, '%')
         OR LOWER(ue.nome) LIKE LOWER(CONCAT('%', :filter, '%'))
      """)
  Page<EmprestimoListProjection> findAllProjectedWithFilter(
      @Param("filter") String filter, Pageable pageable);

  /**
   * Busca paginada de empréstimos por usuário específico (para role-based filtering).
   *
   * @param username username do usuário
   * @param pageable paginação e ordenação
   * @return Page de projeções do usuário
   */
  @Query(
      """
      SELECT e.id as id,
             e.dataEmprestimo as dataEmprestimo,
             e.prazoDevolucao as prazoDevolucao,
             e.dataDevolucao as dataDevolucao,
             ue.nome as usuarioEmprestimoNome
      FROM Emprestimo e
      LEFT JOIN e.usuarioEmprestimo ue
      WHERE ue.username = :username
      """)
  Page<EmprestimoListProjection> findAllProjectedByUsername(
      @Param("username") String username, Pageable pageable);

  /**
   * Busca paginada de empréstimos por usuário específico com filtro de texto.
   *
   * @param username username do usuário
   * @param filter texto para filtrar
   * @param pageable paginação e ordenação
   * @return Page de projeções filtradas do usuário
   */
  @Query(
      """
      SELECT e.id as id,
             e.dataEmprestimo as dataEmprestimo,
             e.prazoDevolucao as prazoDevolucao,
             e.dataDevolucao as dataDevolucao,
             ue.nome as usuarioEmprestimoNome
      FROM Emprestimo e
      LEFT JOIN e.usuarioEmprestimo ue
      WHERE ue.username = :username
        AND (CAST(e.id AS string) LIKE CONCAT('%', :filter, '%')
             OR LOWER(ue.nome) LIKE LOWER(CONCAT('%', :filter, '%')))
      """)
  Page<EmprestimoListProjection> findAllProjectedByUsernameWithFilter(
      @Param("username") String username, @Param("filter") String filter, Pageable pageable);

  // ========== DASHBOARD PESSOAL DO USUARIO ==========

  /**
   * Conta estatisticas de emprestimos do usuario: em aberto, em atraso e total.
   *
   * @param username Username do usuario logado
   * @return Projection com estatisticas (emAberto, emAtraso, total)
   */
  @Query(
      """
      SELECT new br.com.utfpr.gerenciamento.server.dto.dashboards.EstatisticasEmprestimoProjection(
          COALESCE(SUM(CASE WHEN e.dataDevolucao IS NULL AND e.prazoDevolucao >= CURRENT_DATE THEN 1L ELSE 0L END), 0L),
          COALESCE(SUM(CASE WHEN e.dataDevolucao IS NULL AND e.prazoDevolucao < CURRENT_DATE THEN 1L ELSE 0L END), 0L),
          COUNT(e))
      FROM Emprestimo e
      WHERE e.usuarioEmprestimo.username = :username
      """)
  EstatisticasEmprestimoProjection countEstatisticasByUsername(@Param("username") String username);

  /**
   * Busca a proxima data de devolucao pendente do usuario.
   *
   * @param username Username do usuario logado
   * @return Data da proxima devolucao ou null se nao houver
   */
  @Query(
      """
      SELECT MIN(e.prazoDevolucao)
      FROM Emprestimo e
      WHERE e.usuarioEmprestimo.username = :username
        AND e.dataDevolucao IS NULL
      """)
  LocalDate findProximaDevolucaoByUsername(@Param("username") String username);

  /**
   * Busca os itens mais emprestados pelo usuario.
   *
   * @param username Username do usuario logado
   * @param pageable Paginacao para limitar resultados
   * @return Lista de itens frequentes com contagem
   */
  @Query(
      """
      SELECT new br.com.utfpr.gerenciamento.server.dto.dashboards.ItemFrequenteUsuarioDto(
          i.id, i.nome, COUNT(ei), i.saldo)
      FROM EmprestimoItem ei
      JOIN ei.emprestimo e
      JOIN ei.item i
      WHERE e.usuarioEmprestimo.username = :username
      GROUP BY i.id, i.nome, i.saldo
      ORDER BY COUNT(ei) DESC
      """)
  List<ItemFrequenteUsuarioDto> findItensMaisEmprestadosByUsername(
      @Param("username") String username, Pageable pageable);

  /**
   * Conta emprestimos por mes do usuario para historico de uso.
   *
   * @param username Username do usuario logado
   * @param dtIni Data inicial do periodo
   * @param dtFim Data final do periodo
   * @return Lista de arrays [ano, mes, quantidade]
   */
  @Query(
      """
      SELECT YEAR(e.dataEmprestimo), MONTH(e.dataEmprestimo), COUNT(e)
      FROM Emprestimo e
      WHERE e.usuarioEmprestimo.username = :username
        AND e.dataEmprestimo BETWEEN :dtIni AND :dtFim
      GROUP BY YEAR(e.dataEmprestimo), MONTH(e.dataEmprestimo)
      ORDER BY YEAR(e.dataEmprestimo), MONTH(e.dataEmprestimo)
      """)
  List<Object[]> countEmprestimosPorMesByUsername(
      @Param("username") String username,
      @Param("dtIni") LocalDate dtIni,
      @Param("dtFim") LocalDate dtFim);

  /**
   * Busca emprestimos do usuario para exibicao no calendario.
   *
   * @param username Username do usuario logado
   * @param dtIni Data inicial do periodo
   * @param dtFim Data final do periodo
   * @return Lista de emprestimos com itens carregados
   */
  @EntityGraph(attributePaths = {"emprestimoItem", "emprestimoItem.item"})
  @Query(
      """
      SELECT e
      FROM Emprestimo e
      WHERE e.usuarioEmprestimo.username = :username
        AND (e.dataEmprestimo BETWEEN :dtIni AND :dtFim
             OR e.prazoDevolucao BETWEEN :dtIni AND :dtFim
             OR (e.dataDevolucao IS NOT NULL AND e.dataDevolucao BETWEEN :dtIni AND :dtFim))
      """)
  List<Emprestimo> findEmprestimosParaCalendarioByUsername(
      @Param("username") String username,
      @Param("dtIni") LocalDate dtIni,
      @Param("dtFim") LocalDate dtFim);

  /**
   * Busca emprestimos do usuario para timeline de atividades.
   *
   * @param username Username do usuario logado
   * @param pageable Paginacao para limitar resultados
   * @return Lista de emprestimos ordenados por data
   */
  @EntityGraph(attributePaths = {"emprestimoItem", "emprestimoItem.item"})
  @Query(
      """
      SELECT e
      FROM Emprestimo e
      WHERE e.usuarioEmprestimo.username = :username
      ORDER BY e.dataEmprestimo DESC
      """)
  List<Emprestimo> findEmprestimosParaAtividadesByUsername(
      @Param("username") String username, Pageable pageable);
}
