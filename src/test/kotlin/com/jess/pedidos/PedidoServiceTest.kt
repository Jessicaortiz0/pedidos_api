package com.jess.pedidos
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import jakarta.validation.Validation

class PedidoServiceTest {
 private val repo = mock(Repositorio::class.java)
 private val service = PedidoService(repo)
 private val pedido = Pedido(1,"Jess",Estado.PENDIENTE,listOf(LineaPedido(1,"Teclado",2)))
 @Test fun `bloquea cliente con pendiente`() {
  `when`(repo.pendiente("jess")).thenReturn(true)
  val e=assertThrows(Regla::class.java) { service.crear(NuevoPedido("Jess", listOf(LineaSolicitud(1,2)))) }
  assertEquals(HttpStatus.CONFLICT,e.estadoHttp)
  verify(repo,never()).descontar(anyLong(),anyInt())
 }
 @Test fun `rechaza producto inexistente`() {
  val e=assertThrows(Regla::class.java) { service.crear(NuevoPedido("Jess", listOf(LineaSolicitud(9,2)))) }
  assertEquals(HttpStatus.NOT_FOUND,e.estadoHttp)
 }
 @Test fun `rechaza stock insuficiente sin descontar`() {
  `when`(repo.producto(1,true)).thenReturn(Producto(1,"Teclado","USB",1))
  assertThrows(Regla::class.java) { service.crear(NuevoPedido("Jess",listOf(LineaSolicitud(1,2)))) }
  verify(repo,never()).descontar(anyLong(),anyInt())
 }
 @Test fun `valida todos los productos antes de descontar`() {
  `when`(repo.producto(1,true)).thenReturn(Producto(1,"Teclado","USB",10))
  assertThrows(Regla::class.java) { service.crear(NuevoPedido("Jess",listOf(LineaSolicitud(1,2),LineaSolicitud(2,1)))) }
  verify(repo,never()).descontar(anyLong(),anyInt())
 }
 @Test fun `no recibe pedido pendiente`() {
  `when`(repo.pedido(1,true)).thenReturn(pedido)
  assertThrows(Regla::class.java) { service.recibir(1,Confirmacion("123456")) }
  verify(repo,never()).estado(1,Estado.RECIBIDO)
 }
 @Test fun `rechaza codigo incorrecto`() {
  `when`(repo.pedido(1,true)).thenReturn(pedido.copy(estado=Estado.ENVIADO))
  `when`(repo.codigo(1)).thenReturn("123456")
  assertThrows(Regla::class.java) { service.recibir(1,Confirmacion("654321")) }
  verify(repo,never()).estado(1,Estado.RECIBIDO)
 }
 @Test fun `recibe con codigo correcto solo una vez`() {
  `when`(repo.pedido(1,true)).thenReturn(pedido.copy(estado=Estado.ENVIADO))
  `when`(repo.codigo(1)).thenReturn("123456")
  assertEquals(Estado.RECIBIDO,service.recibir(1,Confirmacion("123456")).estado)
  `when`(repo.pedido(1,true)).thenReturn(pedido.copy(estado=Estado.RECIBIDO))
  assertThrows(Regla::class.java) { service.recibir(1,Confirmacion("123456")) }
  verify(repo,times(1)).estado(1,Estado.RECIBIDO)
 }
 @Test fun `no recibe si existe otro pendiente del cliente`() {
  `when`(repo.pedido(1,true)).thenReturn(pedido.copy(estado=Estado.ENVIADO))
  `when`(repo.pendiente("jess")).thenReturn(true)
  assertThrows(Regla::class.java) { service.recibir(1,Confirmacion("123456")) }
  verify(repo,never()).estado(1,Estado.RECIBIDO)
 }
 @Test fun `solo envia pendientes`() {
  `when`(repo.pedido(1,true)).thenReturn(pedido)
  assertEquals(Estado.ENVIADO,service.enviar(1).estado)
  `when`(repo.pedido(1,true)).thenReturn(pedido.copy(estado=Estado.RECIBIDO))
  assertThrows(Regla::class.java) { service.enviar(1) }
 }
 @Test fun `valida cantidades y lista vacia`() {
  Validation.buildDefaultValidatorFactory().use { factory ->
   val validator=factory.validator
   assertFalse(validator.validate(NuevoPedido("",emptyList())).isEmpty())
   assertFalse(validator.validate(NuevoPedido("Jess",listOf(LineaSolicitud(1,0)))).isEmpty())
   assertFalse(validator.validate(NuevoProducto("Teclado","USB",-1)).isEmpty())
  }
 }
}
