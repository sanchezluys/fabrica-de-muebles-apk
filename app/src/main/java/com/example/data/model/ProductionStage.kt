package com.example.data.model

enum class ProductionStage(
    val title: String,
    val shortName: String,
    val defaultHours: Int,
    val stepIndex: Int
) {
    RECEPCION("Recepción y Planos", "Recepción", 24, 0),
    CORTE("Corte y Dimensionado", "Corte", 36, 1),
    ENSAMBLE("Ensamble y Estructura", "Ensamble", 48, 2),
    ACABADO("Lijado y Pintura/Acabado", "Acabado", 48, 3),
    CALIDAD("Control de Calidad", "Calidad", 12, 4),
    LISTO("Listo para Entrega", "Listo", 24, 5);

    fun next(): ProductionStage? {
        val entries = entries
        val nextIdx = ordinal + 1
        return if (nextIdx < entries.size) entries[nextIdx] else null
    }

    fun isLast(): Boolean = this == LISTO
}

enum class Priority(val label: String) {
    ALTA("Alta Prioridad"),
    MEDIA("Prioridad Media"),
    BAJA("Prioridad Normal")
}
