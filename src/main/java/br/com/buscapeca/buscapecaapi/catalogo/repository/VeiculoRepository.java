package br.com.buscapeca.buscapecaapi.catalogo.repository;

import br.com.buscapeca.buscapecaapi.catalogo.entity.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    @Query("select distinct v.marca from Veiculo v order by v.marca")
    List<String> listarMarcas();

    /** Filtros opcionais: parametro nulo e ignorado. Marca e modelo sem diferenciar maiusculas. */
    @Query("""
            select v from Veiculo v
            where (:marca is null or lower(v.marca) = lower(:marca))
              and (:modelo is null or lower(v.modelo) = lower(:modelo))
              and (:ano is null or v.ano = :ano)
            order by v.marca, v.modelo, v.ano, v.versao
            """)
    List<Veiculo> filtrar(@Param("marca") String marca, @Param("modelo") String modelo, @Param("ano") Short ano);

    /** Um veiculo da marca e modelo, para usar a grafia do catalogo (ex.: "Volkswagen", "Gol"). */
    Optional<Veiculo> findFirstByMarcaIgnoreCaseAndModeloIgnoreCase(String marca, String modelo);

    /** Versao nula so casa com versao nula (o UNIQUE do MySQL aceita varias linhas com NULL). */
    @Query("""
            select v from Veiculo v
            where lower(v.marca) = lower(:marca)
              and lower(v.modelo) = lower(:modelo)
              and v.ano = :ano
              and ((:versao is null and v.versao is null) or lower(v.versao) = lower(:versao))
            """)
    Optional<Veiculo> buscar(@Param("marca") String marca, @Param("modelo") String modelo,
                             @Param("ano") Short ano, @Param("versao") String versao);
}
