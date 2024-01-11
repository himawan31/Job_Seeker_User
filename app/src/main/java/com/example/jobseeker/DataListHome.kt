package com.example.jobseeker

data class DataListHome(
    val documentId: String,
    val image_pekerjaan: String,
    val nama_pekerjaan: String,
    val lokasi_pekerjaan: String,
    var salary: String?,
    var working_time: String?,
    var userId: String? = null,
)
