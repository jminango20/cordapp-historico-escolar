package com.template.webserver.dto

import java.time.Instant

data class HistoricoDTO(
        val idAluno: Int,
        val nomeCurso: String,
        val dataInicio: Instant,
        val nota: Int,
        val cargaHoraria: Int,
        val faculdade: String
)
