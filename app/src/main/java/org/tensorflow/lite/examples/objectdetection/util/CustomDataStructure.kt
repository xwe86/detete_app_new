package org.tensorflow.lite.examples.objectdetection.util

import org.tensorflow.lite.task.vision.detector.Detection

class CustomDataStructure(private val validityThreshold: Long = 50) {
    //过期的时间 默认100ms
    private val dataMap = HashMap<String, DataObject>()

    fun putData(key: String, data: DataObject) {
        dataMap[key] = data
    }

    fun getData(key: String): DataObject? {
        val data = dataMap[key]
        if (data != null && System.currentTimeMillis() - data.time <= validityThreshold) {
            return data
        }
        return null
    }

    fun getValidDataMap(): Map<String, DataObject> {
        val currentTime = System.currentTimeMillis()
        val validDataMap = HashMap<String, DataObject>()

        for ((key, data) in dataMap) {
            if (currentTime - data.time <= validityThreshold) {
                validDataMap[key] = data
            }
        }

        return validDataMap
    }

    data class DataObject(val data: Detection, val time: Long)

    fun clearAllData() {
        dataMap.clear()
    }


}