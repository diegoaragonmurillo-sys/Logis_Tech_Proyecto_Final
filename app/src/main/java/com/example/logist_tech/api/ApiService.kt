package com.example.logist_tech.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path

interface ApiService {

    @GET("api/v1/cajas")
    suspend fun getCajas(): Response<List<CajaResponse>>

    @POST("api/v1/cajas")
    suspend fun postCaja(
        @Body caja: CajaRequest
    ): Response<CajaResponse>

    @PATCH("api/v1/cajas/{id}/estado")
    suspend fun patchEstadoCaja(
        @Path("id") id: String,
        @Body estado: EstadoRequest
    ): Response<Unit>

    @POST("api/v1/despachos")
    suspend fun postDespacho(
        @Body despacho: DespachoRequest
    ): Response<Unit>
}