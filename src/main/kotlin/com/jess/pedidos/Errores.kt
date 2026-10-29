package com.jess.pedidos
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.ConcurrencyFailureException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
@RestControllerAdvice
class Errores {
 @ExceptionHandler(Regla::class)
 fun negocio(e: Regla) = ResponseEntity.status(e.estadoHttp).body(mapOf("mensaje" to e.message))
 @ExceptionHandler(MethodArgumentNotValidException::class)
 fun validacion(e: MethodArgumentNotValidException) = ResponseEntity.badRequest().body(mapOf("mensaje" to "Datos inválidos", "errores" to e.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }))
 @ExceptionHandler(HttpMessageNotReadableException::class,MethodArgumentTypeMismatchException::class)
 fun formato(e: Exception) = ResponseEntity.badRequest().body(mapOf("mensaje" to "JSON, campos o identificador inválidos"))
 @ExceptionHandler(ConcurrencyFailureException::class)
 fun concurrencia(e: ConcurrencyFailureException) = ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("mensaje" to "Otra operación modificó estos datos; vuelva a intentarlo"))
 @ExceptionHandler(DuplicateKeyException::class)
 fun duplicado(e: DuplicateKeyException) = ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("mensaje" to "El cliente ya tiene un pedido pendiente"))
}
