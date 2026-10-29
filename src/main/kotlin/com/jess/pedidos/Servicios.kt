package com.jess.pedidos
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Isolation
import org.springframework.http.HttpStatus
import java.security.SecureRandom
import java.util.Locale
class Regla(val estadoHttp: HttpStatus, override val message: String): RuntimeException(message)
@Service
class ProductoService(private val repo: Repositorio) {
 fun listar() = repo.productos()
 @Transactional fun crear(d: NuevoProducto) = repo.crearProducto(d)
}
@Service
class PedidoService(private val repo: Repositorio) {
 private val random = SecureRandom()
 fun obtener(id: Long) = repo.pedido(id) ?: throw Regla(HttpStatus.NOT_FOUND,"El pedido no existe")
 @Transactional(isolation=Isolation.SERIALIZABLE) fun crear(d: NuevoPedido): CreacionPedido {
  val nombre = d.nombreCliente.trim().replace(Regex("\\s+")," ")
  val clave = nombre.lowercase(Locale.ROOT)
  if(repo.pendiente(clave)) throw Regla(HttpStatus.CONFLICT,"El cliente ya tiene un pedido pendiente")
  if(d.productos.map { it.productoId }.distinct().size != d.productos.size)
   throw Regla(HttpStatus.BAD_REQUEST,"No repita productos; indique la cantidad total en una sola línea")
  // Orden estable de bloqueo para evitar interbloqueos entre pedidos.
  val productos = d.productos.sortedBy { it.productoId }.associate { item ->
   val producto = repo.producto(item.productoId,true) ?: throw Regla(HttpStatus.NOT_FOUND,"El producto ${item.productoId} no existe")
   if(producto.stock < item.cantidad) throw Regla(HttpStatus.CONFLICT,"Stock insuficiente para ${producto.nombre}")
   item.productoId to producto
  }
  val lineas = d.productos.map { item ->
   val producto = productos.getValue(item.productoId)
   if(repo.descontar(item.productoId,item.cantidad) != 1) throw Regla(HttpStatus.CONFLICT,"Stock insuficiente")
   LineaPedido(producto.id,producto.nombre,item.cantidad)
  }
  val codigo = (random.nextInt(900000)+100000).toString()
  return CreacionPedido(repo.crearPedido(nombre,clave,lineas,codigo),codigo)
 }
 @Transactional fun enviar(id: Long): Pedido {
  val p = repo.pedido(id,true) ?: throw Regla(HttpStatus.NOT_FOUND,"El pedido no existe")
  if(p.estado != Estado.PENDIENTE) throw Regla(HttpStatus.CONFLICT,"Solo puede enviar un pedido pendiente")
  repo.estado(id,Estado.ENVIADO)
  return p.copy(estado=Estado.ENVIADO)
 }
 @Transactional(isolation=Isolation.SERIALIZABLE) fun recibir(id: Long,d: Confirmacion): Pedido {
  val p = repo.pedido(id,true) ?: throw Regla(HttpStatus.NOT_FOUND,"El pedido no existe")
  if(p.estado != Estado.ENVIADO) throw Regla(HttpStatus.CONFLICT,"El pedido debe estar enviado antes de recibirlo")
  val clave = p.nombreCliente.trim().replace(Regex("\\s+")," ").lowercase(Locale.ROOT)
  if(repo.pendiente(clave)) throw Regla(HttpStatus.CONFLICT,"El cliente tiene otro pedido pendiente; debe enviarse antes de recibir este")
  if(repo.codigo(id) != d.codigo) throw Regla(HttpStatus.BAD_REQUEST,"Código de verificación incorrecto")
  repo.estado(id,Estado.RECIBIDO)
  return p.copy(estado=Estado.RECIBIDO)
 }
}
