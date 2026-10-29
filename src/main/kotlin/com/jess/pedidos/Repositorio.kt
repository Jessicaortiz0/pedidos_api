package com.jess.pedidos
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
@Repository
class Repositorio(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper) {
 private val productoMapper = RowMapper { rs, _ -> Producto(rs.getLong("id"),rs.getString("nombre"),rs.getString("descripcion"),rs.getInt("stock")) }
 private val pedidoMapper = RowMapper { rs, _ -> Pedido(rs.getLong("id"),rs.getString("nombre_cliente"),Estado.valueOf(rs.getString("estado")), mapper.readValue(rs.getString("productos"), object: TypeReference<List<LineaPedido>>() {})) }
 fun productos() = jdbc.query("SELECT * FROM productos ORDER BY id",productoMapper)
 fun producto(id: Long, bloquear: Boolean=false): Producto? = jdbc.query("SELECT * FROM productos WHERE id=?" + if(bloquear) " FOR UPDATE" else "",productoMapper,id).firstOrNull()
 fun crearProducto(d: NuevoProducto): Producto {
  jdbc.update("INSERT INTO productos(nombre,descripcion,stock) VALUES(?,?,?)",d.nombre.trim(),d.descripcion.trim(),d.stock)
  return producto(jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long::class.java)!!)!!
 }
 fun pendiente(clave: String) = jdbc.queryForObject("SELECT COUNT(*) FROM pedidos WHERE cliente_clave=? AND estado='PENDIENTE'",Long::class.java,clave)!! > 0
 fun descontar(id: Long,cantidad: Int) = jdbc.update("UPDATE productos SET stock=stock-? WHERE id=? AND stock>=?",cantidad,id,cantidad)
 fun crearPedido(nombre: String,clave: String,lineas: List<LineaPedido>,codigo: String): Pedido {
  jdbc.update("INSERT INTO pedidos(nombre_cliente,cliente_clave,estado,productos,codigo_verificacion) VALUES(?,?,'PENDIENTE',?,?)",nombre,clave,mapper.writeValueAsString(lineas),codigo)
  return pedido(jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long::class.java)!!)!!
 }
 fun pedido(id: Long,bloquear: Boolean=false): Pedido? = jdbc.query("SELECT * FROM pedidos WHERE id=?" + if(bloquear) " FOR UPDATE" else "",pedidoMapper,id).firstOrNull()
 fun codigo(id: Long) = jdbc.queryForObject("SELECT codigo_verificacion FROM pedidos WHERE id=?",String::class.java,id)!!
 fun estado(id: Long,estado: Estado) { jdbc.update("UPDATE pedidos SET estado=? WHERE id=?",estado.name,id) }
}
