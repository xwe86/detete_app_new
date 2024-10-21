package org.tensorflow.lite.examples.objectdetection.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMainBinding
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCarFront45Binding
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentIdCamraBinding
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentVehicleDamageBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 车辆损伤部位
 */
class VehicleDamageFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var _fragmentVehicleDamageBinding: FragmentVehicleDamageBinding? = null
    private val fragmentVehicleDamageBinding
        get() = _fragmentVehicleDamageBinding!!
    private var currentFragment: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        // 初始显示的 Fragment

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _fragmentVehicleDamageBinding = FragmentVehicleDamageBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        // 车牌
        fragmentVehicleDamageBinding.leftFrontDoorButton.setOnClickListener {
            val fragmentManager = parentFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction() // 创建要跳转的目标 Fragment 实例
            val newFragment = FrontDoorFragment() // 替换当前 Fragment 为目标 Fragment
            fragmentTransaction.replace(R.id.fragment_container, newFragment) // 将事务提交
            fragmentTransaction.addToBackStack(null) // 可选，将当前事务添加到返回栈
            fragmentTransaction.commit()
            currentFragment = DefualtFragment()
        }
        return fragmentVehicleDamageBinding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment VehicleDamageFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            VehicleDamageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }


}