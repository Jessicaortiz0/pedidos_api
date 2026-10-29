package com.jess.pedidos
import jakarta.validation.Valid
import jakarta.validation.constraints.*

enum class Estado { PENDIENTE, ENVIADO, RECIBIDO }
data class Producto(val id: Long, val nombre: String, val descripcion: String, val stock: Int)
data class NuevoProducto(
 @field:NotBlank @field:Size(max=120) val nombre: String,
 @field:NotBlank @field:Size(max=500) val descripcion: String,
 @field:Min(0) val stock: Int
)
data class LineaSolicitud(@field:Positive val productoId: Long, @field:Positive val cantidad: Int)
data class NuevoPedido(
 @field:NotBlank @field:Size(max=120) val nombreCliente: String,
 @field:NotEmpty @field:Size(max=100) @field:Valid val productos: List<LineaSolicitud>
)
data class LineaPedido(val productoId: Long, val nombreProducto: String, val cantidad: Int)
data class Pedido(val id: Long, val nombreCliente: String, val estado: Estado, val productos: List<LineaPedido>)
data class Confirmacion(@field:Pattern(regexp="[0-9]{6}", message="Debe ingresar el código de seis dígitos") val codigo: String)
data class CreacionPedido(val pedido: Pedido, val codigoVerificacion: String)
