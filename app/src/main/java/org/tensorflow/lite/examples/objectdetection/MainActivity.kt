/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.objectdetection

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMainBinding
import org.tensorflow.lite.examples.objectdetection.fragments.CameraFragment
import org.tensorflow.lite.examples.objectdetection.fragments.CarBack45Fragment
import org.tensorflow.lite.examples.objectdetection.fragments.CarFront45Fragment
import org.tensorflow.lite.examples.objectdetection.fragments.DefualtFragment
import org.tensorflow.lite.examples.objectdetection.fragments.DriverIdCardFragment
import org.tensorflow.lite.examples.objectdetection.fragments.IDCamraFragment
import org.tensorflow.lite.examples.objectdetection.fragments.VINCFragment
import org.tensorflow.lite.examples.objectdetection.fragments.VINFragment
import org.tensorflow.lite.examples.objectdetection.fragments.VehicleDamageFragment
import org.tensorflow.lite.examples.objectdetection.fragments.VehicleIdCardFragment
import org.tensorflow.lite.examples.objectdetection.util.GlobalRandomIdManager


/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        // 初始显示的 Fragment
        currentFragment = DefualtFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, currentFragment!!)
            .commit()

        // 车牌
        activityMainBinding.carPlateButton?.setOnClickListener {
            switchFragment(CameraFragment())
        }
        //车架
        activityMainBinding.vinIdButton?.setOnClickListener {
            switchFragment(VINCFragment())
        }
        // 身份证
        activityMainBinding.idCardButton?.setOnClickListener {
            switchFragment(IDCamraFragment())
        }
        //行驶证
        activityMainBinding.vehicleIdButton?.setOnClickListener {
            switchFragment(DriverIdCardFragment())
        }
        // 驾驶证
        activityMainBinding.driverIdButton?.setOnClickListener {
            switchFragment(VehicleIdCardFragment())
        }
        //前45°
        activityMainBinding.carFront45Button?.setOnClickListener {
            switchFragment(CarFront45Fragment())
        }
        // 后45°
        activityMainBinding.carBack45Button?.setOnClickListener {
            switchFragment(CarBack45Fragment())
        }
        //损伤识别
        activityMainBinding.carDamageButton?.setOnClickListener {
            switchFragment(VehicleDamageFragment())
        }



    }

    private fun switchFragment(fragment: Fragment) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Workaround for Android Q memory leak issue in IRequestFinishCallback$Stub.
            // (https://issuetracker.google.com/issues/139738913)
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }




}
