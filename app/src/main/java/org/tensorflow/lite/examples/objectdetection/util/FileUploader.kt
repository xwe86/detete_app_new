package org.tensorflow.lite.examples.objectdetection.util

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class FileUploader {

    private val client = OkHttpClient()

    fun uploadFile(file: File, carPart:String, name:String, callback: Callback) {
        // 创建 RequestBody 对象
        // 创建 MediaType 对象
        val mediaType = "image/jpeg".toMediaTypeOrNull() ?: throw IllegalArgumentException("Unknown content type");
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(mediaType)
            )
            .addFormDataPart("carPart",carPart)
            .addFormDataPart("recordId",GlobalRandomIdManager.getGlobalRandomId())
            .addFormDataPart("name",name)
            .build()

        // 创建 Request 对象
        val request = Request.Builder()
            .url("http://vpn.hisprintgo.com")
            .post(requestBody)
            .build()

        // 发送请求
        client.newCall(request).enqueue(callback)
    }
}