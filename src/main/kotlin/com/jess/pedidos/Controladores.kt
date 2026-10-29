package com.jess.pedidos
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import java.net.URI
@RestController
@RequestMapping("/productos")
class ProductoController(private val service: ProductoService) {
 @GetMapping fun listar() = service.listar()
 @PostMapping fun crear(@Valid @RequestBody d: NuevoProducto): ResponseEntity<Producto> {
  val p=service.crear(d)
  return ResponseEntity.created(URI.create("/productos")).body(p)
 }
}
@RestController
@RequestMapping("/pedidos")
class PedidoController(private val service: PedidoService) {
 @PostMapping fun crear(@Valid @RequestBody d: NuevoPedido): ResponseEntity<CreacionPedido> {
  val p=service.crear(d)
  return ResponseEntity.created(URI.create("/pedidos/${p.pedido.id}")).body(p)
 }
 @GetMapping("/{id}") fun obtener(@PathVariable id: Long) = service.obtener(id)
 @PatchMapping("/{id}/enviar") fun enviar(@PathVariable id: Long) = service.enviar(id)
 @PatchMapping("/{id}/recibir") fun recibir(@PathVariable id: Long,@Valid @RequestBody d: Confirmacion) = service.recibir(id,d)
}
