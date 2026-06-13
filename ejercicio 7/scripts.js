// lee el json recibido por la api
context.message.body.readAsJSON(function (error, entrada) {
  if (error) {
    context.message.statusCode = 400
    context.message.header.set('content-type', 'application/json')
    context.message.body.write({
      codigo: 'JSON_INVALIDO',
      mensaje: 'el body no contiene un json válido'
    })
    return
  }

  var transacciones = Array.isArray(entrada.transacciones)
    ? entrada.transacciones
    : []

  var grupos = {}
  var registrosDescartados = []
  var totalCentavos = 0

  // agrupa los registros válidos y trabaja en centavos para evitar errores con floats
  transacciones.forEach(function (transaccion) {
    var montoOriginal = transaccion.monto
    var monto = Number(montoOriginal)

    var montoInvalido =
      montoOriginal === null ||
      montoOriginal === '' ||
      typeof montoOriginal === 'boolean' ||
      !Number.isFinite(monto)

    if (montoInvalido) {
      registrosDescartados.push(transaccion.id)
      return
    }

    var tipo = String(transaccion.tipoMovimiento || 'SIN_TIPO').toUpperCase()
    var montoCentavos = Math.round((monto + Number.EPSILON) * 100)

    if (!grupos[tipo]) {
      grupos[tipo] = {
        tipo: tipo,
        cantidad: 0,
        subtotalCentavos: 0
      }
    }

    grupos[tipo].cantidad++
    grupos[tipo].subtotalCentavos += montoCentavos
    totalCentavos += montoCentavos
  })

  // construye el resumen y calcula el porcentaje de cada grupo
  var resumen = Object.keys(grupos).map(function (tipo) {
    var grupo = grupos[tipo]
    var porcentaje = totalCentavos === 0
      ? 0
      : redondear((grupo.subtotalCentavos * 100) / totalCentavos)

    return {
      tipo: grupo.tipo,
      cantidad: grupo.cantidad,
      subtotal: redondear(grupo.subtotalCentavos / 100),
      porcentaje: porcentaje
    }
  })

  // ordena los grupos desde el subtotal más alto
  resumen.sort(function (a, b) {
    return b.subtotal - a.subtotal
  })

  var salida = {
    cuenta: entrada.cuenta || '',
    periodo: entrada.periodo || '',
    resumen: resumen,
    totalGeneral: redondear(totalCentavos / 100),
    generadoEn: new Date().toISOString()
  }

  // agrega los ids descartados solo cuando existen
  if (registrosDescartados.length > 0) {
    salida.registrosDescartados = registrosDescartados
  }

  context.message.statusCode = 200
  context.message.header.set('content-type', 'application/json')
  context.message.body.write(salida)
})

// redondea cualquier resultado a dos decimales
function redondear(numero) {
  return Math.round((numero + Number.EPSILON) * 100) / 100
}